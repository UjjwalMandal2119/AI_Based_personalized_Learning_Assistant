import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { Dashboard, GoalRequest, LearningGoal, StudyTask, TaskRequest } from './models';

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
}
