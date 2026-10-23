export interface AuthResponse {
  token: string;
  tokenType: string;
  displayName: string;
  email: string;
}

export type ProficiencyLevel = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';
export type GoalStatus = 'ACTIVE' | 'COMPLETED' | 'PAUSED';
export type TaskType = 'STUDY' | 'PRACTICE' | 'REVIEW' | 'ASSIGNMENT';

export interface LearningGoal {
  id: string;
  studentId: string;
  title: string;
  subject: string;
  topic: string;
  proficiencyLevel: ProficiencyLevel;
  targetDate: string | null;
  status: GoalStatus;
  createdAt: string;
}

export interface GoalRequest {
  title: string;
  subject: string;
  topic: string;
  proficiencyLevel: ProficiencyLevel;
  targetDate: string | null;
  status?: GoalStatus;
}

export interface StudyTask {
  id: string;
  learningGoalId: string;
  learningGoalTitle: string;
  title: string;
  description: string | null;
  taskType: TaskType;
  estimatedMinutes: number;
  dueDate: string | null;
  completed: boolean;
  createdAt: string;
  completedAt: string | null;
}

export interface TaskRequest {
  title: string;
  description: string;
  taskType: TaskType;
  estimatedMinutes: number;
  dueDate: string | null;
}

export interface Dashboard {
  displayName: string;
  activeGoals: LearningGoal[];
  completedTasks: number;
  remainingTasks: number;
  tasksDueToday: StudyTask[];
  upcomingTasks: StudyTask[];
  recentActivity: StudyTask[];
}
