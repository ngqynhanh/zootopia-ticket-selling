# Backend roadmap

Members swap BE/FE every week, so this file says what the backend already has and what comes next.
Commit `e06fc07` ("first commit") holds a ready draft of several later-week pieces. Restore a file
from it instead of rewriting it, then **read it, adapt it and test it** before committing:

```bash
git checkout e06fc07 -- <path>
```

## Week 1 – done

- [x] ERD: `database/ER-diagram.md`
- [x] SQL schema with constraints, FKs and indexes: `V1__init_schema.sql` (13 core tables + 2 junction tables)
- [x] Spring Boot project:
  - config and profiles, CORS, Swagger;
  - `BaseEntity` with audit columns, `SoftDeletableEntity`;
  - `GlobalExceptionHandler` with the shared `ApiError` format;
  - request-id logging with file rotation;
  - Dockerfile, docker-compose.

## Week 2 – Users & core CRUD

| Task | Draft in `e06fc07` |
|---|---|
| `User` entity, `Role` enum, `UserRepository` | `backend/src/main/java/com/zoopass/user/` |
| `user_tokens` table (email verification, password reset, refresh token) as `V2__...sql` | `user_tokens` block in `V1__init_schema.sql` |
| Spring Security, BCrypt, JWT login/refresh, rate limit, RBAC | – |
| Fill `JpaAuditingConfig` auditor from the SecurityContext | – |
| `TicketType`, `Combo` entities + public read API | `backend/src/main/java/com/zoopass/catalog/` |
| Paginated response "Showing 21–40 of 156" | `backend/src/main/java/com/zoopass/common/dto/PageResponse.java` |
| Reference data: ticket prices, group policies, combos | `V2__reference_data.sql` (renumber it) |
| `system_settings` table + service/API | `system_settings` block + `backend/src/main/java/com/zoopass/setting/` |
| `audit_logs` table | `audit_logs` block |
| Tests | `backend/src/test/java/com/zoopass/{catalog,user,common/dto}/` |

## Week 3 – Business logic

- Pricing service (individual + group policies), order state machine.
- Upload of proof documents, Staff review.
- QR e-ticket generation.

## Week 4 – Search & advanced features

- `check_ins` table (QR scan, 1–4 people per scan).
- `faqs` (full-text search), `animals`, `notifications`, `user_saved_activities` tables.
- VNPay, email, scheduled jobs, reports.

The SQL for these tables is in the matching blocks of `V1__init_schema.sql` at `e06fc07`. Add them as new
migrations; never edit a migration a teammate has already run.

## Week 5 – Deploy

- `database/seed-data.sql` (100+ records), deployment, docs.
