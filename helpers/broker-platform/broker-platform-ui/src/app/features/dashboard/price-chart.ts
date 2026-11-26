import {
  AfterViewInit, Component, ElementRef, OnDestroy, effect, inject, input, signal, untracked, viewChild,
} from '@angular/core';
import { Subscription, interval, startWith, switchMap } from 'rxjs';
import {
  CandlestickData, CandlestickSeries, ColorType, createChart, IChartApi, ISeriesApi, UTCTimestamp,
} from 'lightweight-charts';
import { Api } from '../../core/api';
import { ChartPoint } from '../../core/models';

const INTERVALS = [
  { label: '1m', seconds: 60 },
  { label: '5m', seconds: 300 },
  { label: '15m', seconds: 900 },
  { label: '1h', seconds: 3600 },
  { label: '1d', seconds: 86400 },
];

@Component({
  selector: 'app-price-chart',
  template: `
    <header class="panel-head">
      <h2>Chart @if (ticker()) { <span class="chart-ticker">{{ ticker() }}</span> }</h2>
      <div class="intervals" role="group" aria-label="Candle interval">
        @for (option of intervals; track option.seconds) {
          <button type="button" [class.on]="seconds() === option.seconds" (click)="select(option.seconds)">
            {{ option.label }}
          </button>
        }
      </div>
    </header>
    <div class="chart-body">
      <div #chart class="chart-canvas"></div>
      @if (!ticker()) {
        <p class="chart-empty">Pick an instrument from favourites or portfolio.</p>
      }
    </div>
  `,
  styleUrl: './price-chart.scss',
  host: { class: 'panel chart-panel' },
})
export class PriceChart implements AfterViewInit, OnDestroy {
  private api = inject(Api);
  private readonly chartEl = viewChild.required<ElementRef<HTMLDivElement>>('chart');
  private chart?: IChartApi;
  private series?: ISeriesApi<'Candlestick'>;
  private poll?: Subscription;
  private points: ChartPoint[] = [];

  readonly ticker = input<string | null>(null);
  readonly seconds = signal(60);
  readonly intervals = INTERVALS;

  constructor() {
    effect(() => {
      const ticker = this.ticker();
      untracked(() => this.watch(ticker));
    });
  }

  ngAfterViewInit(): void {
    this.chart = createChart(this.chartEl().nativeElement, {
      autoSize: true,
      localization: { locale: 'en-US' },
      layout: {
        background: { type: ColorType.Solid, color: 'transparent' },
        textColor: '#8593ad',
        fontFamily: '"Helvetica Neue", Helvetica, Arial, sans-serif',
        fontSize: 11,
        attributionLogo: false,
      },
      grid: { vertLines: { color: '#17233a' }, horzLines: { color: '#17233a' } },
      rightPriceScale: { borderColor: '#26344f' },
      timeScale: { borderColor: '#26344f', timeVisible: true, secondsVisible: false },
    });
    this.series = this.chart.addSeries(CandlestickSeries, {
      upColor: '#2fc48c',
      downColor: '#f2575f',
      borderVisible: false,
      wickUpColor: '#2fc48c',
      wickDownColor: '#f2575f',
    });
    this.render(true);
  }

  ngOnDestroy(): void {
    this.poll?.unsubscribe();
    this.chart?.remove();
  }

  select(seconds: number): void {
    this.seconds.set(seconds);
    this.render(true);
  }

  private watch(ticker: string | null): void {
    this.poll?.unsubscribe();
    this.points = [];
    this.render(true);
    if (!ticker) {
      return;
    }
    let first = true;
    this.poll = interval(5000)
      .pipe(startWith(0), switchMap(() => this.api.quotes(ticker)))
      .subscribe((points) => {
        this.points = points;
        this.render(first);
        first = false;
      });
  }

  private render(fit: boolean): void {
    this.series?.setData(toCandles(this.points, this.seconds()));
    if (fit) {
      this.chart?.timeScale().fitContent();
    }
  }
}

function toCandles(points: ChartPoint[], seconds: number): CandlestickData[] {
  const candles: CandlestickData[] = [];
  for (const point of points) {
    const time = (Math.floor(point.time / seconds) * seconds) as UTCTimestamp;
    const last = candles[candles.length - 1];
    if (last && last.time === time) {
      last.high = Math.max(last.high, point.value);
      last.low = Math.min(last.low, point.value);
      last.close = point.value;
    } else {
      candles.push({ time, open: point.value, high: point.value, low: point.value, close: point.value });
    }
  }
  return candles;
}
