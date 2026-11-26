import { Component, inject, OnInit, signal } from '@angular/core';
import { Api } from '../../core/api';
import { Trade } from '../../core/models';
import { UsdPipe } from '../../core/usd';
import { RelativeTimePipe } from '../../core/relative-time';

@Component({
  selector: 'app-history',
  imports: [UsdPipe, RelativeTimePipe],
  templateUrl: './history.html',
})
export class History implements OnInit {
  private api = inject(Api);

  readonly trades = signal<Trade[]>([]);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.api.portfolio().subscribe({
      next: (res) => {
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
