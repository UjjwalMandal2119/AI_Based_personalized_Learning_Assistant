import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let auth: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [AuthService, provideHttpClient(), provideHttpClientTesting()],
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('stores the authenticated student and clears the session on logout', () => {
    auth.login('ada@example.com', 'password-123').subscribe();
    const request = http.expectOne('http://localhost:8080/api/auth/login');
    expect(request.request.method).toBe('POST');
    request.flush({
      token: 'signed-token',
      tokenType: 'Bearer',
      displayName: 'Ada',
      email: 'ada@example.com',
    });

    expect(auth.currentUser()?.email).toBe('ada@example.com');
    expect(auth.token()).toBe('signed-token');
    expect(JSON.parse(localStorage.getItem('learning-assistant-session') ?? '{}').token).toBe('signed-token');

    auth.logout();
    expect(auth.currentUser()).toBeNull();
    expect(localStorage.getItem('learning-assistant-session')).toBeNull();
  });
});
