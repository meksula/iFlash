import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe, DecimalPipe } from '@angular/common';
import { Api } from '../../core/api';
import { Holding, Trade } from '../../core/models';

@Component({
  selector: 'app-portfolio',
  imports: [RouterLink, DecimalPipe, DatePipe],
  templateUrl: './portfolio.html',
})
export class Portfolio implements OnInit {
  private api = inject(Api);

  readonly holdings = signal<Holding[]>([]);
  readonly totalValue = signal(0);
  readonly trades = signal<Trade[]>([]);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.api.portfolio().subscribe({
      next: (res) => {
        this.holdings.set(res.holdings);
        this.totalValue.set(res.totalValue);
        this.trades.set(res.trades);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  isBuy(direction: string): boolean {
    return direction === 'BID';
  }
}
