import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../core/auth.service';

@Component({
  standalone: true,
  imports: [RouterLink],
  template: `
    @if (!auth.currentUser()) {
      <header class="landing-header">
        <a class="brand" routerLink="/"><span class="brand-mark">D</span> Disha AI</a>
        <nav aria-label="Main navigation">
          <a class="landing-nav-link" href="#how-it-works">How it works</a>
          <a class="landing-login" routerLink="/login">Sign in</a>
          <a class="button button-primary landing-nav-cta" routerLink="/register">Get started</a>
        </nav>
      </header>
    }

    <main class="landing-page">
      <section class="landing-hero">
        <div class="landing-copy">
          <p class="eyebrow"><span class="landing-sparkle" aria-hidden="true">✦</span> YOUR CURIOSITY, WITH A PLAN</p>
          <h1>Make learning feel like <span>your own.</span></h1>
          <p class="landing-description">
            Turn the things you want to learn into a clear, personal study plan.
            Take one small step at a time and see how far you go.
          </p>
          <div class="landing-actions">
            <a class="button button-primary landing-primary-cta"
               [routerLink]="auth.currentUser() ? '/dashboard' : '/register'">
              {{ auth.currentUser() ? 'Go to my learning' : 'Create my learning plan' }}
              <span aria-hidden="true">→</span>
            </a>
            @if (!auth.currentUser()) {
              <a class="landing-secondary-cta" routerLink="/login">I already have an account</a>
            }
          </div>
          <div class="landing-note">
            <span class="landing-note-icon" aria-hidden="true">✓</span>
            <span>Set your own pace. Celebrate every bit of progress.</span>
          </div>
        </div>

        <div class="landing-preview" aria-label="Preview of a personalized learning plan">
          <div class="preview-orbit preview-orbit-one"></div>
          <div class="preview-orbit preview-orbit-two"></div>
          <article class="preview-card">
            <div class="preview-topline">
              <div>
                <p class="preview-kicker">YOUR STUDY SPACE</p>
                <h2>A little progress, daily.</h2>
              </div>
              <span class="preview-avatar" aria-hidden="true">✦</span>
            </div>
            <div class="preview-goal">
              <span class="preview-goal-icon" aria-hidden="true">∑</span>
              <span><small>YOUR LEARNING GOAL</small><strong>Get comfortable with algebra</strong></span>
              <span class="preview-arrow" aria-hidden="true">↗</span>
            </div>
            <div class="preview-focus">
              <div class="preview-focus-heading">
                <span>Today’s focus</span>
                <span class="preview-time">15 min</span>
              </div>
              <div class="preview-task">
                <span class="preview-checkbox" aria-hidden="true"></span>
                <span><strong>Practice linear equations</strong><small>One concept · At your pace</small></span>
              </div>
              <div class="preview-progress">
                <div><span>YOUR MOMENTUM</span><strong>Just getting started</strong></div>
                <span class="preview-progress-mark" aria-hidden="true">↗</span>
              </div>
            </div>
            <div class="preview-footer"><span>✧</span> Small steps add up.</div>
          </article>
          <span class="preview-decoration preview-decoration-top" aria-hidden="true">✳</span>
          <span class="preview-decoration preview-decoration-bottom" aria-hidden="true">✦</span>
        </div>
      </section>

      <section class="landing-benefits" id="how-it-works" aria-label="How Disha AI helps you learn">
        <div class="landing-benefit">
          <span class="benefit-number">01</span>
          <div><h2>Start with what matters to you</h2><p>Choose a subject, set a goal, and make learning personal from day one.</p></div>
        </div>
        <div class="landing-benefit">
          <span class="benefit-number">02</span>
          <div><h2>Know your next small step</h2><p>Break a big ambition into manageable tasks you can fit into your day.</p></div>
        </div>
        <div class="landing-benefit">
          <span class="benefit-number">03</span>
          <div><h2>Keep your progress in sight</h2><p>See what you’ve completed, what’s next, and the momentum you’re building.</p></div>
        </div>
      </section>
      <footer class="landing-footer"><a class="brand" routerLink="/"><span class="brand-mark">D</span> Disha AI</a><span>Learning, one small step at a time.</span></footer>
    </main>
  `,
})
export class LandingPage {
  readonly auth = inject(AuthService);
}
