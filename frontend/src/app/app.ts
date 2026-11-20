import { Component, inject, ViewChild } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';
import { LearningService } from './core/learning.service';
import { AiCoachModalComponent } from './shared/ai-coach-modal';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet, AiCoachModalComponent],
  template: `
    @if (auth.currentUser(); as user) {
      <a class="skip-link" href="#main-content">Skip to content</a>
      <header class="app-header">
        <a class="brand" routerLink="/dashboard"><span class="brand-mark">D</span> Disha AI</a>
        <nav aria-label="Main navigation">
          <a routerLink="/dashboard" routerLinkActive="active-link" [routerLinkActiveOptions]="{ exact: true }">My learning</a>
          <a class="new-goal-link" routerLink="/goals/new" routerLinkActive="active-link">New goal</a>
        </nav>
        <div class="header-actions">
          <button class="button button-primary" type="button" (click)="startLearning()" aria-label="Start learning">
            <span aria-hidden="true">▶</span> Start Learning
          </button>
          <button class="button button-secondary" type="button" (click)="openCoach()" aria-label="Ask AI Coach">
            <span aria-hidden="true">✦</span> Ask AI Coach
          </button>
        </div>
        <div class="account-menu">
          <span class="account-avatar" aria-hidden="true">{{ user.displayName.charAt(0).toUpperCase() }}</span>
          <span class="account-name">{{ user.displayName }}</span>
          <button class="button button-secondary button-small" type="button" (click)="logout()">Sign out</button>
        </div>
      </header>
    } @else {
      <header class="app-header landing-header">
        <a class="brand" routerLink="/" aria-label="Disha AI home"><span class="brand-mark">D</span> Disha AI</a>
        <nav aria-label="Main navigation">
          <a class="landing-nav-link" routerLink="/login">Sign in</a>
          <a class="button button-primary landing-nav-cta" routerLink="/register">Get started</a>
        </nav>
      </header>
    }
    <router-outlet />
    <app-ai-coach-modal #coachModal />
  `,
  styleUrl: './app.css',
})
export class App {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly learning = inject(LearningService);
  @ViewChild('coachModal') coachModal!: AiCoachModalComponent;

  logout(): void {
    this.auth.logout();
    void this.router.navigateByUrl('/login');
  }

  startLearning(): void {
    this.learning.dashboard().subscribe({
      next: (dashboard) => {
        const activeGoal = dashboard.activeGoals[0];
        if (activeGoal) {
          void this.router.navigateByUrl(`/goals/${activeGoal.id}`);
        } else {
          void this.router.navigateByUrl('/goals/new');
        }
      },
      error: () => {
        void this.router.navigateByUrl('/goals/new');
      }
    });
  }

  openCoach(): void {
    this.coachModal.open();
  }
}
