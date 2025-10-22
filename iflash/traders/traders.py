#!/usr/bin/env python3
import requests
import random
import time
import threading
from typing import List, Dict, Optional

# -----------------------------
# CONFIG
# -----------------------------
BASE_URL = "http://localhost:10023/api/v1"  # <-- set your API base
TICKER = "NVDA"

# trader density / speed
N_INSTANCES = 3            # number of instances per strategy
MIN_SLEEP = 0.05           # per-trader min backoff (seconds)
MAX_SLEEP = 0.25           # per-trader max backoff (seconds)

# order queue processing interval (main thread)
QUEUE_PROCESS_INTERVAL = 0.5  # seconds

# stressor config
STRESSOR_COUNT = 2

# Lookback used by some strategies (internal)
LOOKBACK = 10

# -----------------------------
# THREAD-SAFE ORDER QUEUE
# -----------------------------
class OrderQueue:
    def __init__(self):
        self.lock = threading.Lock()
        self.buy_orders: List[Dict] = []  # list of orders waiting to be filled

    def add_order(self, order: Dict):
        with self.lock:
            self.buy_orders.append(order)

    def snapshot_and_clear_if_empty(self):
        with self.lock:
            return list(self.buy_orders)

    def process_queue(self):
        """
        Try to fill queued buy orders against current asks.
        If partial fill happens, decrement queued volume; otherwise remove.
        This calls fetch_order_book() internally.
        """
        with self.lock:
            if not self.buy_orders:
                return

            # We'll build a new list of remaining orders after attempt to fill
            remaining = []
            for order in self.buy_orders:
                # fetch current asks
                snapshot = fetch_order_book(TICKER)
                asks = snapshot.asks
                total_available = sum(a['volume'] for a in asks) if asks else 0

                if total_available <= 0:
                    # nothing to fill now, keep whole order
                    remaining.append(order)
                    continue

                # fill as much as possible
                to_fill = min(order['volume'], total_available)
                # execute fill (we use execute_order to send POST for the filled part)
                execute_order({**order}, to_fill)
                order['volume'] -= to_fill

                if order['volume'] > 0:
                    remaining.append(order)
                # else order fully filled -> drop it

            # replace queue with remaining
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
        return OrderBookSnapshot(ticker, asks)
    except Exception as e:
        # on error, return empty snapshot
        print(f"[fetch_order_book] error: {e}")
        return OrderBookSnapshot(ticker, [])

def execute_order(order: Dict, filled_volume: int):
    """
    Send a MARKET order for the filled_volume amount.
    We post to /trade/order with volume=filled_volume. If the API responds with
    partial transactions, we simply log them.
    """
    payload = {**order, "volume": filled_volume}
    try:
        r = requests.post(f"{BASE_URL}/trade/order", json=payload, timeout=5)
        # print summary
        try:
            j = r.json()
        except Exception:
            j = {"raw_status": r.status_code, "text": r.text}
        print(f"[EXECUTE] {order.get('orderDirection')} {filled_volume} @ {order.get('price','market')} -> API resp: {j}")
    except Exception as e:
        print(f"[EXECUTE] network error sending order: {e}")

# -----------------------------
# MODELS
# -----------------------------
class OrderBookSnapshot:
    def __init__(self, ticker: str, asks: List[Dict]):
        self.ticker = ticker
        # expects asks elements like: { "orderCreationDate": "...", "price": 185.10, "volume": 10 }
        self.asks = asks or []

    @property
    def best_ask(self) -> Optional[float]:
        if not self.asks:
            return None
        try:
            return min(a['price'] for a in self.asks)
        except Exception:
            return None

    def total_ask_volume(self) -> int:
        return sum(a.get('volume', 0) for a in self.asks)

    def get_top_n_asks(self, n=5) -> List[Dict]:
        return sorted(self.asks, key=lambda x: x.get('price', float('inf')))[:n]

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

class MomentumTrader(TraderStrategy):
    def __init__(self, lookback=LOOKBACK):
        self.lookback = lookback
        self.history: List[float] = []

    def execute(self, snapshot, trader):
        price = snapshot.best_ask
        if price is None:
            return
        self.history.append(price)
        if len(self.history) > self.lookback:
            self.history.pop(0)
        if len(self.history) < 2:
            return
        if self.history[-1] > self.history[-2]:
            trader.try_place_buy(price, 5)
        elif self.history[-1] < self.history[-2]:
            trader.try_place_sell(price, 5)

class MeanReversionTrader(TraderStrategy):
    def __init__(self, lookback=LOOKBACK):
        self.lookback = lookback
        self.history: List[float] = []

    def execute(self, snapshot, trader):
        price = snapshot.best_ask
        if price is None:
            return
        self.history.append(price)
        if len(self.history) > self.lookback:
            self.history.pop(0)
        if len(self.history) < self.lookback:
            return
        avg = sum(self.history)/len(self.history)
        if price > avg * 1.01:
            trader.try_place_sell(price, 5)
        elif price < avg * 0.99:
            trader.try_place_buy(price, 5)

class VolumeWeightedTrader(TraderStrategy):
    def execute(self, snapshot, trader):
        top = snapshot.get_top_n_asks(3)
        if not top:
            return
        total = sum(a.get('volume',0) for a in top) or 1
        for a in top:
            weight = a.get('volume',0)/total
            qty = max(1, int(weight * 10))
            trader.try_place_buy(a.get('price'), qty)

class RandomBreakoutTrader(TraderStrategy):
    def __init__(self, lookback=LOOKBACK):
        self.lookback = lookback
        self.history: List[float] = []

    def execute(self, snapshot, trader):
        price = snapshot.best_ask
        if price is None:
            return
        self.history.append(price)
        if len(self.history) > self.lookback:
            self.history.pop(0)
        if len(self.history) < self.lookback:
            return
        if price >= max(self.history):
            trader.try_place_buy(price, 5)
        elif price <= min(self.history):
            trader.try_place_sell(price, 5)

# -----------------------------
# TRADER THREAD
# -----------------------------
class TraderThread(threading.Thread):
    def __init__(self, name: str, strategy: TraderStrategy):
        super().__init__(daemon=True)
        self.name = name
        self.strategy = strategy
        self.last_price: Optional[float] = None
        self.running = True

    def try_place_buy(self, price: Optional[float], volume: int):
        """
        Decide whether to send market buy:
         - if no asks (price is None) -> enqueue entire order
         - if asks exist: attempt immediate fill for available volume (by calling API),
           if partial fill, the remainder goes to queue
        """
        order = {
            "orderDirection": "BUY",
            "orderType": "MARKET",
            "ticker": TICKER,
            "volume": volume
        }
        # fetch local snapshot for decision
        snapshot = fetch_order_book(TICKER)
        total_available = snapshot.total_ask_volume()

        if total_available >= volume:
            # fully executable now
            execute_order(order, volume)
        elif total_available > 0:
            # partial fill now, remainder to queue
            execute_order(order, total_available)
            remaining = volume - total_available
            print(f"[{self.name}] partial filled {total_available}, enqueue {remaining}")
            order_queue.add_order({**order, "volume": remaining})
        else:
            # nothing available -> enqueue whole order
            print(f"[{self.name}] no asks -> enqueue BUY {volume}")
            order_queue.add_order(order)

    def try_place_sell(self, price: float, volume: int):
        order = {
            "orderDirection": "SELL",
            "orderType": "MARKET",
            "ticker": TICKER,
            "volume": volume,
            "price": price
        }
        # for SELL we assume best effort (server will accept or reject)
        execute_order(order, volume)

    def run(self):
        while self.running:
            snapshot = fetch_order_book(TICKER)
            if snapshot.best_ask is not None:
                self.last_price = snapshot.best_ask

            try:
                self.strategy.execute(snapshot, self)
            except Exception as e:
                print(f"[{self.name}] strategy exec error: {e}")

            # random backoff per trader to create jitter
            time.sleep(random.uniform(MIN_SLEEP, MAX_SLEEP))

    def stop(self):
        self.running = False

# -----------------------------
# NAUGHTY STRESSOR (burst generator)
# -----------------------------
class NaughtyButLegalStressor(threading.Thread):
    def __init__(self,
                 name: str,
                 base_order_size: int = 5,
                 burst_probability: float = 0.15,
                 burst_min_orders: int = 5,
                 burst_max_orders: int = 20,
                 per_order_backoff=(0.01, 0.08),
                 session_volume_cap: int = 500):
        super().__init__(daemon=True)
        self.name = name
        self.ticker = TICKER
        self.base_order_size = base_order_size
        self.burst_probability = burst_probability
        self.burst_min_orders = burst_min_orders
        self.burst_max_orders = burst_max_orders
        self.per_order_backoff = per_order_backoff
        self.session_volume_cap = session_volume_cap
        self.session_volume_used = 0
        self.running = True

    def _can_place(self, qty: int) -> bool:
        return (self.session_volume_used + qty) <= self.session_volume_cap

    def _place_market(self, direction: str, price, qty: int):
        if not self._can_place(qty):
            remaining = self.session_volume_cap - self.session_volume_used
            if remaining <= 0:
                # reached cap
                print(f"[{self.name}] session cap reached; skipping")
                return
            qty = min(qty, remaining)

        order = {"orderDirection": direction, "orderType": "MARKET", "ticker": self.ticker, "volume": qty}
        if direction == "SELL":
            order["price"] = price

        execute_order(order, qty)
        self.session_volume_used += qty
        print(f"[{self.name}] placed {direction} {qty} (used {self.session_volume_used}/{self.session_volume_cap})")

    def maybe_act(self, snapshot: OrderBookSnapshot):
        # update snapshot price not stored
        if random.random() < self.burst_probability:
            n_orders = random.randint(self.burst_min_orders, self.burst_max_orders)
            print(f"[{self.name}] START BURST {n_orders} orders")
            for i in range(n_orders):
                side = "BUY" if random.random() < 0.55 else "SELL"
                qty = max(1, int(random.gauss(self.base_order_size, self.base_order_size*0.5)))
                price = snapshot.best_ask if side == "BUY" else None
                # For BUY we use same logic as TraderThread (respect queue)
                if side == "BUY":
                    # reuse TraderThread logic for buys (partial/queue)
                    # create a temporary minimal 'order' and call the same checking
                    order = {"orderDirection": "BUY", "orderType": "MARKET", "ticker": self.ticker, "volume": qty}
                    snap = fetch_order_book(self.ticker)
                    total_available = snap.total_ask_volume()
                    if total_available >= qty:
                        execute_order(order, qty)
                    elif total_available > 0:
                        execute_order(order, total_available)
                        order_queue.add_order({**order, "volume": qty - total_available})
                    else:
                        order_queue.add_order(order)
                    self.session_volume_used += qty  # count attempted volume towards cap
                else:
                    self._place_market("SELL", price, qty)
                time.sleep(random.uniform(self.per_order_backoff[0], self.per_order_backoff[1]))
            print(f"[{self.name}] END BURST")
            return

        # small prob to place normal market action
        if random.random() < 0.35:
            side = "BUY" if random.random() < 0.5 else "SELL"
            qty = max(1, int(random.gauss(self.base_order_size, self.base_order_size*0.4)))
            if side == "BUY":
                order = {"orderDirection": "BUY", "orderType": "MARKET", "ticker": self.ticker, "volume": qty}
                snap = fetch_order_book(self.ticker)
                total_available = snap.total_ask_volume()
                if total_available >= qty:
                    execute_order(order, qty)
                elif total_available > 0:
                    execute_order(order, total_available)
                    order_queue.add_order({**order, "volume": qty - total_available})
                else:
                    order_queue.add_order(order)
                self.session_volume_used += qty
            else:
                self._place_market("SELL", None, qty)

    def run(self):
        while self.running:
            snapshot = fetch_order_book(self.ticker)
            try:
                self.maybe_act(snapshot)
            except Exception as e:
                print(f"[{self.name}] error in maybe_act: {e}")
            time.sleep(random.uniform(MIN_SLEEP, MAX_SLEEP))

    def stop(self):
        self.running = False

# -----------------------------
# BOOTSTRAP / MAIN
# -----------------------------
def main():
    # strategy classes to instantiate
    strategy_classes = [
        AggressiveTrader,
        PassiveTrader,
        RandomTrader,
        MomentumTrader,
        MeanReversionTrader,
        VolumeWeightedTrader,
        RandomBreakoutTrader
    ]

    # create trader threads
    trader_threads: List[TraderThread] = []
    for cls in strategy_classes:
        for i in range(N_INSTANCES):
            name = f"{cls.__name__}_{i+1}"
            # instantiate strategy (passive classes with params create themselves)
            strategy = cls() if callable(cls) else cls
            t = TraderThread(name, strategy)
            trader_threads.append(t)

    # stressors
    stressors: List[NaughtyButLegalStressor] = []
    for i in range(STRESSOR_COUNT):
        s = NaughtyButLegalStressor(name=f"Stressor_{i+1}",
                                    base_order_size=5,
                                    burst_probability=0.18,
                                    burst_min_orders=6,
                                    burst_max_orders=18,
                                    per_order_backoff=(0.005, 0.04),
                                    session_volume_cap=500)
        stressors.append(s)

    # start traders and stressors
    for t in trader_threads:
        t.start()
        print(f"Started trader {t.name}")
    for s in stressors:
        s.start()
        print(f"Started stressor {s.name}")

    print(f"Simulation running: {len(trader_threads)} traders, {len(stressors)} stressors.")
    try:
        while True:
            # process queued buys periodically
            order_queue.process_queue()
            time.sleep(QUEUE_PROCESS_INTERVAL)
    except KeyboardInterrupt:
        print("Stopping simulation...")
        for t in trader_threads:
            t.stop()
        for s in stressors:
            s.stop()
        # give threads a moment
        time.sleep(1)

if __name__ == "__main__":
    main()
