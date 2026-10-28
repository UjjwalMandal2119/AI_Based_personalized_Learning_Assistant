import { Component, OnInit, inject } from '@angular/core';
import { DatePipe, LowerCasePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { LearningLesson } from '../core/models';
import { LearningService } from '../core/learning.service';
import { ErrorMessageComponent } from '../shared/error-message';
import { LoadingStateComponent } from '../shared/loading-state';

@Component({
  standalone: true,
  imports: [DatePipe, LowerCasePipe, RouterLink, ErrorMessageComponent, LoadingStateComponent],
  template: `
    <main class="page-shell narrow-shell lesson-page" id="main-content">
      <a class="back-link" [routerLink]="goalLink">← Back to {{ lesson?.topic ?? 'your learning goal' }}</a>
      @if (loading) {
        <app-loading-state message="Opening your saved lesson…" />
      } @else if (error) {
        <app-error-message [message]="error" />
      } @else if (lesson; as currentLesson) {
        <article class="lesson-document">
          <header class="lesson-header">
            <p class="eyebrow">Study material · {{ currentLesson.difficulty | lowercase }}</p>
            <h1>{{ currentLesson.title }}</h1>
            <p class="muted">{{ currentLesson.topic }} · {{ currentLesson.estimatedStudyMinutes }} min read · Generated {{ currentLesson.createdAt | date:'mediumDate' }}</p>
          </header>

          <section class="lesson-objectives">
            <h2>What you'll learn</h2>
            <ul>
              @for (objective of currentLesson.content.learningObjectives; track $index) {
                <li>{{ objective }}</li>
              }
            </ul>
          </section>

          @for (section of currentLesson.content.sections; track $index) {
            <section class="lesson-section">
              <h2>{{ section.heading }}</h2>
              <p class="lesson-prose">{{ section.explanation }}</p>
            </section>
          }

          @if (currentLesson.content.examples.length) {
            <section class="lesson-examples">
              <h2>Worked examples</h2>
              @for (example of currentLesson.content.examples; track $index) {
                <article class="example-card">
                  <h3>{{ example.title }}</h3>
                  <p class="example-label">Try this</p>
                  <p class="lesson-prose">{{ example.scenario }}</p>
                  <p class="example-label">Worked solution</p>
                  <pre class="lesson-prose example-solution">{{ example.solution }}</pre>
                </article>
              }
            </section>
          }

          <section class="lesson-takeaways">
            <h2>Key takeaways</h2>
            <ul>
              @for (takeaway of currentLesson.content.keyTakeaways; track $index) {
                <li>{{ takeaway }}</li>
              }
            </ul>
          </section>
        </article>
      }
    </main>
  `,
})
export class LessonPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly learning = inject(LearningService);
  lesson: LearningLesson | null = null;
  loading = true;
  error = '';
  goalLink: string[] = ['/dashboard'];

  ngOnInit(): void {
    const lessonId = this.route.snapshot.paramMap.get('lessonId') ?? '';
    this.learning.lesson(lessonId).subscribe({
      next: (lesson) => {
        this.lesson = lesson;
        this.goalLink = ['/goals', lesson.learningGoalId];
      },
      error: (error: HttpErrorResponse) => {
        this.error = error.error?.detail ?? 'This lesson could not be loaded.';
        this.loading = false;
      },
      complete: () => { this.loading = false; },
    });
  }
}
