import { Component, HostListener, inject, OnInit, signal, ElementRef } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet, Router } from '@angular/router';
import { Auth } from '../../core/auth';
import { UsdPipe } from '../../core/usd';
import { Toast } from '../../core/toast';

@Component({
  selector: 'app-shell',
  imports: [RouterLink, RouterLinkActive, RouterOutlet, UsdPipe],
  templateUrl: './shell.html',
})
export class Shell implements OnInit {
  protected auth = inject(Auth);
  protected toast = inject(Toast);
  private router = inject(Router);
  private host = inject(ElementRef<HTMLElement>);

  readonly menuOpen = signal(false);

  ngOnInit(): void {
    // On a hard refresh the token survives but the in-memory profile does not.
    if (this.auth.email() === null) {
      this.auth.refreshMe().subscribe({ error: () => {} });
    }
  }

  @HostListener('document:click', ['$event'])
  closeMenu(event: MouseEvent): void {
    const menu = this.host.nativeElement.querySelector('.user-menu');
    if (menu && !menu.contains(event.target as Node)) {
      this.menuOpen.set(false);
    }
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
