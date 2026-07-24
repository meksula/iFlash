import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { Api } from '../../core/api';
import { Auth } from '../../core/auth';
import { Payment } from '../../core/models';

@Component({
  selector: 'app-wallet',
  imports: [DecimalPipe, DatePipe],
  templateUrl: './wallet.html',
})
export class Wallet implements OnInit {
  private api = inject(Api);
  protected auth = inject(Auth);

  readonly payments = signal<Payment[]>([]);
  readonly amount = signal<number | null>(null);
  readonly message = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly busy = signal(false);
  readonly loading = signal(true);

  readonly quickAmounts = [100, 500, 1000, 5000];

  ngOnInit(): void {
    this.api.wallet().subscribe({
      next: (res) => {
        this.auth.setBalance(res.balance);
        this.payments.set(res.payments);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  topUp(event: Event): void {
    event.preventDefault();
    const amount = this.amount();
    if (!amount || amount <= 0 || this.busy()) {
      return;
    }
    this.busy.set(true);
    this.message.set(null);
    this.error.set(null);
    this.api.topUp(amount).subscribe({
      next: (res) => {
        this.auth.setBalance(res.balance);
        this.payments.set(res.payments);
        this.message.set(`Account topped up by $${amount.toFixed(2)}.`);
        this.amount.set(null);
        this.busy.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.error ?? 'Top-up failed.');
        this.busy.set(false);
      },
    });
  }
}
