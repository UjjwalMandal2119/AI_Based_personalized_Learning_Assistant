import { Component, OnInit, inject } from '@angular/core';
import { DatePipe, LowerCasePipe, PercentPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AiRecommendation, Dashboard, StudyTask } from '../core/models';
import { LearningService } from '../core/learning.service';
import { DashboardCardComponent } from '../shared/dashboard-card';
import { ErrorMessageComponent } from '../shared/error-message';
import { LoadingStateComponent } from '../shared/loading-state';
import { TaskItemComponent } from '../shared/task-item';

@Component({
  standalone: true,
  imports: [DatePipe, LowerCasePipe, PercentPipe, RouterLink, DashboardCardComponent,
    ErrorMessageComponent, LoadingStateComponent, TaskItemComponent],
  template: `
    <main class="page-shell" id="main-content">
      @if (loading) {
        <app-loading-state />
      } @else if (error) {
        <app-error-message [message]="error" />
        <button class="button button-secondary" style="margin-top:1rem" (click)="load()">Try again</button>
      } @else if (dashboard; as data) {

        <!-- Welcome row -->
        <section class="welcome-row">
          <div>
            <p class="eyebrow">{{ today | date:'EEEE, MMMM d' }}</p>
            <h1>Good to see you, {{ data.displayName }} <span aria-hidden="true">✦</span></h1>
            <p class="muted">A focused session today brings you closer to what you want to achieve.</p>
          </div>
          <a class="button button-primary" routerLink="/goals/new">＋ Create a learning goal</a>
        </section>

        <!-- Stats row -->
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

        <!-- AI Coach panel (full width, above columns) -->
        @if (data.aiRecommendation; as rec) {
          <section class="ai-coach-panel" aria-label="AI Learning Coach">
            <div class="ai-coach-inner">
              <div class="ai-coach-header">
                <span class="ai-coach-icon" aria-hidden="true">✦</span>
                <div>
                  <p class="eyebrow" style="color:#9a6a35">AI Learning Coach</p>
                  <h2 style="margin-bottom:0">{{ rec.title }}</h2>
                </div>
                <button class="button button-secondary button-small ai-coach-refresh"
                        type="button" [disabled]="generatingRec"
                        (click)="refreshRecommendation()"
                        [attr.aria-busy]="generatingRec">
                  {{ generatingRec ? 'Thinking…' : '↻ Refresh' }}
                </button>
              </div>
              <p class="ai-coach-body">{{ rec.recommendation }}</p>
              <p class="ai-coach-reason"><strong>Why:</strong> {{ rec.reason }}</p>
              @if (rec.suggestedActions.length) {
                <ul class="ai-coach-actions">
                  @for (action of rec.suggestedActions; track $index) {
                    <li>{{ action }}</li>
                  }
                </ul>
              }
              <div class="ai-coach-footer">
                <span class="count-pill">{{ rec.topic }}</span>
                <span class="count-pill">{{ rec.activityType | lowercase }}</span>
                <span class="count-pill">{{ rec.estimatedMinutes }} min</span>
                @if (rec.learningGoalId) {
                  <a class="button button-primary button-small"
                     [routerLink]="['/goals', rec.learningGoalId]">Start now →</a>
                }
              </div>
            </div>
          </section>
        } @else if (!generatingRec) {
          <section class="ai-coach-panel ai-coach-empty" aria-label="AI Learning Coach">
            <div class="ai-coach-inner">
              <div class="ai-coach-header">
                <span class="ai-coach-icon" aria-hidden="true">✦</span>
                <div>
                  <p class="eyebrow" style="color:#9a6a35">AI Learning Coach</p>
                  <h2 style="margin-bottom:0">Get a personalised recommendation</h2>
                </div>
              </div>
              <p class="muted">Your AI coach analyses your goals and tasks to suggest what to study next.</p>
              @if (recError) { <app-error-message [message]="recError" /> }
              @if (data.activeGoals.length) {
                <button class="button button-primary" type="button" (click)="refreshRecommendation()">
                  ✦ Generate recommendation
                </button>
              } @else {
                <p class="muted" style="font-size:0.85rem">Create a learning goal first to unlock AI recommendations.</p>
              }
            </div>
          </section>
        } @else {
          <section class="ai-coach-panel" aria-label="AI Learning Coach">
            <div class="ai-coach-inner">
              <p class="loading" role="status" style="padding:0;text-align:left">
                ✦ Your AI coach is thinking…
              </p>
            </div>
          </section>
        }

        <!-- Main columns -->
        <section class="dashboard-columns">
          <div class="dashboard-main">

            <!-- Learning goals -->
            <app-dashboard-card title="Your learning goals">
              @if (data.activeGoals.length) {
                <div class="goal-list">
                  @for (goal of data.activeGoals; track goal.id) {
                    <a class="goal-row" [routerLink]="['/goals', goal.id]">
                      <span class="goal-icon" aria-hidden="true">{{ goal.subject.slice(0, 1) }}</span>
                      <span class="goal-copy">
                        <strong>{{ goal.title }}</strong>
                        <small>{{ goal.subject }} · {{ goal.topic }} · {{ goal.proficiencyLevel | lowercase }}</small>
                        @if (goal.targetDate) {
                          <small style="color:var(--orange)">Target: {{ goal.targetDate | date:'mediumDate' }}</small>
                        }
                      </span>
                      <span class="goal-arrow" aria-hidden="true">↗</span>
                    </a>
                  }
                </div>
              } @else {
                <div class="empty-state">
                  <span class="empty-icon" aria-hidden="true">✧</span>
                  <h3>Your next goal starts here</h3>
                  <p>Choose a topic you're curious about and make a plan.</p>
                  <a class="text-link" routerLink="/goals/new">Create your first goal →</a>
                </div>
              }
            </app-dashboard-card>

            <!-- Recent activity -->
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

            <!-- Today's focus -->
            <app-dashboard-card title="Today's focus">
              @if (data.tasksDueToday.length) {
                <div class="task-list">
                  @for (task of data.tasksDueToday; track task.id) {
                    <app-task-item [task]="task" (completionChange)="toggleTask($event)" />
                  }
                </div>
              } @else {
                <div class="empty-state compact">
                  <h3>Room to breathe</h3>
                  <p>No tasks due today. Enjoy the space or get ahead.</p>
                </div>
              }
            </app-dashboard-card>

            <!-- Coming up -->
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
  styles: [`
    .ai-coach-panel {
      margin-bottom: 1.25rem;
      border: 1px solid #e8d9b8;
      border-radius: 16px;
      background: linear-gradient(135deg, #fffdf7 0%, #fdf6e8 100%);
      box-shadow: 0 8px 24px rgba(180, 130, 50, 0.07);
    }
    .ai-coach-empty {
      border-color: rgba(222, 230, 222, 0.9);
      background: linear-gradient(135deg, #fff 0%, #fbfdf9 100%);
      box-shadow: var(--shadow-card, 0 12px 32px rgba(29,51,39,0.045));
    }
    .ai-coach-inner { padding: 1.5rem 1.75rem; }
    .ai-coach-header {
      display: flex;
      align-items: flex-start;
      gap: 1rem;
      margin-bottom: 1rem;
    }
    .ai-coach-icon {
      display: grid;
      place-items: center;
      flex: 0 0 42px;
      height: 42px;
      border-radius: 14px;
      background: #fbf0df;
      color: #a87542;
      font-size: 1.25rem;
    }
    .ai-coach-header > div { flex: 1; min-width: 0; }
    .ai-coach-refresh { margin-left: auto; white-space: nowrap; }
    .ai-coach-body {
      color: var(--ink, #20302a);
      font-size: 0.97rem;
      line-height: 1.7;
      margin-bottom: 0.6rem;
    }
    .ai-coach-reason {
      color: var(--muted, #6b7770);
      font-size: 0.85rem;
      line-height: 1.6;
      margin-bottom: 0.9rem;
    }
    .ai-coach-actions {
      margin: 0 0 1rem;
      padding-left: 1.2rem;
      color: var(--ink, #20302a);
      font-size: 0.88rem;
      line-height: 1.8;
    }
    .ai-coach-footer {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 0.5rem;
      padding-top: 0.9rem;
      border-top: 1px solid #f0e4c8;
    }
    @media (max-width: 680px) {
      .ai-coach-inner { padding: 1.1rem; }
      .ai-coach-header { flex-wrap: wrap; }
      .ai-coach-refresh { margin-left: 0; }
    }
  `],
})
export class DashboardPage implements OnInit {
  private readonly learning = inject(LearningService);
  dashboard: Dashboard | null = null;
  loading = true;
  error = '';
  recError = '';
  generatingRec = false;
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

  refreshRecommendation(): void {
    if (this.generatingRec) return;
    this.generatingRec = true;
    this.recError = '';
    this.learning.generateRecommendation().subscribe({
      next: (rec: AiRecommendation) => {
        if (this.dashboard) {
          this.dashboard = { ...this.dashboard, aiRecommendation: rec };
        }
      },
      error: (error: HttpErrorResponse) => {
        this.recError = error.status === 204
          ? 'No recommendation available yet. Add a learning goal to get started.'
          : (error.error?.detail ?? 'The recommendation could not be generated. Please try again.');
        this.generatingRec = false;
      },
      complete: () => { this.generatingRec = false; },
    });
  }
}
