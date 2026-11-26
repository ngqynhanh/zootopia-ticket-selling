# ZooPass – Backend

REST API built with Spring Boot 3.5 (Java 21, Maven) and PostgreSQL.

## Prerequisites

- JDK 21+ (tested with JDK 25, compiled for Java 21)
- Maven 3.9+
- PostgreSQL 14+ **or** Docker (to run the app). Tests need neither: they start an embedded PostgreSQL.

## Run

**Option A – Docker (recommended)**, from the project root:

```bash
cp .env.example .env
docker compose up -d --build
```

**Option B – local PostgreSQL.** Create the database once:

```sql
CREATE USER zoopass WITH PASSWORD 'zoopass';
CREATE DATABASE zoopass OWNER zoopass;
```

Then start the backend:

```bash
cd backend
mvn spring-boot:run
```

Flyway creates the tables and reference data on first start.

| URL | What |
|---|---|
| http://localhost:8080/swagger-ui.html | Swagger UI |
| http://localhost:8080/api-docs | OpenAPI JSON |
| http://localhost:8080/actuator/health | Health check (also checks the DB) |

## Test

```bash
mvn test
```

Integration tests run against a real embedded PostgreSQL, the same database engine as production. They check:

- the schema loads through Flyway and matches the JPA entities;
- schema size requirements are met and every foreign key is indexed;
- business-rule constraints, cascades and triggers;
- the security chain: tokens, roles, CORS, headers;
- the auth flows (register, verify, login, refresh rotation, rate limit, password reset);
- user management and the catalog API, including filters, paging, soft delete and audit entries;
- the error response format.

## Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | `dev` = debug detail in errors, `prod` = generic errors |
| `DB_URL` | `jdbc:postgresql://localhost:5432/zoopass` | JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | `zoopass` / `zoopass` | DB credentials (always override in prod) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Comma-separated frontend origins |
| `JWT_SECRET` | dev profile only | HMAC key for access tokens, ≥ 32 characters (**required in prod**) |
| `FRONTEND_URL` | `http://localhost:5173` | Base of links in emails |
| `REFRESH_COOKIE_SECURE` / `REFRESH_COOKIE_SAME_SITE` | `false` / `Lax` (prod: `true`) | Use `true` / `None` when frontend and API are on different domains |
| `SPRING_MAIL_HOST`, `_PORT`, `_USERNAME`, `_PASSWORD` | – | SMTP; when unset, emails go to the log |
| `MAIL_FROM` | `ZooPass <no-reply@zoopass.test>` | Sender address |
| `PORT` | `8080` | HTTP port (Render/Railway set it) |
| `LOG_FILE` | `logs/zoopass.log` | Rolling log file (daily / 10 MB, 14 days) |

## Package structure

```
com.zoopass
├── config/        AppProperties, JPA auditing (current user), OpenAPI, DevDataSeeder (dev profile)
├── security/      SecurityConfig, JwtService, JwtAuthenticationFilter, CurrentUser, 401/403 handlers
├── auth/          register / verify / login / refresh / logout / password reset, tokens, login rate limit
├── user/          User, Role, profile + admin user management
├── catalog/       TicketType, Combo: public search + manager CRUD
├── setting/       SystemSetting: runtime business parameters
├── audit/         AuditLog: who did what, admin search API
├── notification/  EmailService (SMTP, or log in development)
└── common/
    ├── entity/      BaseEntity (id + audit columns), SoftDeletableEntity
    ├── exception/   ApiError, ErrorCode, ApiException + subclasses, GlobalExceptionHandler
    ├── dto/         PageResponse ("Showing 21–40 of 156")
    ├── validation/  @PlainText (no markup), password / phone patterns
    └── web/         RequestIdFilter, PageRequests (paging + sort whitelist), SpecificationUtils
```

Each feature module follows the same layering: `Controller → Service → Repository`, with entities kept
separate from request/response DTOs (records).

## API conventions

- Prefix `/api/v1`, JSON only.
- Status codes:

  | Code | When |
  |---|---|
  | 200 / 201 / 204 | Success |
  | 400 | Validation error |
  | 401 / 403 | Not authenticated / not allowed |
  | 404 | Not found |
  | 409 | Conflict |
  | 422 | Business rule broken |
  | 500 | Unexpected error |

- Every error has the same body:

```json
{
  "timestamp": "2026-11-16T03:47:37Z",
  "status": 400,
  "code": "VALIDATION_FAILED",
  "message": "Dữ liệu không hợp lệ",
  "errors": [{ "field": "size", "message": "Kích thước trang tối đa là 100" }],
  "path": "/api/v1/combos",
  "requestId": "1340f6bc-6f7c-4fad-9344-5c3e3c41c96a"
}
```

`requestId` matches the `X-Request-Id` response header and the server log line, so a reported error can be
traced. In the `dev` profile a `debug` field with the exception detail is added.

## Endpoints

See [docs/API.md](../docs/API.md) for the endpoint list, authentication flow and error codes, or Swagger UI
for the live reference.

## Development accounts

With the `dev` profile, these verified accounts are created on startup:

| Role | Email | Password |
|---|---|---|
| Admin | admin@zoopass.test | Admin123! |
| Manager | manager@zoopass.test | Manager123! |
| Staff | staff@zoopass.test | Staff123! |
| Customer | customer@zoopass.test | Customer123! |

Without SMTP settings, verification and reset emails (with their links) are printed in the log.
