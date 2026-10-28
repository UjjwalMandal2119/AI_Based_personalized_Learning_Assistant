import { Component, OnInit, inject } from '@angular/core';
import { DatePipe, PercentPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Dashboard, StudyTask } from '../core/models';
import { LearningService } from '../core/learning.service';
import { DashboardCardComponent } from '../shared/dashboard-card';
import { ErrorMessageComponent } from '../shared/error-message';
import { LoadingStateComponent } from '../shared/loading-state';
import { TaskItemComponent } from '../shared/task-item';

@Component({
  standalone: true,
  imports: [DatePipe, PercentPipe, RouterLink, DashboardCardComponent,
    ErrorMessageComponent, LoadingStateComponent, TaskItemComponent],
  template: `
    <main class="page-shell" id="main-content">
      @if (loading) {
        <app-loading-state />
      } @else if (error) {
        <app-error-message [message]="error" />
        <button class="button button-secondary" (click)="load()">Try again</button>
      } @else if (dashboard; as data) {
        <section class="welcome-row">
          <div>
            <p class="eyebrow">{{ today | date:'EEEE, MMMM d' }}</p>
            <h1>Good to see you, {{ data.displayName }} <span aria-hidden="true">✦</span></h1>
            <p class="muted">A focused session today brings you closer to what you want to achieve.</p>
          </div>
          <a class="button button-primary" routerLink="/goals/new">＋ Create a learning goal</a>
        </section>

        <section class="stats-grid" aria-label="Learning progress">
          <app-dashboard-card title="Active goals">
            <div class="stat-value">{{ data.activeGoals.length }}</div>
            <p class="stat-note">Keep your momentum going</p>
          </app-dashboard-card>
          <app-dashboard-card title="Tasks completed">
            <div class="stat-value">{{ data.completedTasks }}</div>
            <p class="stat-note">Every finished task counts</p>
          </app-dashboard-card>
          <app-dashboard-card title="Tasks remaining">
            <div class="stat-value">{{ data.remainingTasks }}</div>
            <p class="stat-note">Ready when you are</p>
          </app-dashboard-card>
          <app-dashboard-card title="Overall progress">
            <div class="stat-value">{{ progress | percent }}</div>
            <div class="progress-track" role="progressbar" [attr.aria-valuenow]="progress * 100"
                 aria-valuemin="0" aria-valuemax="100" aria-label="Completed task progress">
              <span [style.width.%]="progress * 100"></span>
            </div>
            <p class="stat-note">Across your study tasks</p>
          </app-dashboard-card>
        </section>

        <section class="dashboard-columns">
          <div class="dashboard-main">
            <app-dashboard-card title="Your learning goals">
              @if (data.activeGoals.length) {
                <div class="goal-list">
                  @for (goal of data.activeGoals; track goal.id) {
                    <a class="goal-row" [routerLink]="['/goals', goal.id]">
                      <span class="goal-icon" aria-hidden="true">{{ goal.subject.slice(0, 1) }}</span>
                      <span class="goal-copy"><strong>{{ goal.title }}</strong><small>{{ goal.subject }} · {{ goal.topic }}</small></span>
                      <span class="goal-arrow" aria-hidden="true">↗</span>
                    </a>
                  }
                </div>
              } @else {
                <div class="empty-state">
                  <span class="empty-icon" aria-hidden="true">✧</span>
                  <h3>Your next goal starts here</h3>
                  <p>Choose a topic you’re curious about and make a plan.</p>
                  <a class="text-link" routerLink="/goals/new">Create your first goal →</a>
                </div>
              }
            </app-dashboard-card>

            <app-dashboard-card title="Recent learning activity">
              @if (data.recentActivity.length) {
                <div class="task-list">
                  @for (task of data.recentActivity; track task.id) {
                    <app-task-item [task]="task" (completionChange)="toggleTask($event)" />
                  }
                </div>
              } @else {
                <p class="empty-line">Completed tasks will show up here.</p>
              }
            </app-dashboard-card>
          </div>
          <aside class="dashboard-side">
            <app-dashboard-card title="Today's focus">
              @if (data.tasksDueToday.length) {
                <div class="task-list">
                  @for (task of data.tasksDueToday; track task.id) {
                    <app-task-item [task]="task" (completionChange)="toggleTask($event)" />
                  }
                </div>
              } @else {
                <div class="empty-state compact"><h3>Room to breathe</h3><p>No tasks due today. Enjoy the space or get ahead.</p></div>
              }
            </app-dashboard-card>
            <app-dashboard-card title="Coming up">
              @if (data.upcomingTasks.length) {
                <div class="task-list">
                  @for (task of data.upcomingTasks; track task.id) {
                    <app-task-item [task]="task" (completionChange)="toggleTask($event)" />
                  }
                </div>
              } @else {
                <p class="empty-line">Nothing coming up in the next week.</p>
              }
            </app-dashboard-card>
          </aside>
        </section>
      }
    </main>
  `,
})
export class DashboardPage implements OnInit {
  private readonly learning = inject(LearningService);
  dashboard: Dashboard | null = null;
  loading = true;
  error = '';
  readonly today = new Date();

  get progress(): number {
    const total = (this.dashboard?.completedTasks ?? 0) + (this.dashboard?.remainingTasks ?? 0);
    return total ? (this.dashboard?.completedTasks ?? 0) / total : 0;
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.learning.dashboard().subscribe({
      next: (dashboard) => { this.dashboard = dashboard; },
      error: (error: HttpErrorResponse) => {
        this.error = error.error?.detail ?? 'Your dashboard could not be loaded. Please try again.';
        this.loading = false;
      },
      complete: () => { this.loading = false; },
    });
  }

  toggleTask(change: { id: string; completed: boolean }): void {
    this.learning.setTaskCompletion(change.id, change.completed).subscribe({
      next: () => this.load(),
      error: (error: HttpErrorResponse) => {
        this.error = error.error?.detail ?? 'The task could not be updated.';
      },
    });
  }
}
