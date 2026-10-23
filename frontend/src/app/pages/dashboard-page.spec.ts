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

  it('loads personalized dashboard data and calculates task progress', () => {
    const fixture = TestBed.createComponent(DashboardPage);
    const page = fixture.componentInstance;
    fixture.detectChanges();
    http.expectOne('http://localhost:8080/api/dashboard').flush({
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
    });

    expect(page.loading).toBe(false);
    expect(page.dashboard?.displayName).toBe('Ada');
    expect(page.dashboard?.activeGoals[0].title).toBe('Learn algebra');
    expect(page.progress).toBe(0.5);
  });
});
