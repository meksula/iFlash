import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { LoginResponse, Me } from './models';

const TOKEN_KEY = 'ibp.token';

/** Holds the JWT and the current user's email/balance as signals the whole app reads. */
@Injectable({ providedIn: 'root' })
export class Auth {
  private http = inject(HttpClient);

  readonly token = signal<string | null>(localStorage.getItem(TOKEN_KEY));
  readonly email = signal<string | null>(null);
  readonly balance = signal<number | null>(null);
  readonly isAuthenticated = computed(() => this.token() !== null);

  login(email: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/auth/login', { email }).pipe(
      tap((res) => {
        localStorage.setItem(TOKEN_KEY, res.token);
        this.token.set(res.token);
        this.email.set(res.email);
      }),
    );
  }

  /** Refreshes email + balance from the server (call after login and after balance changes). */
  refreshMe(): Observable<Me> {
    return this.http.get<Me>('/api/auth/me').pipe(
      tap((me) => {
        this.email.set(me.email);
        this.balance.set(me.balance);
      }),
    );
  }

  setBalance(balance: number): void {
    this.balance.set(balance);
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    this.token.set(null);
    this.email.set(null);
    this.balance.set(null);
  }
}
