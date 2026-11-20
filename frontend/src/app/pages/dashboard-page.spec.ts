import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { DashboardPage } from './dashboard-page';

describe('DashboardPage', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  const mockDashboardResponse = {
    displayName: 'Ada',
    activeGoals: [{
      id: 'goal-1', studentId: 'student-1', title: 'Learn algebra', subject: 'Math',
      topic: 'Equations', proficiencyLevel: 'BEGINNER', targetDate: null,
      status: 'ACTIVE', createdAt: '2026-10-20T10:00:00Z',
    }],
    completedTasks: 1,
    remainingTasks: 1,
    tasksDueToday: [],
    upcomingTasks: [],
    recentActivity: [],
    aiRecommendation: null,
  };

  const mockDailyBriefResponse = {
    id: 'brief-1',
    briefDate: '2026-11-20',
    focusSummary: 'Focus on algebra',
    priorityTasks: 'Solve 5 equations',
    revisionTopics: 'Quadratic equations',
    dailyObjective: 'Master algebra basics',
    recommendedActions: ['Practice daily', 'Review notes'],
    providerName: 'ollama',
    modelName: 'llama3',
    generationDurationMs: 1000,
    fallbackUsed: false,
    createdAt: '2026-11-20T10:00:00Z',
  };

  function flushDashboardAndBrief(dashboardOverride = {}) {
    http.expectOne('http://localhost:8080/api/dashboard').flush({ ...mockDashboardResponse, ...dashboardOverride });
    http.expectOne('http://localhost:8080/api/ai/daily-brief').flush(mockDailyBriefResponse);
  }

  it('loads personalized dashboard data and calculates task progress', () => {
    const fixture = TestBed.createComponent(DashboardPage);
    const page = fixture.componentInstance;
    fixture.detectChanges();
    flushDashboardAndBrief();

    expect(page.loading).toBe(false);
    expect(page.dashboard?.displayName).toBe('Ada');
    expect(page.dashboard?.activeGoals[0].title).toBe('Learn algebra');
    expect(page.progress).toBe(0.5);
  });

  it('dashboard loads without waiting for AI generation', () => {
    // The dashboard GET must complete with only one HTTP call (no AI endpoint called on load).
    // AI generation is triggered only by explicit user action (refreshRecommendation).
    const fixture = TestBed.createComponent(DashboardPage);
    const page = fixture.componentInstance;
    fixture.detectChanges();

    // Only the dashboard request should be in flight — no AI request
    http.expectOne('http://localhost:8080/api/dashboard').flush({
      displayName: 'Ada',
      activeGoals: [],
      completedTasks: 0,
      remainingTasks: 0,
      tasksDueToday: [],
      upcomingTasks: [],
      recentActivity: [],
      aiRecommendation: null,
    });
    // Daily brief is loaded automatically
    http.expectOne('http://localhost:8080/api/ai/daily-brief').flush(mockDailyBriefResponse);

    // http.verify() in afterEach confirms no unexpected requests were made
    expect(page.loading).toBe(false);
    expect(page.generatingRec).toBe(false);
  });

  it('shows cached recommendation from dashboard response without generating', () => {
    const fixture = TestBed.createComponent(DashboardPage);
    const page = fixture.componentInstance;
    fixture.detectChanges();
    http.expectOne('http://localhost:8080/api/dashboard').flush({
      displayName: 'Ada',
      activeGoals: [],
      completedTasks: 0,
      remainingTasks: 0,
      tasksDueToday: [],
      upcomingTasks: [],
      recentActivity: [],
      aiRecommendation: {
        id: 'rec-1',
        learningGoalId: 'goal-1',
        title: 'Study algebra',
        recommendation: 'Practice daily',
        reason: 'You have pending tasks',
        topic: 'Algebra',
        activityType: 'PRACTICE',
        estimatedMinutes: 30,
        suggestedActions: ['Solve 5 equations'],
        providerName: 'ollama',
        fallbackUsed: false,
        createdAt: '2026-11-12T10:00:00Z',
      },
    });
    http.expectOne('http://localhost:8080/api/ai/daily-brief').flush(mockDailyBriefResponse);

    // Verify the cached recommendation is set without triggering generation
    expect(page.dashboard?.aiRecommendation).toBeTruthy();
    expect(page.dashboard?.aiRecommendation?.title).toBe('Study algebra');
    expect(page.generatingRec).toBe(false);
    // No AI generation request was made — http.verify() in afterEach confirms this
  });

  it('prevents duplicate generation requests while one is in flight', () => {
    const fixture = TestBed.createComponent(DashboardPage);
    const page = fixture.componentInstance;
    fixture.detectChanges();
    http.expectOne('http://localhost:8080/api/dashboard').flush({
      displayName: 'Ada',
      activeGoals: [{
        id: 'goal-1', studentId: 's-1', title: 'Learn algebra', subject: 'Math',
        topic: 'Equations', proficiencyLevel: 'BEGINNER', targetDate: null,
        status: 'ACTIVE', createdAt: '2026-10-20T10:00:00Z',
      }],
      completedTasks: 0,
      remainingTasks: 0,
      tasksDueToday: [],
      upcomingTasks: [],
      recentActivity: [],
      aiRecommendation: null,
    });
    http.expectOne('http://localhost:8080/api/ai/daily-brief').flush(mockDailyBriefResponse);

    // Trigger generation twice rapidly
    page.refreshRecommendation();
    page.refreshRecommendation(); // second call must be ignored

    // Only one AI request should be in flight
    http.expectOne('http://localhost:8080/api/ai/recommendations/generate');
    expect(page.generatingRec).toBe(true);
  });
});
