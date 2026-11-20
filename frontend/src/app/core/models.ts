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
  aiRecommendation: AiRecommendation | null;
}

export interface LessonSection {
  heading: string;
  explanation: string;
}

export interface LessonExample {
  title: string;
  scenario: string;
  solution: string;
}

export interface LessonContent {
  learningObjectives: string[];
  sections: LessonSection[];
  examples: LessonExample[];
  keyTakeaways: string[];
}

export interface AiRecommendation {
  id: string;
  learningGoalId: string | null;
  title: string;
  recommendation: string;
  reason: string;
  topic: string;
  activityType: string;
  estimatedMinutes: number;
  suggestedActions: string[];
  providerName: string | null;
  fallbackUsed: boolean;
  createdAt: string;
}

export interface LearningLesson {
  id: string;
  learningGoalId: string;
  studentId: string;
  title: string;
  topic: string;
  difficulty: ProficiencyLevel;
  content: LessonContent;
  estimatedStudyMinutes: number;
  createdAt: string;
}

export type CoachMode = 'LEARN' | 'PRACTICE' | 'QUIZ' | 'PLAN' | 'INTERVIEW' | 'IMPROVE';

export interface CoachModeInfo {
  value: CoachMode;
  label: string;
  description: string;
}

export interface CoachAction {
  type: string;
  label: string;
  payload: string;
}

export interface CoachMetadata {
  provider: string;
  model: string;
  durationMs: number;
  fallbackUsed: boolean;
}

export interface CoachMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  providerName: string | null;
  modelName: string | null;
  createdAt: string;
}

export interface CoachConversation {
  id: string;
  title: string;
  subject: string | null;
  topic: string | null;
  createdAt: string;
  updatedAt: string;
  messageCount: number;
}

export interface CoachConversationDetail extends CoachConversation {
  messages: CoachMessage[];
}

export interface CoachRequest {
  conversationId: string | null;
  message: string;
  mode: CoachMode;
  history: CoachMessage[];
}

export interface CoachResponse {
  conversationId: string;
  reply: string;
  mode: CoachMode;
  suggestedQuestions: string[];
  actions: CoachAction[];
  metadata: CoachMetadata;
}

export interface DailyBrief {
  id: string;
  briefDate: string;
  focusSummary: string;
  priorityTasks: string;
  revisionTopics: string;
  dailyObjective: string;
  recommendedActions: string[];
  providerName: string | null;
  modelName: string | null;
  generationDurationMs: number | null;
  fallbackUsed: boolean;
  createdAt: string;
}
