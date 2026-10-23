import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./pages/auth-page').then((module) => module.AuthPage), data: { mode: 'login' } },
  { path: 'register', loadComponent: () => import('./pages/auth-page').then((module) => module.AuthPage), data: { mode: 'register' } },
  { path: 'dashboard', canActivate: [authGuard], loadComponent: () => import('./pages/dashboard-page').then((module) => module.DashboardPage) },
  { path: 'goals/new', canActivate: [authGuard], loadComponent: () => import('./pages/goal-form-page').then((module) => module.GoalFormPage) },
  { path: 'goals/:goalId', canActivate: [authGuard], loadComponent: () => import('./pages/goal-detail-page').then((module) => module.GoalDetailPage) },
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: '**', redirectTo: 'dashboard' },
];
