# Backend Configuration Fix Report

## Root cause

The backend requires `DB_PASSWORD` and `JWT_SECRET`; `DB_USERNAME` previously had no default. Spring Boot does not load a dotenv file named `.env` automatically, so invoking `mvn spring-boot:run` in a fresh shell without first exporting the variables leaves required placeholders unresolved. The first datasource placeholder Spring encounters is `DB_USERNAME`; `DB_PASSWORD` and `JWT_SECRET` are also required.

The supplied error excerpt contained only Spring placeholder-resolution stack frames, not the decisive `Could not resolve placeholder 'NAME'` line. Therefore the exact name reported by that original failed run cannot be proven from the excerpt alone. The code confirms the required unresolved placeholders above. The fix imports the ignored root `.env` whether Maven is run from the repository root or `backend`.

The separate MySQL access-denied failure seen earlier was a credential/account issue, not a placeholder issue. The local MySQL root credential was verified, and the application now connects with the configured root account.

## Files inspected

- `pom.xml`
- `src/main/resources/application.yml`
- `src/test/resources/application-test.yml`
- `src/main/resources/db/migration/V1__create_initial_schema.sql`
- `src/main/resources/db/migration/V2__create_lessons.sql`
- `src/main/resources/db/migration/V3__add_google_identity.sql`
- `src/main/resources/db/migration/mysql/V1__create_initial_schema.sql`
- `src/main/resources/db/migration/mysql/V2__create_lessons.sql`
- `src/main/resources/db/migration/mysql/V3__add_google_identity.sql`
- Java `@Value` references and `@Configuration` classes under `src/main/java`
- Root `.env` and `.env.example` (values were not copied into this report)
- Maven wrapper/settings and duplicate application configuration files (none found in the repository; no user Maven `settings.xml` found)

No `application.properties` file exists. Runtime Flyway is configured to use only the MySQL migration directory. The old PostgreSQL migration history remains separate and is not loaded.

## Placeholder inventory

| Property | Required? | Source/default | Purpose |
|---|---|---|---|
| `DB_URL` | No | `jdbc:mysql://localhost:3306/learning_assistant` | MySQL JDBC URL |
| `DB_USERNAME` | No | `root` | Local MySQL account |
| `DB_PASSWORD` | Yes | Root `.env` or process environment | MySQL password; deliberately no default |
| `JWT_SECRET` | Yes | Root `.env` or process environment | Signs application JWTs |
| `JWT_EXPIRATION_SECONDS` | No | `3600` | Application JWT lifetime |
| `SERVER_PORT` | No | `8080` | HTTP port |
| `FLYWAY_LOCATIONS` | No | `classpath:db/migration/mysql` | MySQL schema migrations |
| `GOOGLE_CLIENT_ID` / `app.google.client-id` | No | Empty when unset | Used by existing Google ID-token sign-in code; not an OAuth client-login configuration |
| `GEMINI_API_KEY` / `gemini.api-key` | No | Empty when unset | Optional lesson-generation integration |
| `GEMINI_MODEL` | No | `gemini-3.8-flash` | Lesson-generation model |
| `GEMINI_CONNECT_TIMEOUT` | No | `5s` | Provider connection timeout |
| `GEMINI_RESPONSE_TIMEOUT` | No | `60s` | Provider response timeout |
| `jjwt.version`, `springdoc.version` | Yes in Maven | Defined in `pom.xml` | Dependency versions; not Spring runtime placeholders |

`GOOGLE_CLIENT_SECRET`, `spring.security.oauth2.client`, and `oauth2Login` are not present. However, the repository does contain a Google ID-token sign-in implementation and uses `GOOGLE_CLIENT_ID`; removing that existing configuration or its dependency would break current functionality, so it was preserved.

## Changes made

- `src/main/resources/application.yml`
  - Imports `.env` from the current directory and its parent as optional Java-properties files, supporting both repository-root and backend working directories.
  - Keeps the MySQL URL default, defaults the username to `root`, requires `DB_PASSWORD`, and declares the MySQL JDBC driver.
  - Enables SQL logging.
  - Keeps `ddl-auto: validate`: Flyway owns schema changes, and Hibernate validates rather than mutating an existing database. No `create` or `create-drop` is used for the application database. The test-only H2 database remains isolated in the test profile.
- `src/test/resources/application-test.yml`
  - Explicitly selects the H2 driver for tests so the runtime MySQL driver setting does not override the test database.
- `.env.example`
  - Uses `root` as the local username example; the password remains a placeholder.
- Root `.env` (git-ignored local file)
  - Set `DB_USERNAME=root`; the existing password value was left unchanged and is not recorded here.
- `../README.md`
  - Documents starting Maven directly with the automatically imported `.env`.

No MySQL dialect override is needed; Hibernate detects the MySQL dialect from JDBC metadata.

## Required configuration and startup

The ignored repository-root `.env` must define:

```dotenv
DB_URL=jdbc:mysql://localhost:3306/learning_assistant
DB_USERNAME=root
DB_PASSWORD=<your-local-MySQL-password>
JWT_SECRET=<base64-encoded-random-secret-of-at-least-32-bytes>
```

Do not commit `.env` or place real credentials in source control. To set secrets only for the current PowerShell process instead, run from the repository root:

```powershell
function Set-ProcessSecretFromPrompt([string]$Name, [string]$Prompt) {
    $secureValue = Read-Host $Prompt -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureValue)
    try {
        [Environment]::SetEnvironmentVariable(
            $Name,
            [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer),
            'Process'
        )
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}
Set-ProcessSecretFromPrompt 'DB_PASSWORD' 'MySQL password'
Set-ProcessSecretFromPrompt 'JWT_SECRET' 'Base64 JWT secret'
```

Then start the API from the repository root:

```powershell
mvn -f backend\pom.xml spring-boot:run
```

Or from the backend directory:

```powershell
mvn spring-boot:run
```

If `DB_PASSWORD` or `JWT_SECRET` is absent from both `.env` and the process environment, Spring fails startup with an unresolved-placeholder error instead of using a fallback password.

If the database does not exist, create it without dropping any existing database:

```sql
CREATE DATABASE learning_assistant CHARACTER SET utf8mb4;
```

## Verification results

- MySQL server: running on `localhost:3306`, version `8.0.46` (service `MySQL80`).
- Database: `learning_assistant` exists; configured root credentials verified.
- Flyway: validated all three MySQL migrations; schema `learning_assistant` is at version 3 and up to date.
- Hibernate/JPA: initialized the entity manager and loaded four JPA repository interfaces.
- `mvn clean`: passed.
- `mvn compile`: passed.
- `mvn test`: passed, 8 tests, 0 failures, 0 errors.
- `mvn spring-boot:run`: app loads `.env`, resolves all placeholders, connects to MySQL, runs Flyway, and initializes JPA. Startup completes the data layer successfully. The embedded web server could not bind because port 8080 is already in use by another process in this environment (PID 12540) — an environment/port conflict, not a configuration or placeholder error. Stop the process on 8080 or set `SERVER_PORT` to a free port before running.

## Remaining notes

- Hibernate logs a Spring Security warning about its configured `AuthenticationProvider`; it is not a startup failure and is unrelated to placeholder resolution.
- The generic original stack excerpt does not identify which unresolved-placeholder name appeared in that exact invocation. Based on the old configuration and a fresh shell, `DB_USERNAME` was the first required datasource placeholder without a value; `DB_PASSWORD` and `JWT_SECRET` also needed resolution.
- Using MySQL `root` is limited here to the requested local development setup. Prefer a least-privilege application account outside this local environment.
