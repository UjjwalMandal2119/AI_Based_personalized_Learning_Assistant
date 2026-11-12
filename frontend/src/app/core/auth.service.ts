import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthResponse } from './models';

const SESSION_KEY = 'learning-assistant-session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly userSignal = signal<AuthResponse | null>(this.readSession());
  readonly currentUser = this.userSignal.asReadonly();

  constructor(private readonly http: HttpClient) {}

  register(displayName: string, email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/register`, {
      displayName,
      email,
      password,
    }).pipe(tap((response) => this.saveSession(response)));
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/login`, {
      email,
      password,
    }).pipe(tap((response) => this.saveSession(response)));
  }

  logout(): void {
    localStorage.removeItem(SESSION_KEY);
    this.userSignal.set(null);
  }

  token(): string | null {
    return this.userSignal()?.token ?? null;
  }

  private saveSession(session: AuthResponse): void {
    localStorage.setItem(SESSION_KEY, JSON.stringify(session));
    this.userSignal.set(session);
  }

  private readSession(): AuthResponse | null {
    const stored = localStorage.getItem(SESSION_KEY);
    if (!stored) return null;

    try {
      const session = JSON.parse(stored) as AuthResponse;
      return session.token && session.email ? session : null;
    } catch {
      localStorage.removeItem(SESSION_KEY);
      return null;
    }
  }
}
