import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    @if (auth.currentUser(); as user) {
      <a class="skip-link" href="#main-content">Skip to content</a>
      <header class="app-header">
        <a class="brand" routerLink="/dashboard"><span class="brand-mark">D</span> Disha AI</a>
        <nav aria-label="Main navigation">
          <a routerLink="/dashboard" routerLinkActive="active-link" [routerLinkActiveOptions]="{ exact: true }">My learning</a>
          <a class="new-goal-link" routerLink="/goals/new" routerLinkActive="active-link">New goal</a>
        </nav>
        <div class="account-menu">
          <span class="account-avatar" aria-hidden="true">{{ user.displayName.charAt(0).toUpperCase() }}</span>
          <span class="account-name">{{ user.displayName }}</span>
          <button class="button button-secondary button-small" type="button" (click)="logout()">Sign out</button>
        </div>
      </header>
    }
    <router-outlet />
  `,
  styleUrl: './app.css',
})
export class App {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  logout(): void {
    this.auth.logout();
    void this.router.navigateByUrl('/login');
  }
}
