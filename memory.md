# Disha AI - AI-Based Personalized Learning Assistant

## Project Overview

**Disha AI** is a personalized learning assistant that helps students turn learning goals into structured, actionable plans with AI-powered guidance. The application combines goal management, task tracking, AI-generated lessons, and an intelligent learning coach that adapts to each student's progress.

## Tech Stack

### Backend
- **Java 21** / **Spring Boot 3.5.x**
- **Spring Security** with JWT authentication
- **Spring Data JPA** / **Hibernate** with MySQL 8.x
- **Flyway** for database migrations
- **Maven** for build management
- **AI Providers**: Ollama (primary, local) → Gemini (fallback, cloud)
- **SpringDoc OpenAPI** for API documentation

### Frontend
- **Angular 21** with standalone components
- **TypeScript** with strict mode
- **RxJS** for reactive programming
- **Angular CLI** with Vitest for testing
- Pure CSS design system (no external UI library)
- Vite-based dev server (Angular 21 default)

### Database
- **MySQL 8.x** with InnoDB
- UUID primary keys (BINARY(16))
- JSON columns for structured AI content
- Flyway migrations in `backend/src/main/resources/db/migration/mysql/`

## Project Structure

```
Disha AI/
├── backend/
│   ├── src/main/java/com/learningassistant/
│   │   ├── ai/                 # AI provider architecture + Coach services
│   │   │   ├── AiProvider.java              # Interface for AI providers
│   │   │   ├── AiProviderRouter.java        # Ollama → Gemini fallback
│   │   │   ├── OllamaAiProvider.java        # Local Ollama integration
│   │   │   ├── GeminiAiProvider.java        # Google Gemini integration
│   │   │   ├── StudentContext.java          # Structured student data for AI
│   │   │   ├── StudentContextService.java   # Builds personalized context
│   │   │   ├── ChatService.java             # Conversation persistence
│   │   │   ├── CoachResponse.java           # Structured coach responses
│   │   │   └── CoachService.java            # Main AI coach logic (TODO)
│   │   ├── auth/               # Authentication & JWT
│   │   ├── chat/               # Chat entities & repositories
│   │   │   ├── Conversation.java
│   │   │   ├── ChatMessage.java
│   │   │   ├── ConversationRepository.java
│   │   │   └── ChatMessageRepository.java
│   │   ├── common/             # Exception handling
│   │   ├── config/             # Security & JWT config
│   │   ├── dashboard/          # Dashboard aggregation
│   │   ├── learninggoal/       # Learning goals CRUD
│   │   ├── lesson/             # AI-generated lessons
│   │   ├── recommendation/     # AI recommendations (cached 6hr)
│   │   ├── student/            # Student entity
│   │   └── task/               # Study tasks CRUD
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/mysql/  # Flyway migrations V1-V7
│
├── frontend/
│   ├── src/app/
│   │   ├── core/               # Services, models, guards, interceptors
│   │   │   ├── models.ts                # All TypeScript interfaces
│   │   │   ├── learning.service.ts      # API calls
│   │   │   ├── auth.service.ts          # Auth state (signals)
│   │   │   ├── auth.interceptor.ts      # JWT interceptor
│   │   │   └── auth.guard.ts            # Route guard
│   │   ├── pages/              # Lazy-loaded page components
│   │   │   ├── landing-page.ts          # Public landing
│   │   │   ├── auth-page.ts             # Login/Register
│   │   │   ├── dashboard-page.ts        # Main dashboard + AI panel
│   │   │   ├── goal-form-page.ts        # Create goal
│   │   │   ├── goal-detail-page.ts      # Goal + tasks + lessons
│   │   │   └── lesson-page.ts           # Read-only lesson viewer
│   │   ├── shared/             # Reusable UI components
│   │   │   ├── dashboard-card.ts
│   │   │   ├── error-message.ts
│   │   │   ├── loading-state.ts
│   │   │   └── task-item.ts
│   │   ├── app.ts              # Root component + header
│   │   ├── app.routes.ts       # Route definitions
│   │   └── app.config.ts       # App providers
│   └── src/styles.css          # 718-line design system
│
├── docs/
├── deploy/
├── infra/
├── ops/
└── .github/
```

## Database Schema (MySQL)

### Core Tables
| Table | Purpose |
|-------|---------|
| `students` | User accounts (email, password_hash, display_name) |
| `learning_goals` | Student goals (title, subject, topic, proficiency, target_date, status) |
| `study_tasks` | Tasks linked to goals (title, description, type, minutes, due_date, completed) |
| `lessons` | AI-generated lessons (JSON content, difficulty, estimated_minutes) |
| `ai_recommendations` | Cached AI recommendations (6hr TTL, provider metadata) |
| `conversations` | AI coach chat sessions (title, subject, topic, timestamps) |
| `chat_messages` | Individual messages (role, content, provider, model) |

### Key Relationships
- Student 1→N LearningGoal
- LearningGoal 1→N StudyTask
- LearningGoal 1→N Lesson
- Student 1→N AiRecommendation
- Student 1→N Conversation
- Conversation 1→N ChatMessage

## Existing Features

### 1. Learning Goals
- Create, read, update, delete goals
- Proficiency levels: BEGINNER, INTERMEDIATE, ADVANCED
- Status: ACTIVE, COMPLETED, PAUSED
- Target dates with progress tracking

### 2. Study Tasks
- Four types: STUDY, PRACTICE, REVIEW, ASSIGNMENT
- Estimated minutes (1-1440)
- Due dates with overdue tracking
- Completion toggles with timestamps
- Optimistic UI updates

### 3. AI-Generated Lessons
- Structured JSON content: objectives, sections, examples, takeaways
- Ollama primary → Gemini fallback for lesson generation
- Proficiency-aware prompts
- Content validation (length, structure)
- Persisted per student per goal

### 4. AI Recommendations (Dashboard)
- Single recommendation panel on dashboard
- 6-hour cache to avoid repeated LLM calls
- Manual refresh button
- Based on: goals, completed/pending tasks, student name
- Returns: title, recommendation, reason, topic, activityType, minutes, suggestedActions

### 5. Dashboard
- Stats: active goals, completed/remaining tasks, progress %
- Today's focus (due today)
- Coming up (next 7 days)
- Recent activity (last 30 days)
- AI recommendation panel (cached or manual refresh)

### 6. Authentication
- JWT tokens (HS256, configurable expiry)
- BCrypt password hashing
- Register/Login endpoints
- Signal-based auth state in frontend with localStorage persistence

## AI Architecture

### Provider Pattern
```
AiProvider (interface)
    ├── OllamaAiProvider (primary)
    │   ├── POST /api/generate to local Ollama
    │   ├── Health check: GET /api/tags
    │   └── Model: llama3.2:3b (configurable)
    └── GeminiAiProvider (fallback)
        ├── POST /models/{model}:generateContent
        ├── API key in x-goog-api-key header
        └── Model: gemini-3.8-flash (configurable)
```

### Routing Logic (AiProviderRouter)
1. Try Ollama first
2. On retryable error (timeout, 5xx, model not found) → try Gemini if fallback enabled
3. On non-retryable error → fail immediately
4. Returns `AiGenerationResult` with provider metadata

### Personalization Context (StudentContextService)
Builds structured context for LLM:
- Student display name
- Active goals (title, subject, topic, proficiency, target, status)
- Pending tasks (up to 10, sorted by due date, with overdue flag)
- Completed tasks (up to 10, recent first)
- Recent lessons (up to 5)
- Recent recommendations (up to 3)
- Counts: pending, completed, overdue

### Conversation Persistence
Entities exist but **no service/controller implemented yet**:
- `Conversation` - chat sessions with title/subject/topic
- `ChatMessage` - role (user/assistant), content, provider, model
- Repositories: `ConversationRepository`, `ChatMessageRepository`

## Planned Enhancements (In Progress)

### Smart Todo Features
- [ ] AI-powered task prioritization
- [ ] Task breakdown from goals
- [ ] Recurring tasks & habits
- [ ] Time-blocking suggestions
- [ ] Progress analytics per topic
- [ ] Smart notifications/reminders

### Fully Featured AI Coach
- [ ] Conversational modal with history
- [ ] Six interaction modes:
  1. **Learn** - Explain concepts step-by-step
  2. **Practice** - Generate questions, evaluate answers
  3. **Quiz** - Interactive quizzes with scoring
  4. **Plan** - Create study schedules
  5. **Interview** - Mock interviews with feedback
  6. **Improve** - Analyze performance, suggest improvements
- [ ] Structured responses with:
  - Suggested follow-up questions
  - Action buttons (Start practice, Create task, etc.)
  - Code blocks, markdown rendering
- [ ] Persistent conversation history
- [ ] Context-aware (uses StudentContext)

### Daily Learning Brief
- [ ] Cached daily brief (24hr TTL)
- [ ] Focus areas, priority tasks, revision topics
- [ ] One achievable daily objective
- [ ] Async loading on dashboard

### Header CTAs
- [ ] Primary: "Start Learning" → active goal or goal creation
- [ ] Secondary: "Ask AI Coach" → opens conversational modal

## Configuration

### Environment Variables (`.env`)
```bash
DB_URL=jdbc:mysql://localhost:3306/learning_assistant
DB_USERNAME=root
DB_PASSWORD=your_password
JWT_SECRET=base64_encoded_32_byte_secret
JWT_EXPIRATION_SECONDS=3600
GEMINI_API_KEY=your_gemini_key
GEMINI_MODEL=gemini-3.8-flash
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=llama3.2:3b
AI_FALLBACK_ENABLED=true
AI_GEMINI_ENABLED=true
```

### Key Application Properties
- `spring.jpa.hibernate.ddl-auto=validate` (Flyway owns schema)
- `ai.fallback.enabled=true` (Ollama → Gemini)
- `ai.ollama.enabled=true`
- `ai.gemini.enabled=true`
- Recommendation cache: 6 hours
- Dashboard loads AI recommendation asynchronously

## Development Commands

### Backend
```powershell
# From repo root
mvn -f backend\pom.xml clean compile
mvn -f backend\pomxml test
mvn -f backend\pomxml spring-boot:run
```

### Frontend
```powershell
# From frontend/
npm ci
npm start          # ng serve on :4200
npm run build      # ng build
npm test           # vitest
```

### Docker
```powershell
docker compose up -d postgres    # Database only
docker compose up --build -d     # All services
```

## API Endpoints (Key)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/auth/register` | Register new student |
| POST | `/api/auth/login` | Login, returns JWT |
| GET | `/api/dashboard` | Aggregated dashboard data |
| GET | `/api/learning-goals` | List active goals |
| POST | `/api/learning-goals` | Create goal |
| GET | `/api/learning-goals/{id}/tasks` | List tasks for goal |
| POST | `/api/learning-goals/{id}/tasks` | Create task |
| PATCH | `/api/tasks/{id}/completion` | Toggle completion |
| POST | `/api/learning-goals/{id}/lessons/generate` | Generate AI lesson |
| GET | `/api/ai/recommendations` | Get cached recommendation |
| POST | `/api/ai/recommendations/generate` | Force new recommendation |
| GET | `/api/ai/status` | Provider availability |

## Known Limitations

1. **No conversation API yet** - Entities exist but no REST endpoints
2. **No markdown rendering** - AI responses show as plain text
3. **Single recommendation only** - No history or multiple recommendations
4. **No streaming responses** - Full response waits for completion
5. **No WebSocket/SSE** - Polling only
6. **Ollama must be running locally** - No cloud fallback if both fail
7. **No push notifications** - Manual refresh only
8. **No assessment/quiz engine** - Practice mode not implemented

## Next Implementation Priority

1. **CoachService** - Structured AI responses with modes
2. **ChatController** - REST endpoints for conversations
3. **DailyBriefService** - Cached daily learning briefs
4. **Frontend AI Coach Modal** - Conversational UI with mode selector
5. **Smart Todo Features** - AI-powered task management
6. **Markdown Renderer** - Rich AI response display