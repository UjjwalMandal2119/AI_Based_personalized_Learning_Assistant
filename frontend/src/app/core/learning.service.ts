import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import {
  AiRecommendation,
  CoachAction,
  CoachConversation,
  CoachConversationDetail,
  CoachModeInfo,
  CoachRequest,
  CoachResponse,
  DailyBrief,
  Dashboard,
  GoalRequest,
  LearningGoal,
  LearningLesson,
  StudyTask,
  TaskRequest
} from './models';

@Injectable({ providedIn: 'root' })
export class LearningService {
  private readonly apiUrl = environment.apiUrl;

  constructor(private readonly http: HttpClient) {}

  dashboard() {
    return this.http.get<Dashboard>(`${this.apiUrl}/dashboard`);
  }

  createGoal(request: GoalRequest) {
    return this.http.post<LearningGoal>(`${this.apiUrl}/learning-goals`, request);
  }

  getGoal(id: string) {
    return this.http.get<LearningGoal>(`${this.apiUrl}/learning-goals/${id}`);
  }

  updateGoal(id: string, request: GoalRequest) {
    return this.http.put<LearningGoal>(`${this.apiUrl}/learning-goals/${id}`, request);
  }

  deleteGoal(id: string) {
    return this.http.delete<void>(`${this.apiUrl}/learning-goals/${id}`);
  }

  tasks(goalId: string) {
    return this.http.get<StudyTask[]>(`${this.apiUrl}/learning-goals/${goalId}/tasks`);
  }

  createTask(goalId: string, request: TaskRequest) {
    return this.http.post<StudyTask>(`${this.apiUrl}/learning-goals/${goalId}/tasks`, request);
  }

  setTaskCompletion(taskId: string, completed: boolean) {
    return this.http.patch<StudyTask>(`${this.apiUrl}/tasks/${taskId}/completion`, { completed });
  }

  generateLesson(goalId: string) {
    return this.http.post<LearningLesson>(`${this.apiUrl}/learning-goals/${goalId}/lessons/generate`, {});
  }

  lessons(goalId: string) {
    return this.http.get<LearningLesson[]>(`${this.apiUrl}/learning-goals/${goalId}/lessons`);
  }

  lesson(id: string) {
    return this.http.get<LearningLesson>(`${this.apiUrl}/lessons/${id}`);
  }

  getRecommendation() {
    return this.http.get<AiRecommendation>(`${this.apiUrl}/ai/recommendations`);
  }

  generateRecommendation() {
    return this.http.post<AiRecommendation>(`${this.apiUrl}/ai/recommendations/generate`, {});
  }

  // AI Coach
  coachModes() {
    return this.http.get<CoachModeInfo[]>(`${this.apiUrl}/ai/coach/modes`);
  }

  coachRespond(request: CoachRequest) {
    return this.http.post<CoachResponse>(`${this.apiUrl}/ai/coach/respond`, request);
  }

  coachConversations() {
    return this.http.get<CoachConversation[]>(`${this.apiUrl}/ai/coach/conversations`);
  }

  coachConversation(id: string) {
    return this.http.get<CoachConversationDetail>(`${this.apiUrl}/ai/coach/conversations/${id}`);
  }

  coachCreateConversation(request: CoachRequest) {
    return this.http.post<CoachConversationDetail>(`${this.apiUrl}/ai/coach/conversations`, request);
  }

  coachDeleteConversation(id: string) {
    return this.http.delete<void>(`${this.apiUrl}/ai/coach/conversations/${id}`);
  }

  // Daily Brief
  dailyBrief() {
    return this.http.get<DailyBrief>(`${this.apiUrl}/ai/daily-brief`);
  }

  generateDailyBrief() {
    return this.http.post<DailyBrief>(`${this.apiUrl}/ai/daily-brief/generate`, {});
  }

  refreshDailyBrief() {
    return this.http.post<DailyBrief>(`${this.apiUrl}/ai/daily-brief/refresh`, {});
  }
}
