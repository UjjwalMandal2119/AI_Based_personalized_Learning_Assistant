# Disha AI — AI-Based Personalized Learning Assistant

This project provides student registration/login, JWT-protected learning goals and study tasks, a personalized dashboard, and Gemini-generated lessons persisted in PostgreSQL.

## Stack

- Angular 21, standalone components, TypeScript, and Angular's Vitest-based test runner
- Java 21 / Spring Boot 3.5, Spring Security, BCrypt, and JWT
- PostgreSQL 17 with Flyway migrations
- Gemini API `generateContent` with schema-constrained JSON output
- Maven, Docker Compose, and GitHub Actions

The workspace was scaffolded with the installed Node 24.14 / npm 11 / Angular CLI 21.2 toolchain. Java 25 is installed on the development machine; the Maven compiler targets Java 21 (`--release 21`), and the container/CI runtime uses Java 21. Install a Java 21 JDK if you need to run the packaged application locally using exactly the production runtime.

## Prerequisites

- Java 21 (Java 25 can compile this project with its Java 21 target)
- Maven 3.9+
- Node.js 24 and npm 11
- Docker Desktop with the Linux container engine running, plus Docker Compose

## Configure and start PostgreSQL

From the repository root in PowerShell:

```powershell
Copy-Item .env.example .env
```

Edit `.env` and replace the password and JWT placeholder. Generate a base64 JWT secret of 32 random bytes with:

```powershell
[Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

Copy the output into `JWT_SECRET` in `.env`. Do not commit `.env`.

Start PostgreSQL (or the PostgreSQL and backend containers together):

```powershell
docker compose up -d postgres
# Optional: build and start the backend container as well
docker compose up --build -d
docker compose ps
```

The PostgreSQL data is stored in the named `postgres_data` volume. Flyway creates the schema on the backend's first startup. Stop the services without removing data using `docker compose down`. `docker compose down -v` removes the database volume and all persisted local data.

## Run the backend locally

With PostgreSQL running, load the variables from `.env` into the current PowerShell process and start Spring Boot:

```powershell
Get-Content .env | Where-Object { $_ -match '^[^#]+=.*$' } | ForEach-Object {
  $name, $value = $_ -split '=', 2
  [Environment]::SetEnvironmentVariable($name, $value, 'Process')
}
mvn -f backend\pom.xml spring-boot:run
```

The API runs at `http://localhost:8080`. OpenAPI documentation is available at `http://localhost:8080/swagger-ui.html`.

## Configure Gemini lesson generation

Create a Gemini API key in [Google AI Studio](https://aistudio.google.com/apikey) and set `GEMINI_API_KEY` in the untracked `.env` file. The backend passes it only in the `x-goog-api-key` request header; it is never sent to Angular or returned by the API. The default model is [`gemini-3.8-flash`](https://ai.google.dev/gemini-api/docs/models/gemini-3.8-flash), whose current model documentation lists structured output support. The integration follows Google's [GenerateContent REST API](https://ai.google.dev/api/generate-content) and [structured output guide](https://ai.google.dev/gemini-api/docs/structured-output). Override the model with `GEMINI_MODEL` only with a model ID supported by your Gemini API key. Backend connection/response timeout defaults are 5 seconds and 60 seconds.

The backend uses Google's REST `models.generateContent` endpoint with `responseMimeType: application/json` and a response schema, then validates every generated field before persisting it. Provider rate limits return HTTP 429; malformed provider output returns HTTP 502; unavailable provider/configuration returns HTTP 503. No generated raw text is accepted by the UI.

## Run the frontend locally

In a second PowerShell terminal:

```powershell
Set-Location frontend
npm ci
npm start
```

Open `http://localhost:4200`. The frontend API URL is configured in `frontend/src/environments/environment.ts`; production builds use `environment.prod.ts`.

## Verify the vertical slice

1. Register at `/register`, then log in at `/login`.
2. Create a learning goal from the dashboard.
3. On the goal details page, add study tasks with optional due dates or choose **Generate study material**.
4. The generated lesson opens with objectives, sections, worked examples, takeaways, and estimated study time. Use **Saved lessons** on the goal page to reopen persisted lessons after navigating away or refreshing.
5. Mark a task complete from the goal page or dashboard. Dashboard counts and recent activity refresh from the API.
6. Reload the browser and sign in again; goals, tasks, and generated lessons are stored in PostgreSQL.
7. Open Swagger UI to inspect the API. Protected endpoints require the `Bearer` token returned by registration/login.

All learning-goal, task, and lesson reads/updates are scoped to the authenticated student's email. Passwords are BCrypt-hashed, tokens are signed using the environment-provided JWT secret, and task completion timestamps are persisted. Lesson structured content is stored as PostgreSQL JSONB.

## Lesson API

- `POST /api/learning-goals/{goalId}/lessons/generate` — generate and persist one lesson for the owned goal.
- `GET /api/learning-goals/{goalId}/lessons` — list the student's saved lessons for an owned goal.
- `GET /api/lessons/{lessonId}` — reopen a saved lesson owned by the authenticated student.

The generate button is disabled while a generation request is active, preventing accidental repeated clicks from the UI.

## Known limitations

- Lesson generation requires a valid Gemini API key, outbound network access, and model/API quota; tests use mocked provider responses and do not make real Gemini requests.
- A generation currently creates a new saved lesson each time the user requests one; intentional re-generation is not deduplicated.
- Lesson text is rendered as plain text (not trusted HTML/markdown), which keeps model output inert but does not provide rich markdown formatting.

## Tests

From the repository root:

```powershell
mvn -f backend\pom.xml verify
Set-Location frontend
npm test -- --watch=false
npm run build
```

Backend integration tests use an in-memory H2 database in PostgreSQL compatibility mode and mocked Gemini responses; production/local app persistence uses PostgreSQL. The CI workflow runs backend verification, frontend tests, and a production frontend build.
