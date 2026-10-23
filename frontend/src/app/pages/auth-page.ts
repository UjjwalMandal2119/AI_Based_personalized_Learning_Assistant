import { Component, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../core/auth.service';
import { ErrorMessageComponent } from '../shared/error-message';

@Component({
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, ErrorMessageComponent],
  template: `
    <main class="auth-layout">
      <section class="auth-intro">
        <a class="brand" routerLink="/login"><span class="brand-mark">L</span> Lumen</a>
        <div>
          <p class="eyebrow">A little progress, every day</p>
          <h1>Your learning journey, made personal.</h1>
          <p class="intro-copy">Set a goal, build a steady study habit, and see your progress come to life.</p>
        </div>
        <p class="intro-foot">Your next breakthrough starts with one small step.</p>
      </section>
      <section class="auth-panel">
        <div class="auth-form-wrap">
          <p class="eyebrow">{{ isRegister ? 'Create your account' : 'Welcome back' }}</p>
          <h2>{{ isRegister ? 'Start learning with intention.' : 'Pick up where you left off.' }}</h2>
          <p class="muted">{{ isRegister ? 'Your personal study space is a few details away.' : 'Sign in to see what’s on your learning plan.' }}</p>
          @if (error) { <app-error-message [message]="error" /> }
          <form [formGroup]="form" (ngSubmit)="submit()">
            @if (isRegister) {
              <label class="field-label" for="displayName">Your name</label>
              <input id="displayName" formControlName="displayName" autocomplete="name" placeholder="Alex Morgan">
              @if (form.controls.displayName.touched && form.controls.displayName.invalid) {
                <span class="field-error">Enter your name (up to 80 characters).</span>
              }
            }
            <label class="field-label" for="email">Email address</label>
            <input id="email" type="email" formControlName="email" autocomplete="email" placeholder="you@example.com">
            @if (form.controls.email.touched && form.controls.email.invalid) {
              <span class="field-error">Enter a valid email address.</span>
            }
            <label class="field-label" for="password">Password</label>
            <input id="password" type="password" formControlName="password"
                   [autocomplete]="isRegister ? 'new-password' : 'current-password'"
                   [placeholder]="isRegister ? 'At least 8 characters' : 'Your password'">
            @if (form.controls.password.touched && form.controls.password.invalid) {
              <span class="field-error">{{ isRegister ? 'Password must be at least 8 characters.' : 'Enter your password.' }}</span>
            }
            <button class="button button-primary full-width" type="submit" [disabled]="form.invalid || loading">
              {{ loading ? 'Please wait…' : (isRegister ? 'Create account' : 'Sign in') }}
            </button>
          </form>
          <p class="auth-switch">
            {{ isRegister ? 'Already have an account?' : 'New to Lumen?' }}
            <a [routerLink]="isRegister ? '/login' : '/register'">
              {{ isRegister ? 'Sign in' : 'Create an account' }}
            </a>
          </p>
        </div>
      </section>
    </main>
  `,
})
export class AuthPage {
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly isRegister = inject(ActivatedRoute).snapshot.data['mode'] === 'register';
  readonly form = this.fb.group({
    displayName: ['', this.isRegister ? [Validators.required, Validators.maxLength(80)] : []],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    password: ['', [Validators.required, ...(this.isRegister ? [Validators.minLength(8), Validators.maxLength(72)] : [])]],
  });
  loading = false;
  error = '';

  submit(): void {
    if (this.form.invalid || this.loading) return;
    this.loading = true;
    this.error = '';
    const { displayName, email, password } = this.form.getRawValue();
    const request = this.isRegister
      ? this.auth.register(displayName, email, password)
      : this.auth.login(email, password);
    request.subscribe({
      next: () => void this.router.navigateByUrl('/dashboard'),
      error: (error: HttpErrorResponse) => {
        this.error = error.error?.detail ?? 'We could not sign you in. Check your details and try again.';
        this.loading = false;
      },
      complete: () => { this.loading = false; },
    });
  }
}
