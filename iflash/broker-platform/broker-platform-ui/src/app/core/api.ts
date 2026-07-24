import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ChartPoint, DashboardResponse, InstrumentDetail, InstrumentsResponse, OrderBook,
  OrderRequest, OrderResponse, PortfolioResponse, Price, WalletResponse,
} from './models';

/** Thin typed wrapper over the iBP REST API. */
@Injectable({ providedIn: 'root' })
export class Api {
  private http = inject(HttpClient);

  dashboard(): Observable<DashboardResponse> {
    return this.http.get<DashboardResponse>('/api/dashboard');
  }

  instruments(): Observable<InstrumentsResponse> {
    return this.http.get<InstrumentsResponse>('/api/instruments');
  }

  instrument(ticker: string): Observable<InstrumentDetail> {
    return this.http.get<InstrumentDetail>(`/api/instruments/${encodeURIComponent(ticker)}`);
  }

  portfolio(): Observable<PortfolioResponse> {
    return this.http.get<PortfolioResponse>('/api/portfolio');
  }

  wallet(): Observable<WalletResponse> {
    return this.http.get<WalletResponse>('/api/wallet');
  }

  topUp(amount: number): Observable<WalletResponse> {
    return this.http.post<WalletResponse>('/api/wallet/topup', { amount });
  }

  placeOrder(order: OrderRequest): Observable<OrderResponse> {
    return this.http.post<OrderResponse>('/api/orders', order);
  }

  addFavorite(ticker: string): Observable<void> {
    return this.http.post<void>(`/api/favorites/${encodeURIComponent(ticker)}`, {});
  }

  removeFavorite(ticker: string): Observable<void> {
    return this.http.delete<void>(`/api/favorites/${encodeURIComponent(ticker)}`);
  }

  price(ticker: string): Observable<Price> {
    return this.http.get<Price>(`/api/market/price/${encodeURIComponent(ticker)}`);
  }

  quotes(ticker: string): Observable<ChartPoint[]> {
    return this.http.get<ChartPoint[]>(`/api/market/quotes/${encodeURIComponent(ticker)}`);
  }

  orderBook(ticker: string): Observable<OrderBook> {
    return this.http.get<OrderBook>(`/api/market/orderbook/${encodeURIComponent(ticker)}`);
  }
}
