import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { Api } from '../../core/api';
import { Auth } from '../../core/auth';
import { Instrument } from '../../core/models';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, DecimalPipe],
  templateUrl: './dashboard.html',
})
export class Dashboard implements OnInit {
  private api = inject(Api);
  protected auth = inject(Auth);

  readonly favorites = signal<Instrument[]>([]);
  readonly engineError = signal(false);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.api.dashboard().subscribe({
      next: (res) => {
        this.favorites.set(res.favorites);
        this.engineError.set(res.engineError);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
