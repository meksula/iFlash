import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { Api } from '../../core/api';
import { Instrument } from '../../core/models';

@Component({
  selector: 'app-instruments',
  imports: [RouterLink, DecimalPipe],
  templateUrl: './instruments.html',
})
export class Instruments implements OnInit {
  private api = inject(Api);

  readonly instruments = signal<Instrument[]>([]);
  readonly favorites = signal<Set<string>>(new Set());
  readonly engineError = signal(false);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.api.instruments().subscribe({
      next: (res) => {
        this.instruments.set(res.instruments);
        this.favorites.set(new Set(res.favorites));
        this.engineError.set(res.engineError);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  isFavorite(ticker: string): boolean {
    return this.favorites().has(ticker);
  }

  toggleFavorite(ticker: string): void {
    const on = this.favorites().has(ticker);
    const request = on ? this.api.removeFavorite(ticker) : this.api.addFavorite(ticker);
    request.subscribe({
      next: () => {
        const next = new Set(this.favorites());
        on ? next.delete(ticker) : next.add(ticker);
        this.favorites.set(next);
      },
    });
  }
}
