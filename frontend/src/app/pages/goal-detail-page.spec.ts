import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { LearningLesson } from '../core/models';
import { LearningService } from '../core/learning.service';
import { GoalDetailPage } from './goal-detail-page';

describe('GoalDetailPage lesson generation', () => {
  const lesson: LearningLesson = {
    id: 'lesson-1',
    learningGoalId: 'goal-1',
    studentId: 'student-1',
    title: 'Fractions made clear',
    topic: 'Fractions',
    difficulty: 'BEGINNER',
    content: {
      learningObjectives: ['Compare fractions'],
      sections: [{ heading: 'Equivalent fractions', explanation: 'Multiply both parts by the same value.' }],
      examples: [],
      keyTakeaways: ['Equivalent fractions have equal values.'],
    },
    estimatedStudyMinutes: 20,
    createdAt: '2026-10-23T10:00:00Z',
  };

  let generateResponse: Subject<LearningLesson>;
  let learning: {
    getGoal: ReturnType<typeof vi.fn>;
    tasks: ReturnType<typeof vi.fn>;
    lessons: ReturnType<typeof vi.fn>;
    generateLesson: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    generateResponse = new Subject<LearningLesson>();
    learning = {
      getGoal: vi.fn().mockReturnValue(of({
        id: 'goal-1', studentId: 'student-1', title: 'Learn fractions', subject: 'Math',
        topic: 'Fractions', proficiencyLevel: 'BEGINNER', targetDate: null,
        status: 'ACTIVE', createdAt: '2026-10-23T10:00:00Z',
      })),
      tasks: vi.fn().mockReturnValue(of([])),
      lessons: vi.fn().mockReturnValue(of([])),
      generateLesson: vi.fn().mockReturnValue(generateResponse.asObservable()),
    };

    await TestBed.configureTestingModule({
      imports: [GoalDetailPage],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'goal-1' } } } },
        { provide: LearningService, useValue: learning },
      ],
    }).compileComponents();
  });

  it('shows loading, prevents duplicate clicks, and opens the generated lesson', () => {
    const fixture = TestBed.createComponent(GoalDetailPage);
    fixture.detectChanges();
    const page = fixture.componentInstance;
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    page.generateLesson();
    page.generateLesson();

    expect(page.generatingLesson).toBe(true);
    expect(learning.generateLesson).toHaveBeenCalledTimes(1);

    generateResponse.next(lesson);
    generateResponse.complete();
    fixture.detectChanges(false);

    expect(page.generatingLesson).toBe(false);
    expect(page.lessons[0].title).toBe('Fractions made clear');
    expect(navigate).toHaveBeenCalledWith(['/lessons', 'lesson-1']);
  });

  it('displays provider errors and clears the loading state', () => {
    learning.generateLesson.mockReturnValue(throwError(() =>
      new HttpErrorResponse({ status: 503, error: { detail: 'Gemini is unavailable.' } })));
    const fixture = TestBed.createComponent(GoalDetailPage);
    fixture.detectChanges();

    fixture.componentInstance.generateLesson();

    expect(fixture.componentInstance.generatingLesson).toBe(false);
    expect(fixture.componentInstance.lessonError).toBe('Gemini is unavailable.');
  });
});
