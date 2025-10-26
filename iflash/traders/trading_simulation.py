#!/usr/bin/env python3
import requests
import random
import time
import threading
from typing import List, Dict, Optional
import logging
import datetime

# -----------------------------
# CONFIG
# -----------------------------
BASE_URL = "http://localhost:10023/api/v1"  # <-- set your API base
TICKER = "NVDA"

# trader density / speed
N_INSTANCES = 3
MIN_SLEEP = 0.05
MAX_SLEEP = 0.25

# order queue processing interval (main thread)
QUEUE_PROCESS_INTERVAL = 0.5

# stressor config
STRESSOR_COUNT = 2

# Lookback used by some strategies (internal)
LOOKBACK = 10

# starting capital for traders
INITIAL_CAPITAL = 10000  # USD available to each trader

# log setup
logging.basicConfig(
    format="%(asctime)s [%(levelname)s] %(message)s",
    level=logging.INFO
)

# -----------------------------
# THREAD-SAFE ORDER QUEUE
# -----------------------------
class OrderQueue:
    def __init__(self):
        self.lock = threading.Lock()
        self.buy_orders: List[Dict] = []

    def add_order(self, order: Dict):
        with self.lock:
            self.buy_orders.append(order)

    def process_queue(self):
        with self.lock:
            if not self.buy_orders:
                return
            remaining = []
            for order in self.buy_orders:
                snapshot = fetch_order_book(TICKER)
                asks = snapshot.asks
                total_available = sum(a['volume'] for a in asks) if asks else 0

                if total_available <= 0:
                    remaining.append(order)
                    continue

                to_fill = min(order['volume'], total_available)
                execute_order({**order}, to_fill)
                order['volume'] -= to_fill

                if order['volume'] > 0:
                    remaining.append(order)
            self.buy_orders = remaining

order_queue = OrderQueue()

# -----------------------------
# HELPERS: network interactions
# -----------------------------
def fetch_order_book(ticker: str, page=0, size=20, orderBy="DESC") -> 'OrderBookSnapshot':
    try:
        r = requests.get(f"{BASE_URL}/orderbook/{ticker}?page={page}&size={size}&orderBy={orderBy}", timeout=5)
        r.raise_for_status()
        data = r.json()
        asks = data.get("asks", {}).get("elements", [])
        bids = data.get("bids", {}).get("elements", [])
        return OrderBookSnapshot(ticker, asks, bids)
    except Exception as e:
        print(f"[fetch_order_book] error: {e}")
        return OrderBookSnapshot(ticker, [], [])

def execute_order(order: Dict, filled_volume: int):
    payload = {**order, "volume": filled_volume}
    try:
        r = requests.post(f"{BASE_URL}/trade/order", json=payload, timeout=5)
        try:
            j = r.json()
        except Exception:
            j = {"raw_status": r.status_code, "text": r.text}

        direction = order.get("orderDirection", "BUY")
        color = "\033[92m" if direction == "BUY" else "\033[91m"
        print(f"{color}[EXECUTE] {direction} {filled_volume} @ {order.get('price','market')} -> API resp: {j}\033[0m")
    except Exception as e:
        print(f"[EXECUTE] network error sending order: {e}")

# -----------------------------
# MODELS
# -----------------------------
class OrderBookSnapshot:
    def __init__(self, ticker: str, asks: List[Dict], bids: Optional[List[Dict]] = None):
        self.ticker = ticker
        self.asks = asks or []
        self.bids = bids or []

    @property
    def best_ask(self) -> Optional[float]:
        if not self.asks:
            return None
        try:
            return min(a['price'] for a in self.asks)
        except Exception:
            return None

    @property
    def best_bid(self) -> Optional[float]:
        if not self.bids:
            return None
        try:
            return max(b['price'] for b in self.bids)
        except Exception:
            return None

    def total_ask_volume(self) -> int:
        return sum(a.get('volume', 0) for a in self.asks)

    def total_bid_volume(self) -> int:
        return sum(b.get('volume', 0) for b in self.bids)

    def get_top_n_asks(self, n=5) -> List[Dict]:
        return sorted(self.asks, key=lambda x: x.get('price', float('inf')))[:n]

    def get_top_n_bids(self, n=5) -> List[Dict]:
        return sorted(self.bids, key=lambda x: x.get('price', 0), reverse=True)[:n]

# -----------------------------
# STRATEGIES
# -----------------------------
class TraderStrategy:
    def execute(self, snapshot: OrderBookSnapshot, trader: 'TraderThread'):
        raise NotImplementedError

class AggressiveTrader(TraderStrategy):
    def execute(self, snapshot, trader):
        top = snapshot.get_top_n_asks(1)
        if top:
            price = top[0]['price']
            trader.try_place_buy(price, 10)

class PassiveTrader(TraderStrategy):
    def execute(self, snapshot, trader):
        price = snapshot.best_ask
        if price is None:
            return
        if trader.last_price and price < trader.last_price - 1:
            trader.try_place_buy(price, 5)

class RandomTrader(TraderStrategy):
    def execute(self, snapshot, trader):
        side = random.choice(["BUY","SELL"])
        qty = random.randint(1,10)
        if side == "BUY":
            price = snapshot.best_ask
            trader.try_place_buy(price, qty)
        else:
            trader.try_place_sell(snapshot.best_ask or 0.0, qty)

class ETFTrader(TraderStrategy):
    def __init__(self, capital: float = 10_000_000, lookback=20):
        self.capital = capital
        self.lookback = lookback
        self.history: List[float] = []

    def execute(self, snapshot: 'OrderBookSnapshot', trader: 'TraderThread'):
        price = snapshot.best_ask
        if price is None or price <= 0:
            return

        self.history.append(price)
        if len(self.history) > self.lookback:
            self.history.pop(0)

        avg = sum(self.history) / len(self.history)

        if price < avg * 0.98:
            max_affordable = int(self.capital / price)
            qty = min(max_affordable, 1000)
            if qty > 0:
                trader.try_place_buy(price, qty)
                self.capital -= qty * price
        elif price > avg * 1.02:
            qty = min(1000, trader.portfolio.get(TICKER, 0))
            if qty > 0:
                trader.try_place_sell(price, qty)
                self.capital += qty * price

class MomentumTrader(TraderStrategy):
    """
    Kupuje, jeśli cena rośnie (momentum) — sprzedaje, jeśli spada.
    """
    def __init__(self):
        self.prev_price = None

    def execute(self, snapshot, trader):
        price = snapshot.best_ask
        if price is None:
            return

        if self.prev_price is not None:
            if price > self.prev_price * 1.002:  # wzrost >0.2%
                trader.try_place_buy(price, 10)
            elif price < self.prev_price * 0.998:  # spadek >0.2%
                trader.try_place_sell(price, 10)

        self.prev_price = price

class MeanReversionTrader(TraderStrategy):
    """
    Kupuje, gdy cena spada poniżej średniej; sprzedaje powyżej średniej.
    Typowa strategia „powrotu do średniej”.
    """
    def __init__(self, lookback=15):
        self.lookback = lookback
        self.history = []

    def execute(self, snapshot, trader):
        price = snapshot.best_ask
        if price is None:
            return

        self.history.append(price)
        if len(self.history) > self.lookback:
            self.history.pop(0)

        avg = sum(self.history) / len(self.history)
        if len(self.history) < self.lookback:
            return  # nie mamy jeszcze pełnej historii

        if price < avg * 0.98:  # 2% poniżej średniej
            trader.try_place_buy(price, 10)
        elif price > avg * 1.02:  # 2% powyżej średniej
            trader.try_place_sell(price, 10)

class LiquiditySeeker(TraderStrategy):
    """
    Handluje tylko wtedy, gdy na rynku jest wystarczająca płynność (duży wolumen).
    """
    def execute(self, snapshot, trader):
        total_ask_vol = snapshot.total_ask_volume()
        total_bid_vol = snapshot.total_bid_volume()
        price = snapshot.best_ask

        if not price:
            return

        if total_ask_vol > 1000 and total_bid_vol > 1000:
            if random.random() < 0.5:
                trader.try_place_buy(price, 20)
            else:
                trader.try_place_sell(price, 20)

class ScalperTrader(TraderStrategy):
    """
    Scalper – próbuje szybko kupować i sprzedawać na małych ruchach cenowych.
    """
    def __init__(self):
        self.last_price = None

    def execute(self, snapshot, trader):
        price = snapshot.best_ask
        if price is None:
            return
        if self.last_price is not None:
            delta = (price - self.last_price) / self.last_price
            if delta < -0.001:  # spadek >0.1%
                trader.try_place_buy(price, 5)
            elif delta > 0.001:  # wzrost >0.1%
                trader.try_place_sell(price, 5)
        self.last_price = price

class RiskAverseTrader(TraderStrategy):
    """
    Bardzo ostrożny trader – inwestuje tylko, gdy cena jest stabilna przez dłuższy czas.
    """
    def __init__(self, stability_window=5):
        self.history = []
        self.stability_window = stability_window

    def execute(self, snapshot, trader):
        price = snapshot.best_ask
        if price is None:
            return

        self.history.append(price)
        if len(self.history) > self.stability_window:
            self.history.pop(0)

        if len(self.history) < self.stability_window:
            return

        mean = sum(self.history) / len(self.history)
        variance = sum((p - mean)**2 for p in self.history) / len(self.history)

        if variance < 0.05:  # stabilny rynek
            trader.try_place_buy(price, 3)

class ContrarianTrader(TraderStrategy):
    """
    Trader kontrariański – robi odwrotnie do kierunku rynku.
    Kupuje przy spadkach, sprzedaje przy wzrostach.
    """
    def __init__(self):
        self.prev_price = None

    def execute(self, snapshot, trader):
        price = snapshot.best_ask
        if price is None:
            return

        if self.prev_price is not None:
            if price < self.prev_price * 0.995:
                trader.try_place_buy(price, 15)
            elif price > self.prev_price * 1.005:
                trader.try_place_sell(price, 15)
        self.prev_price = price

# -----------------------------
# TRADER THREAD
# -----------------------------
class TraderThread(threading.Thread):
    def __init__(self, name: str, strategy: TraderStrategy, capital: float = 0.0):
        super().__init__(daemon=True)
        self.name = name
        self.strategy = strategy
        self.last_price: Optional[float] = None
        self.running = True
        self.capital = capital
        self.portfolio: Dict[str, int] = {}

    def try_place_buy(self, price: Optional[float], volume: int):
        if price is not None:
            max_affordable = int(self.capital // price)
            if max_affordable <= 0:
                print(f"[{self.name}] brak środków na BUY {volume} @ {price}")
                return
            if max_affordable < volume:
                print(f"[{self.name}] ograniczam BUY do {max_affordable} ze względu na kapitał")
                volume = max_affordable

        order = {"orderDirection": "BUY", "orderType": "MARKET", "ticker": TICKER, "volume": volume}
        snapshot = fetch_order_book(TICKER)
        total_available = snapshot.total_ask_volume()

        if total_available >= volume:
            execute_order(order, volume)
            if price:
                self.capital -= price * volume
            self.portfolio[TICKER] = self.portfolio.get(TICKER, 0) + volume
        elif total_available > 0:
            execute_order(order, total_available)
            if price:
                self.capital -= price * total_available
            self.portfolio[TICKER] = self.portfolio.get(TICKER, 0) + total_available
            remaining = volume - total_available
            print(f"[{self.name}] partial filled {total_available}, enqueue {remaining}")
            order_queue.add_order({**order, "volume": remaining})
        else:
            print(f"[{self.name}] no asks -> enqueue BUY {volume}")
            order_queue.add_order(order)

    def try_place_sell(self, price: float, volume: int):
        current_shares = self.portfolio.get(TICKER, 0)
        if current_shares <= 0:
            print(f"[{self.name}] brak akcji do SELL {volume}")
            return
        volume = min(volume, current_shares)
        snapshot = fetch_order_book(TICKER)
        if price is None or price <= 0:
            bids = snapshot.get_top_n_bids(1)
            if bids:
                price = bids[0]['price']
            else:
                print(f"[{self.name}] no valid bid to sell {volume}, skipping")
                return
        order = {"orderDirection": "SELL", "orderType": "MARKET", "ticker": TICKER,
                 "volume": volume, "price": price}
        execute_order(order, volume)
        self.capital += price * volume
        self.portfolio[TICKER] -= volume

    def run(self):
        while self.running:
            snapshot = fetch_order_book(TICKER)
            if snapshot.best_ask is not None:
                self.last_price = snapshot.best_ask
            try:
                self.strategy.execute(snapshot, self)
            except Exception as e:
                print(f"[{self.name}] strategy exec error: {e}")
            time.sleep(random.uniform(MIN_SLEEP, MAX_SLEEP))

    def stop(self):
        self.running = False

# -----------------------------
# BOOTSTRAP / MAIN
# -----------------------------
def monitor_capital(traders: List[TraderThread], interval=1800):
    def _monitor():
        while True:
            for t in traders:
                logging.info(f"[CAPITAL] {t.name}: ${t.capital:.2f}")
            time.sleep(interval)
    thread = threading.Thread(target=_monitor, daemon=True)
    thread.start()
    return thread

def main():
    strategy_classes = [
            AggressiveTrader, PassiveTrader, RandomTrader, ETFTrader,
            MomentumTrader, MeanReversionTrader, LiquiditySeeker,
            ScalperTrader, RiskAverseTrader, ContrarianTrader
        ]

    trader_threads: List[TraderThread] = []
    for cls in strategy_classes:
        for i in range(N_INSTANCES):
            name = f"{cls.__name__}_{i+1}"
            strategy = cls()
            if cls == ETFTrader:
                capital = 10_000_000
            else:
                capital = INITIAL_CAPITAL
            t = TraderThread(name, strategy, capital)
            trader_threads.append(t)

    for t in trader_threads:
        t.start()
        print(f"Started trader {t.name}")

    try:
        time.sleep(10)  # pauza po starcie graczy
    except KeyboardInterrupt:
        print("Pauza przerwana przez użytkownika")

    monitor_capital(trader_threads, interval=1800)

    print(f"Simulation running: {len(trader_threads)} traders.")
    try:
        while True:
            order_queue.process_queue()
            time.sleep(QUEUE_PROCESS_INTERVAL)
    except KeyboardInterrupt:
        print("Stopping simulation...")
        for t in trader_threads:
            t.stop()
        time.sleep(1)

        print("\n===== FINAL STATE OF TRADERS =====")
        for t in trader_threads:
            shares = t.portfolio.get(TICKER, 0)
            print(f"{t.name}: Capital ${t.capital:,.2f}, Shares {shares}")

        now = datetime.datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
        filename = f"final_trader_state_{now}.log"
        with open(filename, "w") as f:
            for t in trader_threads:
                shares = t.portfolio.get(TICKER, 0)
                f.write(f"{t.name}: Capital ${t.capital:,.2f}, Shares {shares}\n")

if __name__ == "__main__":
    main()
