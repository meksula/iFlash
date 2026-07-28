import {
  AfterViewInit, Component, ElementRef, OnDestroy, OnInit, inject, signal, viewChild,
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { Subscription, interval, startWith, switchMap } from 'rxjs';
import {
  AreaSeries, ColorType, createChart, IChartApi, ISeriesApi, UTCTimestamp,
} from 'lightweight-charts';
import { Api } from '../../core/api';
import { Auth } from '../../core/auth';
import { BookLevel, Direction, InstrumentDetail } from '../../core/models';

@Component({
  selector: 'app-instrument',
  imports: [DecimalPipe],
  templateUrl: './instrument.html',
})
export class Instrument implements OnInit, AfterViewInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private api = inject(Api);
  private auth = inject(Auth);

  private readonly chartEl = viewChild.required<ElementRef<HTMLDivElement>>('chart');
  private chart?: IChartApi;
  private series?: ISeriesApi<'Area'>;
  private poll?: Subscription;

  readonly ticker = signal('');
  readonly price = signal<number | null>(null);
  readonly favorite = signal(false);
  readonly position = signal(0);
  readonly orderTypes = signal<string[]>([]);
  readonly directions = signal<Direction[]>([]);
  readonly engineError = signal(false);
  readonly bids = signal<BookLevel[]>([]);
  readonly asks = signal<BookLevel[]>([]);

  // order form
  readonly direction = signal('BID');
  readonly orderType = signal('MARKET');
  readonly orderPrice = signal<number | null>(null);
  readonly volume = signal<number | null>(null);
  readonly message = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly busy = signal(false);

  isMarket(): boolean {
    return this.orderType() === 'MARKET';
  }

  ngOnInit(): void {
    const ticker = (this.route.snapshot.paramMap.get('ticker') ?? '').toUpperCase();
    this.ticker.set(ticker);
    this.loadDetail();
  }

  ngAfterViewInit(): void {
    this.chart = createChart(this.chartEl().nativeElement, {
      autoSize: true,
      layout: { background: { type: ColorType.Solid, color: 'transparent' }, textColor: '#b09a8a' },
      grid: {
        vertLines: { color: 'rgba(255,173,92,0.06)' },
        horzLines: { color: 'rgba(255,173,92,0.06)' },
      },
      rightPriceScale: { borderColor: 'rgba(255,173,92,0.18)' },
      timeScale: { borderColor: 'rgba(255,173,92,0.18)', timeVisible: true, secondsVisible: false },
    });
    this.series = this.chart.addSeries(AreaSeries, {
      lineColor: '#ff7a1a',
      topColor: 'rgba(255,106,26,0.35)',
      bottomColor: 'rgba(255,106,26,0.02)',
      lineWidth: 2,
    });
    this.loadChart();
    this.startPolling();
  }

  ngOnDestroy(): void {
    this.poll?.unsubscribe();
    this.chart?.remove();
  }

  private loadDetail(): void {
    this.api.instrument(this.ticker()).subscribe({
      next: (d: InstrumentDetail) => {
        this.price.set(d.price);
        this.favorite.set(d.favorite);
        this.position.set(d.position);
        this.orderTypes.set(d.orderTypes);
        this.directions.set(d.directions);
        this.engineError.set(d.engineError);
      },
    });
  }

  private loadChart(): void {
    this.api.quotes(this.ticker()).subscribe({
      next: (points) => {
        this.series?.setData(points.map((p) => ({ time: p.time as UTCTimestamp, value: p.value })));
        this.chart?.timeScale().fitContent();
      },
    });
  }

  private startPolling(): void {
    this.poll = interval(3000)
      .pipe(startWith(0), switchMap(() => this.api.orderBook(this.ticker())))
      .subscribe({
        next: (book) => {
          this.bids.set(book.bids);
          this.asks.set(book.asks);
        },
      });
    // Price ticks on the same cadence, driving the chart's latest point.
    interval(3000)
      .pipe(startWith(0), switchMap(() => this.api.price(this.ticker())))
      .subscribe({
        next: (p) => {
          if (p?.price != null) {
            this.price.set(p.price);
            const time = Math.floor(Date.now() / 1000) as UTCTimestamp;
            this.series?.update({ time, value: p.price });
          }
        },
      });
  }

  toggleFavorite(): void {
    const on = this.favorite();
    const request = on ? this.api.removeFavorite(this.ticker()) : this.api.addFavorite(this.ticker());
    request.subscribe({ next: () => this.favorite.set(!on) });
  }

  placeOrder(event: Event): void {
    event.preventDefault();
    const volume = this.volume();
    if (!volume || volume <= 0 || this.busy()) {
      return;
    }
    if (!this.isMarket() && (!this.orderPrice() || this.orderPrice()! <= 0)) {
      this.error.set('Price is required for a ' + this.orderType() + ' order.');
      return;
    }
    this.busy.set(true);
    this.message.set(null);
    this.error.set(null);
    this.api
      .placeOrder({
        direction: this.direction(),
        orderType: this.orderType(),
        ticker: this.ticker(),
        price: this.isMarket() ? null : this.orderPrice(),
        volume,
      })
      .subscribe({
        next: (res) => {
          this.message.set(res.message);
          this.volume.set(null);
          this.busy.set(false);
          this.loadDetail();
          this.auth.refreshMe().subscribe({ error: () => {} });
        },
        error: (err) => {
          this.error.set(err?.error?.error ?? 'Order failed.');
          this.busy.set(false);
        },
      });
  }
}
