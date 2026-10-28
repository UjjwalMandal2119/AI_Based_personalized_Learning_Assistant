import { Component, OnInit, inject } from '@angular/core';
import { DatePipe, LowerCasePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { LearningGoal, LearningLesson, StudyTask, TaskType } from '../core/models';
import { LearningService } from '../core/learning.service';
import { ErrorMessageComponent } from '../shared/error-message';
import { LoadingStateComponent } from '../shared/loading-state';
import { TaskItemComponent } from '../shared/task-item';

@Component({
  standalone: true,
  imports: [DatePipe, LowerCasePipe, ReactiveFormsModule, RouterLink, ErrorMessageComponent,
    LoadingStateComponent, TaskItemComponent],
  template: `
    <main class="page-shell narrow-shell" id="main-content">
      <a class="back-link" routerLink="/dashboard">← Back to your dashboard</a>
      @if (loading) {
        <app-loading-state message="Opening your learning goal…" />
      } @else if (error && !goal) {
        <app-error-message [message]="error" />
      } @else if (goal; as currentGoal) {
        <section class="goal-detail-header">
          <div>
            <p class="eyebrow">{{ currentGoal.subject }} · {{ currentGoal.topic }}</p>
            <h1>{{ currentGoal.title }}</h1>
            <p class="muted">Starting at {{ currentGoal.proficiencyLevel | lowercase }}
              @if (currentGoal.targetDate) { · Target {{ currentGoal.targetDate | date:'mediumDate' }} }
            </p>
          </div>
          <div class="button-row">
            <button class="button button-secondary" type="button" (click)="editing = !editing">
              {{ editing ? 'Cancel edit' : 'Edit goal' }}
            </button>
            <button class="button button-danger" type="button" (click)="deleteGoal()">Delete</button>
          </div>
        </section>
        @if (error) { <app-error-message [message]="error" /> }

        <section class="form-card compact-card lesson-create">
          <p class="eyebrow">Personalized study material</p>
          <div class="lesson-action-row">
            <div>
              <h2>Learn about {{ currentGoal.topic }}</h2>
              <p class="muted">A lesson tailored to your {{ currentGoal.proficiencyLevel | lowercase }} level.</p>
            </div>
            <button class="button button-primary" type="button" [disabled]="generatingLesson"
                    (click)="generateLesson()" [attr.aria-busy]="generatingLesson">
              {{ generatingLesson ? 'Generating lesson…' : '✦ Generate study material' }}
            </button>
          </div>
          @if (generatingLesson) {
            <p class="lesson-loading" role="status">Creating a lesson for your learning goal. This may take a few moments…</p>
          }
          @if (lessonError) { <app-error-message [message]="lessonError" /> }
          @if (loadingLessons) { <p class="muted">Loading saved lessons…</p> }
          @if (lessons.length) {
            <div class="saved-lessons">
              <h3>Saved lessons</h3>
              @for (lesson of lessons; track lesson.id) {
                <a class="saved-lesson-row" [routerLink]="['/lessons', lesson.id]">
                  <span><strong>{{ lesson.title }}</strong><small>{{ lesson.estimatedStudyMinutes }} min · {{ lesson.createdAt | date:'mediumDate' }}</small></span>
                  <span aria-hidden="true">↗</span>
                </a>
              }
            </div>
          }
        </section>

        @if (editing) {
          <section class="form-card compact-card">
            <h2>Edit goal details</h2>
            <form [formGroup]="goalForm" (ngSubmit)="saveGoal()">
              <label class="field-label" for="goalTitle">Title</label>
              <input id="goalTitle" formControlName="title">
              <div class="form-row">
                <div><label class="field-label" for="goalSubject">Subject</label><input id="goalSubject" formControlName="subject"></div>
                <div><label class="field-label" for="goalTopic">Topic</label><input id="goalTopic" formControlName="topic"></div>
              </div>
              <div class="form-row">
                <div><label class="field-label" for="goalLevel">Current level</label>
                  <select id="goalLevel" formControlName="proficiencyLevel">
                    <option value="BEGINNER">Beginner</option><option value="INTERMEDIATE">Intermediate</option><option value="ADVANCED">Advanced</option>
                  </select>
                </div>
                <div><label class="field-label" for="goalStatus">Status</label>
                  <select id="goalStatus" formControlName="status">
                    <option value="ACTIVE">Active</option><option value="PAUSED">Paused</option><option value="COMPLETED">Completed</option>
                  </select>
                </div>
              </div>
              <label class="field-label" for="goalDate">Target date</label><input id="goalDate" type="date" formControlName="targetDate">
              <button class="button button-primary" type="submit" [disabled]="goalForm.invalid || savingGoal">Save changes</button>
            </form>
          </section>
        }

        <section class="form-card compact-card task-create">
          <p class="eyebrow">Make your next step tangible</p>
          <h2>Add a study task</h2>
          <form [formGroup]="taskForm" (ngSubmit)="createTask()">
            <label class="field-label" for="taskTitle">Task</label>
            <input id="taskTitle" formControlName="title" placeholder="Review the first lesson">
            <label class="field-label" for="taskDescription">Notes <span class="optional">(optional)</span></label>
            <textarea id="taskDescription" formControlName="description" rows="2" placeholder="What do you want to focus on?"></textarea>
            <div class="form-row">
              <div><label class="field-label" for="taskType">Task type</label>
                <select id="taskType" formControlName="taskType">
                  <option value="STUDY">Study</option><option value="PRACTICE">Practice</option>
                  <option value="REVIEW">Review</option><option value="ASSIGNMENT">Assignment</option>
                </select>
              </div>
              <div><label class="field-label" for="minutes">Minutes</label>
                <input id="minutes" type="number" min="1" max="1440" formControlName="estimatedMinutes">
              </div>
              <div><label class="field-label" for="dueDate">Due date</label>
                <input id="dueDate" type="date" formControlName="dueDate">
              </div>
            </div>
            <button class="button button-primary" type="submit" [disabled]="taskForm.invalid || savingTask">
              {{ savingTask ? 'Adding…' : 'Add task' }}
            </button>
          </form>
        </section>

        <section class="task-section">
          <div class="section-heading"><div><p class="eyebrow">Your plan in motion</p><h2>Study tasks</h2></div>
            <span class="count-pill">{{ completedCount }}/{{ tasks.length }} done</span></div>
          @if (tasks.length) {
            <div class="task-list task-list-card">
              @for (task of tasks; track task.id) {
                <app-task-item [task]="task" (completionChange)="toggleTask($event)" />
              }
            </div>
          } @else {
            <div class="empty-state"><h3>One step at a time</h3><p>Add a task above to give this goal a first next step.</p></div>
          }
        </section>
      }
    </main>
  `,
})
export class GoalDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly learning = inject(LearningService);
  readonly goalForm = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(120)]],
    subject: ['', [Validators.required, Validators.maxLength(80)]],
    topic: ['', [Validators.required, Validators.maxLength(120)]],
    proficiencyLevel: ['BEGINNER'],
    targetDate: [''],
    status: ['ACTIVE'],
  });
  readonly taskForm = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(160)]],
    description: [''],
    taskType: ['STUDY' as TaskType, Validators.required],
    estimatedMinutes: [25, [Validators.required, Validators.min(1), Validators.max(1440)]],
    dueDate: [''],
  });
  goal: LearningGoal | null = null;
  tasks: StudyTask[] = [];
  loading = true;
  savingGoal = false;
  savingTask = false;
  generatingLesson = false;
  loadingLessons = true;
  lessons: LearningLesson[] = [];
  lessonError = '';
  editing = false;
  error = '';
  private goalId = '';

  get completedCount(): number {
    return this.tasks.filter((task) => task.completed).length;
  }

  ngOnInit(): void {
    this.goalId = this.route.snapshot.paramMap.get('goalId') ?? '';
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.learning.getGoal(this.goalId).subscribe({
      next: (goal) => {
        this.goal = goal;
        this.goalForm.reset({
          title: goal.title, subject: goal.subject, topic: goal.topic,
          proficiencyLevel: goal.proficiencyLevel, targetDate: goal.targetDate ?? '', status: goal.status,
        });
        this.loadTasks();
        this.loadLessons();
      },
      error: (error: HttpErrorResponse) => {
        this.error = error.error?.detail ?? 'This learning goal could not be found.';
        this.loading = false;
      },
    });
  }

  private loadTasks(): void {
    this.learning.tasks(this.goalId).subscribe({
      next: (tasks) => { this.tasks = tasks; },
      error: (error: HttpErrorResponse) => {
        this.error = error.error?.detail ?? 'Study tasks could not be loaded.';
      },
      complete: () => { this.loading = false; },
    });
  }

  private loadLessons(): void {
    this.loadingLessons = true;
    this.learning.lessons(this.goalId).subscribe({
      next: (lessons) => { this.lessons = lessons; },
      error: (error: HttpErrorResponse) => {
        this.lessonError = error.error?.detail ?? 'Saved lessons could not be loaded.';
        this.loadingLessons = false;
      },
      complete: () => { this.loadingLessons = false; },
    });
  }

  generateLesson(): void {
    if (this.generatingLesson) return;
    this.generatingLesson = true;
    this.lessonError = '';
    this.learning.generateLesson(this.goalId).subscribe({
      next: (lesson) => {
        this.lessons = [lesson, ...this.lessons];
        void this.router.navigate(['/lessons', lesson.id]);
      },
      error: (error: HttpErrorResponse) => {
        this.lessonError = error.error?.detail ?? 'The lesson could not be generated. Please try again.';
        this.generatingLesson = false;
      },
      complete: () => { this.generatingLesson = false; },
    });
  }

  createTask(): void {
    if (this.taskForm.invalid || this.savingTask) return;
    this.savingTask = true;
    const value = this.taskForm.getRawValue();
    this.learning.createTask(this.goalId, { ...value, dueDate: value.dueDate || null }).subscribe({
      next: () => {
        this.taskForm.reset({ title: '', description: '', taskType: 'STUDY', estimatedMinutes: 25, dueDate: '' });
        this.loadTasks();
      },
      error: (error: HttpErrorResponse) => {
        this.error = error.error?.detail ?? 'The task could not be added.';
        this.savingTask = false;
      },
      complete: () => { this.savingTask = false; },
    });
  }

  toggleTask(change: { id: string; completed: boolean }): void {
    this.learning.setTaskCompletion(change.id, change.completed).subscribe({
      next: () => this.loadTasks(),
      error: (error: HttpErrorResponse) => {
        this.error = error.error?.detail ?? 'The task could not be updated.';
      },
    });
  }

  saveGoal(): void {
    if (this.goalForm.invalid || this.savingGoal) return;
    this.savingGoal = true;
    const value = this.goalForm.getRawValue();
    this.learning.updateGoal(this.goalId, {
      ...value,
      proficiencyLevel: value.proficiencyLevel as LearningGoal['proficiencyLevel'],
      status: value.status as LearningGoal['status'],
      targetDate: value.targetDate || null,
    }).subscribe({
      next: (goal) => { this.goal = goal; this.editing = false; },
      error: (error: HttpErrorResponse) => {
        this.error = error.error?.detail ?? 'The goal could not be updated.';
        this.savingGoal = false;
      },
      complete: () => { this.savingGoal = false; },
    });
  }

  deleteGoal(): void {
    if (!window.confirm('Delete this goal and all of its study tasks? This cannot be undone.')) return;
    this.learning.deleteGoal(this.goalId).subscribe({
      next: () => void this.router.navigateByUrl('/dashboard'),
      error: (error: HttpErrorResponse) => {
        this.error = error.error?.detail ?? 'The goal could not be deleted.';
      },
    });
  }
}
