import { Component, inject, OnInit } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet, Router } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { Auth } from '../../core/auth';

@Component({
  selector: 'app-shell',
  imports: [RouterLink, RouterLinkActive, RouterOutlet, DecimalPipe],
  templateUrl: './shell.html',
})
export class Shell implements OnInit {
  protected auth = inject(Auth);
  private router = inject(Router);

  ngOnInit(): void {
    // On a hard refresh the token survives but the in-memory profile does not.
    if (this.auth.email() === null) {
      this.auth.refreshMe().subscribe({ error: () => {} });
    }
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
