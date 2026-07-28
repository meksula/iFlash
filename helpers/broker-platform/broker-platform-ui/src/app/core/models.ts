// Shapes returned by the iBP REST API (/api/**). Money/price come across as JSON numbers.

export interface Instrument {
  ticker: string;
  currentPrice: number | null;
}

export interface LoginResponse {
  token: string;
  email: string;
}

export interface Me {
  email: string;
  balance: number;
}

export interface DashboardResponse {
  favorites: Instrument[];
  engineError: boolean;
}

export interface InstrumentsResponse {
  instruments: Instrument[];
  favorites: string[];
  engineError: boolean;
}

export interface Direction {
  code: string;
  label: string;
}

export interface InstrumentDetail {
  ticker: string;
  price: number | null;
  favorite: boolean;
  position: number;
  orderTypes: string[];
  directions: Direction[];
  engineError: boolean;
}

export interface Holding {
  ticker: string;
  quantity: number;
  currentPrice: number;
  marketValue: number;
}

export interface Trade {
  id: number;
  ticker: string;
  direction: string;
  orderType: string;
  price: number | null;
  filledQuantity: number;
  cashAmount: number;
  createdAt: string;
}

export interface PortfolioResponse {
  holdings: Holding[];
  totalValue: number;
  trades: Trade[];
}

export interface Payment {
  id: number;
  amount: number;
  createdAt: string;
}

export interface WalletResponse {
  balance: number;
  payments: Payment[];
}

export interface OrderRequest {
  direction: string;
  orderType: string;
  ticker: string;
  price: number | null;
  volume: number;
}

export interface OrderOutcome {
  ticker: string;
  direction: string;
  orderType: string;
  requestedVolume: number;
  filledQuantity: number;
  cashAmount: number;
}

export interface OrderResponse {
  message: string;
  outcome: OrderOutcome;
}

export interface Price {
  quoteTimestamp: number;
  ticker: string;
  price: number | null;
}

export interface ChartPoint {
  time: number;
  value: number;
}

export interface BookLevel {
  price: number;
  volume: number;
}

export interface OrderBook {
  bids: BookLevel[];
  asks: BookLevel[];
}
