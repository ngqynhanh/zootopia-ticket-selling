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

- the schema loads through Flyway;
- schema size requirements are met and every foreign key is indexed;
- business-rule constraints, cascades and triggers;
- the error response format.

## Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | `dev` = debug detail in errors, `prod` = generic errors |
| `DB_URL` | `jdbc:postgresql://localhost:5432/zoopass` | JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | `zoopass` / `zoopass` | DB credentials (always override in prod) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Comma-separated frontend origins |
| `PORT` | `8080` | HTTP port (Render/Railway set it) |
| `LOG_FILE` | `logs/zoopass.log` | Rolling log file (daily / 10 MB, 14 days) |

## Package structure

```
com.zoopass
├── config/        AppProperties, CORS, JPA auditing, OpenAPI
├── common/
│   ├── entity/    BaseEntity (id + audit columns), SoftDeletableEntity
│   ├── exception/ ApiError, ErrorCode, ApiException + subclasses, GlobalExceptionHandler
│   └── web/       RequestIdFilter (X-Request-Id, access log)
└── (feature modules from week 2: user/, auth/, catalog/, order/, ... – see docs/backend-roadmap.md)
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
  | 401 / 403 | Not authenticated / not allowed (week 2) |
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

Week 1 is the skeleton only: no business endpoints yet, just `/actuator/health` and Swagger UI.
The API is built from week 2 (see [docs/backend-roadmap.md](../docs/backend-roadmap.md)).
