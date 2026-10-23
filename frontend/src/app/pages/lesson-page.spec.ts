import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { LearningLesson } from '../core/models';
import { LearningService } from '../core/learning.service';
import { LessonPage } from './lesson-page';

describe('LessonPage', () => {
  it('renders persisted structured lesson content as inert text', async () => {
    const lesson: LearningLesson = {
      id: 'lesson-1',
      learningGoalId: 'goal-1',
      studentId: 'student-1',
      title: 'Fractions made clear',
      topic: 'Fractions',
      difficulty: 'BEGINNER',
      content: {
        learningObjectives: ['Compare fractions'],
        sections: [{ heading: 'Equivalent fractions', explanation: '<script>alert(1)</script> Multiply both parts equally.' }],
        examples: [{
          title: 'A simple comparison',
          scenario: 'Compare 1/2 and 2/4.',
          solution: 'They have the same value.',
        }],
        keyTakeaways: ['Equivalent fractions have equal values.'],
      },
      estimatedStudyMinutes: 20,
      createdAt: '2026-10-23T10:00:00Z',
    };

    await TestBed.configureTestingModule({
      imports: [LessonPage],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'lesson-1' } } } },
        { provide: LearningService, useValue: { lesson: () => of(lesson) } },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(LessonPage);
    fixture.detectChanges();

    const page = fixture.nativeElement as HTMLElement;
    expect(page.querySelector('h1')?.textContent).toContain('Fractions made clear');
    expect(page.textContent).toContain('Compare fractions');
    expect(page.textContent).toContain('A simple comparison');
    expect(page.textContent).toContain('Equivalent fractions have equal values.');
    expect(page.querySelector('script')).toBeNull();
  });
});
