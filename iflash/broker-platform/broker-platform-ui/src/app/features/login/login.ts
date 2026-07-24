import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Auth } from '../../core/auth';

@Component({
  selector: 'app-login',
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private auth = inject(Auth);
  private router = inject(Router);

  readonly email = signal('');
  readonly error = signal<string | null>(null);
  readonly busy = signal(false);

  submit(event: Event): void {
    event.preventDefault();
    const email = this.email().trim();
    if (!email || this.busy()) {
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    this.auth.login(email).subscribe({
      next: () => {
        this.auth.refreshMe().subscribe({
          next: () => this.router.navigate(['/']),
          error: () => this.router.navigate(['/']),
        });
      },
      error: (err) => {
        this.error.set(err?.error?.error ?? 'Could not sign in. Please try again.');
        this.busy.set(false);
      },
    });
  }
}
