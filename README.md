# Lumen — AI-Based Personalized Learning Assistant

The first milestone is a locally runnable learning-planning foundation: student registration and login, JWT-protected learning goals and study tasks, and a personalized dashboard backed by PostgreSQL.

## Stack

- Angular 21, standalone components, TypeScript, and Angular's Vitest-based test runner
- Java 21 / Spring Boot 3.5, Spring Security, BCrypt, and JWT
- PostgreSQL 17 with Flyway migrations
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
3. On the goal details page, add study tasks with optional due dates.
4. Mark a task complete from the goal page or dashboard. Dashboard counts and recent activity refresh from the API.
5. Reload the browser and sign in again; the goal and task state are stored in PostgreSQL.
6. Open Swagger UI to inspect the API. Protected endpoints require the `Bearer` token returned by registration/login.

All learning-goal and task reads/updates are scoped to the authenticated student's email. Passwords are BCrypt-hashed, tokens are signed using the environment-provided JWT secret, and task completion timestamps are persisted.

## Tests

From the repository root:

```powershell
mvn -f backend\pom.xml verify
Set-Location frontend
npm test -- --watch=false
npm run build
```

Backend integration tests use an in-memory H2 database in PostgreSQL compatibility mode; production/local app persistence uses PostgreSQL. The CI workflow runs backend verification, frontend tests, and a production frontend build.
