import { Component, OnDestroy, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { Subscription, forkJoin, interval, startWith, switchMap } from 'rxjs';
import { Api } from '../../core/api';
import { Auth } from '../../core/auth';
import { FavoriteQuote, Holding, Lot } from '../../core/models';
import { PercentPipe, UsdPipe } from '../../core/usd';
import { RelativeTimePipe } from '../../core/relative-time';
import { Toast, fillMessage } from '../../core/toast';
import { OrderDialog } from './order-dialog';
import { ConfirmDialog } from './confirm-dialog';
import { PriceChart } from './price-chart';
import { TableSort } from './table-sort';

@Component({
  selector: 'app-dashboard',
  imports: [NgTemplateOutlet, UsdPipe, PercentPipe, RelativeTimePipe, OrderDialog, ConfirmDialog, PriceChart],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit, OnDestroy {
  private api = inject(Api);
  private auth = inject(Auth);
  private toast = inject(Toast);
  private poll?: Subscription;
  private readonly orderDialog = viewChild.required(OrderDialog);
  private readonly confirmDialog = viewChild.required(ConfirmDialog);

  readonly favorites = signal<FavoriteQuote[]>([]);
  readonly holdings = signal<Holding[]>([]);
  readonly engineError = signal(false);
  readonly loading = signal(true);
  readonly expanded = signal<Set<string>>(new Set());
  readonly selected = signal<string | null>(null);
  readonly favoriteSort = new TableSort<FavoriteQuote>(
    {
      name: () => null,
      ticker: (quote) => quote.ticker,
      currentPrice: (quote) => quote.currentPrice,
      dailyChangePercent: (quote) => quote.dailyChangePercent,
    },
    'ticker',
  );
  readonly holdingSort = new TableSort<Holding>(
    {
      name: () => null,
      ticker: (holding) => holding.ticker,
      quantity: (holding) => holding.quantity,
      purchasePrice: (holding) => holding.purchasePrice,
      currentPrice: (holding) => holding.currentPrice,
      marketValue: (holding) => holding.marketValue,
      changePercent: (holding) => holding.changePercent,
    },
    'ticker',
  );
  readonly flash = signal<string | null>(null);

  readonly chartTicker = computed(
    () => this.selected() ?? this.favorites()[0]?.ticker ?? this.holdings()[0]?.ticker ?? null,
  );

  readonly sortedFavorites = computed(() => this.favoriteSort.apply(this.favorites()));
  readonly sortedHoldings = computed(() => this.holdingSort.apply(this.holdings()));

  ngOnInit(): void {
    this.poll = interval(5000)
      .pipe(
        startWith(0),
        switchMap(() => forkJoin({ dashboard: this.api.dashboard(), portfolio: this.api.portfolio() })),
      )
      .subscribe({
        next: ({ dashboard, portfolio }) => {
          this.favorites.set(dashboard.favorites);
          this.engineError.set(dashboard.engineError);
          this.holdings.set(portfolio.holdings);
          this.loading.set(false);
          this.auth.refreshMe().subscribe({ error: () => {} });
        },
        error: () => this.loading.set(false),
      });
  }

  ngOnDestroy(): void {
    this.poll?.unsubscribe();
  }

  toggle(ticker: string): void {
    const next = new Set(this.expanded());
    next.has(ticker) ? next.delete(ticker) : next.add(ticker);
    this.expanded.set(next);
  }

  openOrder(ticker: string, direction: string, event: Event): void {
    event.stopPropagation();
    this.orderDialog().open(ticker, direction);
  }

  showChart(ticker: string, event: Event): void {
    event.stopPropagation();
    this.selected.set(ticker);
  }

  async liquidatePosition(holding: Holding, event: Event): Promise<void> {
    event.stopPropagation();
    if (await this.confirmDialog().ask(`Sell all ${holding.quantity} ${holding.ticker} at market?`)) {
      this.settle(this.api.liquidatePosition(holding.ticker));
    }
  }

  async liquidateLot(holding: Holding, lot: Lot): Promise<void> {
    if (await this.confirmDialog().ask(`Sell ${lot.quantity} ${holding.ticker} from this lot at market?`)) {
      this.settle(this.api.liquidateLot(lot.id));
    }
  }

  refresh(): void {
    forkJoin({ dashboard: this.api.dashboard(), portfolio: this.api.portfolio() }).subscribe(
      ({ dashboard, portfolio }) => {
        this.favorites.set(dashboard.favorites);
        this.holdings.set(portfolio.holdings);
      },
    );
    this.auth.refreshMe().subscribe({ error: () => {} });
  }

  changeClass(value: number | null): string {
    return value === null || value === 0 ? '' : value > 0 ? 'buy' : 'sell';
  }

  private settle(request: ReturnType<Api['liquidateLot']>): void {
    this.flash.set(null);
    request.subscribe({
      next: (outcome) => {
        if (outcome.filledQuantity > 0) {
          this.toast.show(fillMessage(outcome));
        } else {
          this.flash.set(`Nothing sold — no matching liquidity for ${outcome.ticker} right now.`);
        }
        this.refresh();
      },
      error: (err) => this.flash.set(err?.error?.error ?? 'Liquidation failed.'),
    });
  }
}
