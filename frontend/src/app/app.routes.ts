import { Routes } from '@angular/router';
import { authGuard, homeGuard, onboardingGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', canActivate: [homeGuard], loadComponent: () => import('./pages/home.page').then((m) => m.HomePage) },
  { path: 'login', loadComponent: () => import('./pages/auth.page').then((m) => m.AuthPage), data: { mode: 'login' } },
  { path: 'signup', loadComponent: () => import('./pages/auth.page').then((m) => m.AuthPage), data: { mode: 'signup' } },
  { path: 'auth/callback', loadComponent: () => import('./pages/callback.page').then((m) => m.CallbackPage) },
  { path: 'join', canActivate: [authGuard, onboardingGuard], loadComponent: () => import('./pages/join.page').then((m) => m.JoinPage) },
  { path: 'chef', canActivate: [authGuard], loadComponent: () => import('./pages/chef.page').then((m) => m.ChefPage) },
  { path: '**', redirectTo: '' },
];
