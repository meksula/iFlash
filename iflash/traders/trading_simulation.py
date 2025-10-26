#!/usr/bin/env python3
import requests, random, time, threading, logging, datetime
from typing import List, Dict, Optional

# -----------------------------
# CONFIG
# -----------------------------
BASE_URL = "http://localhost:10023/api/v1"
TICKER = "NVDA"

N_INSTANCES = 3
MIN_SLEEP = 0.05
MAX_SLEEP = 0.25
QUEUE_PROCESS_INTERVAL = 0.5
LOOKBACK = 10
INITIAL_CAPITAL = 10000

logging.basicConfig(format="%(asctime)s [%(levelname)s] %(message)s", level=logging.INFO)

# -----------------------------
# ORDER QUEUE
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
                snapshot = fetch_order_book(TICKER, "ASK" if order['orderDirection']=="BID" else "BID")
                total_available = snapshot.total_ask_volume() if order['orderDirection']=="BID" else snapshot.total_bid_volume()
                if total_available <= 0:
                    remaining.append(order)
                    continue
                to_fill = min(order['volume'], total_available)
                execute_order({**order}, to_fill)
                order['volume'] -= to_fill
                if order['volume'] > 0:
                    remaining.append(order)
            self.orders = remaining

order_queue = OrderQueue()

# -----------------------------
# HELPERS
# -----------------------------
def fetch_order_book(ticker: str, orderDirection="BID", page=0, size=20):
    try:
        r = requests.get(f"{BASE_URL}/orderbook/{ticker}?page={page}&size={size}&orderBy=ASC&orderDirection={orderDirection}", timeout=5)
        r.raise_for_status()
        data = r.json().get("data", {}).get("elements", [])
        if orderDirection=="BID":
            return OrderBookSnapshot(ticker, [], data)
        return OrderBookSnapshot(ticker, data, [])
    except Exception as e:
        print(f"[fetch_order_book] error: {e}")
        return OrderBookSnapshot(ticker, [], [])

def execute_order(order: Dict, filled_volume: int):
    payload = {**order, "volume": filled_volume}
    try:
        r = requests.post(f"{BASE_URL}/trade/order", json=payload, timeout=5)
        try: j = r.json()
        except: j = {"raw_status": r.status_code, "text": r.text}
        color = "\033[92m" if order.get("orderDirection","BID")=="BID" else "\033[91m"
        print(f"{color}[EXECUTE] {order['orderDirection']} {filled_volume} @ {order.get('price','market')} -> {j}\033[0m")
    except Exception as e:
        print(f"[EXECUTE] network error: {e}")

# -----------------------------
# MODELS
# -----------------------------
class OrderBookSnapshot:
    def __init__(self, ticker: str, asks: List[Dict], bids: Optional[List[Dict]]=None):
        self.ticker = ticker
        self.asks = asks or []
        self.bids = bids or []

    @property
    def best_ask(self):
        return min((a['price'] for a in self.asks), default=None)

    @property
    def best_bid(self):
        return max((b['price'] for b in self.bids), default=None)

    def total_ask_volume(self): return sum(a.get('volume',0) for a in self.asks)
    def total_bid_volume(self): return sum(b.get('volume',0) for b in self.bids)
    def get_top_n_asks(self,n=5): return sorted(self.asks,key=lambda x:x['price'])[:n]
    def get_top_n_bids(self,n=5): return sorted(self.bids,key=lambda x:x['price'],reverse=True)[:n]

# -----------------------------
# TRADER STRATEGIES
# -----------------------------
class TraderStrategy:
    def execute(self, snapshot, trader): raise NotImplementedError

class AggressiveTrader(TraderStrategy):
    def execute(self,snapshot,trader):
        top=snapshot.get_top_n_asks(1)
        if top: trader.try_place_market("BID",10)

class PassiveTrader(TraderStrategy):
    def execute(self,snapshot,trader):
        price=snapshot.best_ask
        if price and trader.last_price and price<trader.last_price-1: trader.try_place_market("BID",5)

class RandomTrader(TraderStrategy):
    def execute(self,snapshot,trader):
        side=random.choice(["BID","ASK"])
        qty=random.randint(1,10)
        trader.try_place_market(side,qty)

class ETFTrader(TraderStrategy):
    def __init__(self,capital=10_000_000,lookback=20):
        self.capital=capital
        self.lookback=lookback
        self.history=[]
    def execute(self,snapshot,trader):
        price=snapshot.best_ask
        if price<=0: return
        self.history.append(price)
        if len(self.history)>self.lookback: self.history.pop(0)
        avg=sum(self.history)/len(self.history)
        if price<avg*0.98:
            qty=min(int(self.capital/price),1000)
            if qty>0: trader.try_place_market("BID",qty); self.capital-=qty*price
        elif price>avg*1.02:
            qty=min(1000,trader.portfolio.get(TICKER,0))
            if qty>0: trader.try_place_market("ASK",qty); self.capital+=qty*price

class MomentumTrader(TraderStrategy):
    def __init__(self): self.prev=None
    def execute(self,snapshot,trader):
        price=snapshot.best_ask
        if price is None: return
        if self.prev is not None:
            if price>self.prev*1.002: trader.try_place_market("BID",10)
            elif price<self.prev*0.998: trader.try_place_market("ASK",10)
        self.prev=price

class MeanReversionTrader(TraderStrategy):
    def __init__(self,lookback=15): self.lookback=lookback; self.history=[]
    def execute(self,snapshot,trader):
        price=snapshot.best_ask
        if price is None: return
        self.history.append(price)
        if len(self.history)>self.lookback: self.history.pop(0)
        if len(self.history)<self.lookback: return
        avg=sum(self.history)/len(self.history)
        if price<avg*0.98: trader.try_place_market("BID",10)
        elif price>avg*1.02: trader.try_place_market("ASK",10)

class ScalperTrader(TraderStrategy):
    def __init__(self): self.prev=None
    def execute(self,snapshot,trader):
        price=snapshot.best_ask
        if price is None: return
        if self.prev is not None:
            delta=(price-self.prev)/self.prev
            if delta<-0.001: trader.try_place_market("BID",5)
            elif delta>0.001: trader.try_place_market("ASK",5)
        self.prev=price

class RiskAverseTrader(TraderStrategy):
    def __init__(self,window=5): self.hist=[]; self.window=window
    def execute(self,snapshot,trader):
        price=snapshot.best_ask
        if price is None: return
        self.hist.append(price)
        if len(self.hist)>self.window: self.hist.pop(0)
        if len(self.hist)<self.window: return
        mean=sum(self.hist)/len(self.hist)
        var=sum((p-mean)**2 for p in self.hist)/len(self.hist)
        if var<0.05: trader.try_place_market("BID",3)

class ContrarianTrader(TraderStrategy):
    def __init__(self): self.prev=None
    def execute(self,snapshot,trader):
        price=snapshot.best_ask
        if price is None: return
        if self.prev is not None:
            if price<self.prev*0.995: trader.try_place_market("BID",15)
            elif price>self.prev*1.005: trader.try_place_market("ASK",15)
        self.prev=price

class LiquiditySeeker(TraderStrategy):
    def execute(self,snapshot,trader):
        if snapshot.total_ask_volume()>1000 and snapshot.total_bid_volume()>1000:
            side=random.choice(["BID","ASK"])
            trader.try_place_market(side,20)

class MarketMakerTrader(TraderStrategy):
    def execute(self,snapshot,trader):
        mid=None
        if snapshot.best_ask and snapshot.best_bid: mid=(snapshot.best_ask+snapshot.best_bid)/2
        elif snapshot.best_ask: mid=snapshot.best_ask
        elif snapshot.best_bid: mid=snapshot.best_bid
        else: return
        spread=mid*0.002
        trader.try_place_limit("BID", round(mid-spread,2), random.randint(5,15))
        trader.try_place_limit("ASK", round(mid+spread,2), random.randint(5,15))

# -----------------------------
# TRADER THREAD
# -----------------------------
class TraderThread(threading.Thread):
    def __init__(self,name:str,strategy:TraderStrategy,capital:float=0.0):
        super().__init__(daemon=True)
        self.name=name; self.strategy=strategy
        self.last_price=None; self.running=True
        self.capital=capital; self.portfolio={}
    def try_place_market(self,direction:str,volume:int):
        snapshot=fetch_order_book(TICKER,"ASK" if direction=="BID" else "BID")
        price=snapshot.best_ask if direction=="BID" else snapshot.best_bid
        if price is None: self.try_place_limit(direction,self.last_price or 100,volume); return
        execute_order({"orderDirection":direction,"orderType":"MARKET","ticker":TICKER,"volume":volume,"price":price},volume)
        if direction=="BID": self.capital-=price*volume; self.portfolio[TICKER]=self.portfolio.get(TICKER,0)+volume
        else: self.capital+=price*volume; self.portfolio[TICKER]=self.portfolio.get(TICKER,0)-volume
    def try_place_limit(self,direction:str,price:float,volume:int):
        execute_order({"orderDirection":direction,"orderType":"LIMIT","ticker":TICKER,"volume":volume,"price":price},volume)
    def run(self):
        while self.running:
            snapshot_bid=fetch_order_book(TICKER,"BID")
            snapshot_ask=fetch_order_book(TICKER,"ASK")
            if snapshot_ask.best_ask is not None: self.last_price=snapshot_ask.best_ask
            try: self.strategy.execute(OrderBookSnapshot(TICKER,snapshot_ask.asks,snapshot_bid.bids),self)
            except Exception as e: print(f"[{self.name}] strategy exec error: {e}")
            time.sleep(random.uniform(MIN_SLEEP,MAX_SLEEP))
    def stop(self): self.running=False

# -----------------------------
# MONITORING & MAIN
# -----------------------------
def monitor_capital(traders:List[TraderThread],interval=1800):
    def _mon():
        while True:
            for t in traders: logging.info(f"[CAPITAL] {t.name}: ${t.capital:.2f}")
            time.sleep(interval)
    thread=threading.Thread(target=_mon,daemon=True)
    thread.start(); return thread

def main():
    strategy_classes=[MarketMakerTrader,AggressiveTrader,PassiveTrader,RandomTrader,ETFTrader,MomentumTrader,MeanReversionTrader,ScalperTrader,RiskAverseTrader,ContrarianTrader,LiquiditySeeker]
    trader_threads=[]
    for cls in strategy_classes:
        for i in range(N_INSTANCES):
            name=f"{cls.__name__}_{i+1}"
            strategy=cls()
            capital=10_000_000 if cls==MarketMakerTrader else INITIAL_CAPITAL
            t=TraderThread(name,strategy,capital)
            trader_threads.append(t)
    for t in trader_threads: t.start(); print(f"Started trader {t.name}")
    monitor_capital(trader_threads)
    print(f"Simulation running: {len(trader_threads)} traders.")
    try:
        while True:
            order_queue.process_queue()
            time.sleep(QUEUE_PROCESS_INTERVAL)
    except KeyboardInterrupt:
        print("Stopping simulation...")
        for t in trader_threads: t.stop()
        time.sleep(1)
        print("\n===== FINAL STATE OF TRADERS =====")
        for t in trader_threads:
            shares=t.portfolio.get(TICKER,0)
            print(f"{t.name}: Capital ${t.capital:,.2f}, Shares {shares}")
        now=datetime.datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
        with open(f"final_trader_state_{now}.log","w") as f:
            for t in trader_threads:
                shares=t.portfolio.get(TICKER,0)
                f.write(f"{t.name}: Capital ${t.capital:,.2f}, Shares {shares}\n")

if __name__=="__main__": main()
