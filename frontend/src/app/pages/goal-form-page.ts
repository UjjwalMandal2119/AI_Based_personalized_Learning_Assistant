import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { LearningService } from '../core/learning.service';
import { ProficiencyLevel } from '../core/models';
import { ErrorMessageComponent } from '../shared/error-message';

@Component({
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, ErrorMessageComponent],
  template: `
    <main class="page-shell narrow-shell">
      <a class="back-link" routerLink="/dashboard">← Back to your dashboard</a>
      <section class="form-card">
        <p class="eyebrow">A plan with purpose</p>
        <h1>Create a learning goal</h1>
        <p class="muted">Start with one topic you’d like to understand a little better.</p>
        @if (error) { <app-error-message [message]="error" /> }
        <form [formGroup]="form" (ngSubmit)="submit()">
          <label class="field-label" for="title">Goal title</label>
          <input id="title" formControlName="title" placeholder="Understand the fundamentals of algebra">
          @if (form.controls.title.touched && form.controls.title.invalid) { <span class="field-error">Enter a goal title up to 120 characters.</span> }
          <div class="form-row">
            <div>
              <label class="field-label" for="subject">Subject</label>
              <input id="subject" formControlName="subject" placeholder="Mathematics">
            </div>
            <div>
              <label class="field-label" for="topic">Topic</label>
              <input id="topic" formControlName="topic" placeholder="Linear equations">
            </div>
          </div>
          <div class="form-row">
            <div>
              <label class="field-label" for="proficiency">Current level</label>
              <select id="proficiency" formControlName="proficiencyLevel">
                <option value="BEGINNER">Beginner</option>
                <option value="INTERMEDIATE">Intermediate</option>
                <option value="ADVANCED">Advanced</option>
              </select>
            </div>
            <div>
              <label class="field-label" for="targetDate">Target date <span class="optional">(optional)</span></label>
              <input id="targetDate" type="date" formControlName="targetDate">
            </div>
          </div>
          <div class="form-actions">
            <a class="button button-secondary" routerLink="/dashboard">Cancel</a>
            <button class="button button-primary" type="submit" [disabled]="form.invalid || saving">
              {{ saving ? 'Saving…' : 'Create goal' }}
            </button>
          </div>
        </form>
      </section>
    </main>
  `,
})
export class GoalFormPage {
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly learning = inject(LearningService);
  private readonly router = inject(Router);
  readonly form = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(120)]],
    subject: ['', [Validators.required, Validators.maxLength(80)]],
    topic: ['', [Validators.required, Validators.maxLength(120)]],
    proficiencyLevel: ['BEGINNER' as ProficiencyLevel, Validators.required],
    targetDate: [''],
  });
  saving = false;
  error = '';

  submit(): void {
    if (this.form.invalid || this.saving) return;
    this.saving = true;
    this.error = '';
    const value = this.form.getRawValue();
    this.learning.createGoal({ ...value, targetDate: value.targetDate || null }).subscribe({
      next: (goal) => void this.router.navigate(['/goals', goal.id]),
      error: (error: HttpErrorResponse) => {
        this.error = error.error?.detail ?? 'Your goal could not be saved.';
        this.saving = false;
      },
      complete: () => { this.saving = false; },
    });
  }
}
