import { Routes } from '@angular/router';
import { authGuard } from './core/auth-guard';
import { Shell } from './features/shell/shell';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/login/login').then((m) => m.Login),
  },
  {
    path: '',
    component: Shell,
    canActivate: [authGuard],
    children: [
      { path: '', loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.Dashboard) },
      { path: 'instruments', loadComponent: () => import('./features/instruments/instruments').then((m) => m.Instruments) },
      { path: 'instruments/:ticker', loadComponent: () => import('./features/instrument/instrument').then((m) => m.Instrument) },
      { path: 'portfolio', loadComponent: () => import('./features/portfolio/portfolio').then((m) => m.Portfolio) },
      { path: 'wallet', loadComponent: () => import('./features/wallet/wallet').then((m) => m.Wallet) },
    ],
  },
  { path: '**', redirectTo: '' },
];
