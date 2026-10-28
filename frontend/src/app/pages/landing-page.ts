import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../core/auth.service';

@Component({
  standalone: true,
  imports: [RouterLink],
  template: `
    @if (!auth.currentUser()) {
      <header class="landing-header">
        <a class="brand" routerLink="/" aria-label="Disha AI home"><span class="brand-mark">D</span> Disha AI</a>
        <nav aria-label="Main navigation">
          <a class="landing-nav-link" href="#how-it-works">How it works</a>
          <a class="landing-login" routerLink="/login">Sign in</a>
          <a class="button button-primary landing-nav-cta" routerLink="/register">Get started</a>
        </nav>
      </header>
    }

    <main class="landing-page">
      <section class="landing-hero" aria-labelledby="landing-title">
        <div class="landing-copy">
          <p class="eyebrow"><span class="landing-sparkle" aria-hidden="true">✦</span> YOUR PERSONAL AI LEARNING COACH</p>
          <h1 id="landing-title">Turn your learning goals into a <span>plan you can follow.</span></h1>
          <p class="landing-description">
            Choose what you want to learn. Get a structured daily plan, personalized lessons,
            and targeted practice—with recommendations that show what to study next and how to improve.
          </p>
          <div class="landing-actions">
            <a class="button button-primary landing-primary-cta"
               [routerLink]="auth.currentUser() ? '/dashboard' : '/register'">
              {{ auth.currentUser() ? 'Go to my learning' : 'Start learning' }}
              <span aria-hidden="true">→</span>
            </a>
            <a class="landing-secondary-cta" href="#how-it-works">See how it works <span aria-hidden="true">↓</span></a>
          </div>
          <p class="landing-note"><span aria-hidden="true">✓</span> A clear next step for every study session.</p>
        </div>

        <div class="landing-preview-wrap">
          <article class="study-preview" aria-labelledby="preview-title">
            <div class="preview-label"><span aria-hidden="true">✳</span> ILLUSTRATIVE PREVIEW</div>
            <div class="preview-heading">
              <div>
                <p class="preview-kicker">YOUR LEARNING PLAN</p>
                <h2 id="preview-title">Learn Java Collections</h2>
              </div>
              <span class="preview-subject-icon" aria-hidden="true">J</span>
            </div>

            <div class="preview-progress">
              <div class="preview-progress-copy"><span>Today’s progress</span><strong>2 of 4 tasks</strong></div>
              <div class="preview-progress-track" role="progressbar" aria-label="Illustrative daily plan progress"
                   aria-valuemin="0" aria-valuemax="4" aria-valuenow="2"><span></span></div>
            </div>

            <h3 class="preview-plan-title">TODAY’S PLAN</h3>
            <ul class="preview-task-list">
              <li class="preview-task is-done"><span class="preview-check" aria-hidden="true">✓</span><span>Review ArrayList</span><span class="preview-task-meta">Review</span></li>
              <li class="preview-task is-done"><span class="preview-check" aria-hidden="true">✓</span><span>Learn HashMap</span><span class="preview-task-meta">Lesson</span></li>
              <li class="preview-task"><span class="preview-check" aria-hidden="true"></span><span>Complete a short quiz</span><span class="preview-task-meta">Practice</span></li>
              <li class="preview-task"><span class="preview-check" aria-hidden="true"></span><span>Revise incorrect answers</span><span class="preview-task-meta">Review</span></li>
            </ul>

            <div class="preview-recommendation">
              <span class="recommendation-icon" aria-hidden="true">↗</span>
              <div><strong>Your next step</strong><p>After the quiz, revisit any HashMap questions you found tricky.</p></div>
            </div>
          </article>
          <p class="preview-disclaimer">Example plan for illustration only.</p>
        </div>
      </section>

      <section class="how-section landing-section" id="how-it-works" aria-labelledby="how-title">
        <div class="section-intro">
          <p class="eyebrow">A PLAN THAT MOVES WITH YOU</p>
          <h2 id="how-title">From “I want to learn” to “I know what’s next.”</h2>
          <p>Turn a big goal into a learning routine that feels clear and manageable.</p>
        </div>
        <ol class="steps-list">
          <li class="step-card">
            <span class="step-icon" aria-hidden="true">◎</span>
            <span class="step-number">STEP 01</span>
            <h3>Set your goal</h3>
            <p>Choose a topic, your current level, and what you want to achieve.</p>
          </li>
          <li class="step-card">
            <span class="step-icon" aria-hidden="true">☷</span>
            <span class="step-number">STEP 02</span>
            <h3>Follow your learning plan</h3>
            <p>Get AI-generated explanations, study material, and manageable daily tasks.</p>
          </li>
          <li class="step-card">
            <span class="step-icon" aria-hidden="true">↗</span>
            <span class="step-number">STEP 03</span>
            <h3>Practice and improve</h3>
            <p>Complete assignments, understand mistakes, and find a useful next step.</p>
          </li>
        </ol>
      </section>

      <section class="features-section landing-section" aria-labelledby="features-title">
        <div class="section-intro">
          <p class="eyebrow">BUILT AROUND YOUR PROGRESS</p>
          <h2 id="features-title">Everything you need to make progress</h2>
          <p>One personal learning space to help you focus, practice, and keep moving forward.</p>
        </div>
        <div class="feature-grid">
          <article class="feature-card">
            <span class="feature-icon" aria-hidden="true">✎</span>
            <h3>Personalized study material</h3>
            <p>Explore topics at your level with clear explanations and examples made for your goal.</p>
          </article>
          <article class="feature-card">
            <span class="feature-icon" aria-hidden="true">☷</span>
            <h3>Daily learning plan</h3>
            <p>Know what to study today and which manageable task to take on next.</p>
          </article>
          <article class="feature-card">
            <span class="feature-icon" aria-hidden="true">✓</span>
            <h3>Practice and assignments</h3>
            <p>Check your understanding with questions and exercises as you learn.</p>
          </article>
          <article class="feature-card">
            <span class="feature-icon" aria-hidden="true">⌕</span>
            <h3>Weak-area insights</h3>
            <p>Use your assessment results to spot concepts that could use another look.</p>
          </article>
          <article class="feature-card">
            <span class="feature-icon" aria-hidden="true">↗</span>
            <h3>Adaptive recommendations</h3>
            <p>Let your learning results guide what to review or practice in your next session.</p>
          </article>
        </div>
      </section>

      <section class="landing-cta" aria-labelledby="cta-title">
        <div>
          <p class="eyebrow">YOUR NEXT STEP STARTS HERE</p>
          <h2 id="cta-title">Your next learning goal starts here.</h2>
          <p>Build your study plan, learn at your own pace, and keep improving.</p>
        </div>
        <a class="button button-primary landing-primary-cta"
           [routerLink]="auth.currentUser() ? '/dashboard' : '/register'">
          {{ auth.currentUser() ? 'Go to my learning' : 'Create your learning plan' }}
          <span aria-hidden="true">→</span>
        </a>
      </section>

      <footer class="landing-footer">
        <a class="brand" routerLink="/" aria-label="Disha AI home"><span class="brand-mark">D</span> Disha AI</a>
        <span>Learn smarter. Stay on track. Achieve your learning goals.</span>
      </footer>
    </main>
  `,
})
export class LandingPage {
  readonly auth = inject(AuthService);
}
