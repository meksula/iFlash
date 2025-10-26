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
BASE_URL = "http://localhost:10023/api/v1"
N_INSTANCES = 3
MIN_SLEEP = 0.05
MAX_SLEEP = 0.25
QUEUE_PROCESS_INTERVAL = 0.5
LOOKBACK = 10
INITIAL_CAPITAL = 10000

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
        self.orders: List[Dict] = []

    def add_order(self, order: Dict):
        with self.lock:
            self.orders.append(order)

    def process_queue(self):
        with self.lock:
            if not self.orders:
                return
            remaining = []
            for order in self.orders:
                snapshot = fetch_order_book(order['ticker'], order['orderDirection'])
                volume_available = snapshot.total_ask_volume() if order['orderDirection']=='BID' else snapshot.total_bid_volume()
                if volume_available <= 0:
                    remaining.append(order)
                    continue
                to_fill = min(order['volume'], volume_available)
                execute_order({**order}, to_fill)
                order['volume'] -= to_fill
                if order['volume'] > 0:
                    remaining.append(order)
            self.orders = remaining

order_queue = OrderQueue()

# -----------------------------
# HELPERS
# -----------------------------
def fetch_instruments() -> List[str]:
    try:
        r = requests.get(f"{BASE_URL}/instrument", timeout=5)
        r.raise_for_status()
        data = r.json()
        return [i['ticker'] for i in data]
    except Exception as e:
        print(f"[fetch_instruments] error: {e}")
        return []

def fetch_order_book(ticker: str, order_direction="BID", page=0, size=20, orderBy="ASC") -> 'OrderBookSnapshot':
    try:
        r = requests.get(f"{BASE_URL}/orderbook/{ticker}?page={page}&size={size}&orderBy={orderBy}&orderDirection={order_direction}", timeout=5)
        r.raise_for_status()
        data = r.json()
        elements = data.get("data", {}).get("elements", [])
        if order_direction=="BID":
            bids = elements
            asks = []
        else:
            asks = elements
            bids = []
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
        direction = order.get("orderDirection", "BID")
        color = "\033[92m" if direction=="BID" else "\033[91m"
        print(f"{color}[EXECUTE] {direction} {filled_volume} {order.get('ticker')} @ {order.get('price','market')} -> API resp: {j}\033[0m")
    except Exception as e:
        print(f"[EXECUTE] network error: {e}")

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
        return min(a['price'] for a in self.asks)

    @property
    def best_bid(self) -> Optional[float]:
        if not self.bids:
            return None
        return max(b['price'] for b in self.bids)

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

class RandomTrader(TraderStrategy):
    def execute(self, snapshot, trader):
        side = random.choice(["BID","ASK"])
        qty = random.randint(1,10)
        price = snapshot.best_ask if side=="BID" else snapshot.best_bid or 0.0
        if price is None:
           return
        if random.random()<0.7:
            trader.try_place_market(side, qty, snapshot.ticker, price)
        else:
            trader.try_place_limit(side, qty, snapshot.ticker, price*(0.995 if side=="BID" else 1.005))

class MarketMakerTrader(TraderStrategy):
    def execute(self, snapshot, trader):
        price = snapshot.best_ask or 100.0
        if price is None:
            trader.try_place_limit("ASK", 5, snapshot.ticker, 100)
            trader.try_place_limit("BID", 5, snapshot.ticker, 99)
        trader.try_place_limit("BID", 5, snapshot.ticker, price*0.995)
        trader.try_place_limit("ASK", 5, snapshot.ticker, price*1.005)

class AggressiveTrader(TraderStrategy):
    def execute(self, snapshot, trader):
        top = snapshot.get_top_n_asks(1)
        if top:
            price = top[0]['price']
            if price is None:
               return
            trader.try_place_market("BID", 10, snapshot.ticker, price)

class PassiveTrader(TraderStrategy):
    def execute(self, snapshot, trader):
        price = snapshot.best_ask
        if price is None:
            return
        last = trader.last_price.get(snapshot.ticker)
        if last and price < last - 1:
            trader.try_place_market("BID", 5, snapshot.ticker, price)
        trader.last_price[snapshot.ticker] = price

class ETFTrader(TraderStrategy):
    def __init__(self, capital: float = 10_000_000, lookback=20):
        self.capital = capital
        self.lookback = lookback
        self.history: Dict[str, List[float]] = {}

    def execute(self, snapshot, trader):
        price = snapshot.best_ask
        if price is None or price <= 0:
            return
        self.history.setdefault(snapshot.ticker, []).append(price)
        h = self.history[snapshot.ticker]
        if len(h) > self.lookback:
            h.pop(0)
        if len(h) < self.lookback:
            return
        avg = sum(h) / len(h)
        if price < avg * 0.98:
            max_affordable = int(self.capital / price)
            qty = min(max_affordable, 1000)
            if qty > 0:
                trader.try_place_market("BID", qty, snapshot.ticker, price)
                self.capital -= qty * price
        elif price > avg * 1.02:
            qty = min(1000, trader.portfolio.get(snapshot.ticker, 0))
            if qty > 0:
                trader.try_place_market("ASK", qty, snapshot.ticker, price)
                self.capital += qty * price

class MomentumTrader(TraderStrategy):
    def __init__(self):
        self.prev_price: Dict[str, Optional[float]] = {}

    def execute(self, snapshot, trader):
        price = snapshot.best_ask
        if price is None:
            return
        prev = self.prev_price.get(snapshot.ticker)
        if prev is not None:
            if price > prev * 1.002:
                trader.try_place_market("BID", 10, snapshot.ticker, price)
            elif price < prev * 0.998:
                trader.try_place_market("ASK", 10, snapshot.ticker, price)
        self.prev_price[snapshot.ticker] = price

class MeanReversionTrader(TraderStrategy):
    def __init__(self, lookback=15):
        self.lookback = lookback
        self.history: Dict[str, List[float]] = {}

    def execute(self, snapshot, trader):
        price = snapshot.best_ask
        if price is None:
            return
        self.history.setdefault(snapshot.ticker, []).append(price)
        h = self.history[snapshot.ticker]
        if len(h) > self.lookback:
            h.pop(0)
        if len(h) < self.lookback:
            return
        avg = sum(h) / len(h)
        if price < avg * 0.98:
            trader.try_place_market("BID", 10, snapshot.ticker, price)
        elif price > avg * 1.02:
            trader.try_place_market("ASK", 10, snapshot.ticker, price)

class LiquiditySeeker(TraderStrategy):
    def execute(self, snapshot, trader):
        total_ask = snapshot.total_ask_volume()
        total_bid = snapshot.total_bid_volume()
        price = snapshot.best_ask
        if not price:
            return
        if total_ask > 1000 and total_bid > 1000:
            if random.random() < 0.5:
                trader.try_place_market("BID", 20, snapshot.ticker, price)
            else:
                trader.try_place_market("ASK", 20, snapshot.ticker, price)

class ScalperTrader(TraderStrategy):
    def __init__(self):
        self.last_price: Dict[str, Optional[float]] = {}

    def execute(self, snapshot, trader):
        price = snapshot.best_ask
        if price is None:
            return
        last = self.last_price.get(snapshot.ticker)
        if last is not None:
            delta = (price - last) / last
            if delta < -0.001:
                trader.try_place_market("BID", 5, snapshot.ticker, price)
            elif delta > 0.001:
                trader.try_place_market("ASK", 5, snapshot.ticker, price)
        self.last_price[snapshot.ticker] = price

class RiskAverseTrader(TraderStrategy):
    def __init__(self, stability_window=5):
        self.stability_window = stability_window
        self.history: Dict[str, List[float]] = {}

    def execute(self, snapshot, trader):
        price = snapshot.best_ask
        if price is None:
            return
        self.history.setdefault(snapshot.ticker, []).append(price)
        h = self.history[snapshot.ticker]
        if len(h) > self.stability_window:
            h.pop(0)
        if len(h) < self.stability_window:
            return
        mean = sum(h) / len(h)
        variance = sum((p - mean) ** 2 for p in h) / len(h)
        if variance < 0.05:
            trader.try_place_market("BID", 3, snapshot.ticker, price)

class ContrarianTrader(TraderStrategy):
    def __init__(self):
        self.prev_price: Dict[str, Optional[float]] = {}

    def execute(self, snapshot, trader):
        price = snapshot.best_ask
        if price is None:
            return
        prev = self.prev_price.get(snapshot.ticker)
        if prev is not None:
            if price < prev * 0.995:
                trader.try_place_market("BID", 15, snapshot.ticker, price)
            elif price > prev * 1.005:
                trader.try_place_market("ASK", 15, snapshot.ticker, price)
        self.prev_price[snapshot.ticker] = price


# -----------------------------
# TRADER THREAD
# -----------------------------
class TraderThread(threading.Thread):
    def __init__(self, name, strategy, tickers: List[str], capital=0.0):
        super().__init__(daemon=True)
        self.name=name
        self.strategy=strategy
        self.tickers=tickers
        self.capital=capital
        self.portfolio={t:0 for t in tickers}
        self.last_price={t: None for t in tickers}
        self.running=True

    def try_place_market(self, side: str, volume: int, ticker: str, price: Optional[float]=None):
        max_affordable = int(self.capital // price) if price else volume
        if side=="BID" and max_affordable<=0:
            return
        volume = min(volume, max_affordable)
        order = {"orderDirection": side, "orderType": "MARKET", "ticker": ticker, "volume": volume}
        snapshot = fetch_order_book(ticker, side)
        total_available = snapshot.total_ask_volume() if side=="BID" else snapshot.total_bid_volume()
        if total_available>=volume:
            execute_order(order, volume)
            if side=="BID" and price:
                self.capital -= price*volume
            if side=="ASK":
                self.capital += price*volume
            if side=="BID":
                self.portfolio[ticker]+=volume
            else:
                self.portfolio[ticker]-=volume
        elif total_available>0:
            execute_order(order, total_available)
            if side=="BID" and price:
                self.capital-=price*total_available
            if side=="BID":
                self.portfolio[ticker]+=total_available
            else:
                self.portfolio[ticker]-=total_available
            remaining=volume-total_available
            order_queue.add_order({**order,"volume":remaining})
        else:
            order_queue.add_order(order)

    def try_place_limit(self, side: str, volume: int, ticker: str, price: float):
        order = {"orderDirection": side, "orderType": "LIMIT", "ticker": ticker, "volume": volume, "price": price}
        order_queue.add_order(order)

    def run(self):
        while self.running:
            ticker = random.choice(self.tickers)
            snapshot = fetch_order_book(ticker, "BID")
            if snapshot.best_ask:
                self.last_price[ticker]=snapshot.best_ask
            try:
                self.strategy.execute(snapshot, self)
            except Exception as e:
                print(f"[{self.name}] strategy exec error: {e}")
            time.sleep(random.uniform(MIN_SLEEP, MAX_SLEEP))

    def stop(self):
        self.running=False

# -----------------------------
# BOOTSTRAP / MAIN
# -----------------------------
def monitor_capital(traders: List[TraderThread], interval=1800):
    def _mon():
        while True:
            for t in traders:
                logging.info(f"{t.name}: ${t.capital:.2f}")
            time.sleep(interval)
    threading.Thread(target=_mon, daemon=True).start()

def main():
    tickers = fetch_instruments()
    print(f"Loaded {len(tickers)} tickers")
    strategy_classes = [
        MarketMakerTrader, RandomTrader,
        AggressiveTrader, PassiveTrader, ETFTrader,
        MomentumTrader, MeanReversionTrader, LiquiditySeeker,
        ScalperTrader, RiskAverseTrader, ContrarianTrader
    ]
    trader_threads=[]
    for cls in strategy_classes:
        for i in range(N_INSTANCES):
            name=f"{cls.__name__}_{i+1}"
            strategy=cls()
            capital=10_000_000 if cls==MarketMakerTrader else INITIAL_CAPITAL
            t=TraderThread(name,strategy,tickers,capital)
            trader_threads.append(t)
    for t in trader_threads:
        t.start()
        print(f"Started trader {t.name}")
    monitor_capital(trader_threads)
    try:
        while True:
            order_queue.process_queue()
            time.sleep(QUEUE_PROCESS_INTERVAL)
    except KeyboardInterrupt:
        print("Stopping simulation...")
        for t in trader_threads: t.stop()
        time.sleep(1)
        for t in trader_threads:
            print(f"{t.name}: Capital ${t.capital:.2f}, Portfolio {t.portfolio}")

if __name__=="__main__":
    main()
