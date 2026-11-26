# Backend roadmap

From week 2 on, one member (Nguyễn Quỳnh Anh) owns the backend for the rest of the project, so there are no
more weekly BE/FE swaps. This file says what the backend has and what comes next. Commit `e06fc07`
("first commit") still holds drafts of later SQL blocks. Restore one with the command below, then
**read, adapt and test it** before committing:

```bash
git checkout e06fc07 -- <path>
```

## Week 1 – done

- [x] ERD: `database/ER-diagram.md`
- [x] SQL schema with constraints, FKs and indexes: `V1__init_schema.sql` (13 core tables + 2 junction tables)
- [x] Spring Boot skeleton:
  - config, Swagger;
  - audit base entities;
  - `GlobalExceptionHandler`;
  - request-id logging;
  - Docker.

## Week 2 – done (branch `feature/week2-auth-crud`)

- [x] `V2`: `user_tokens`, `audit_logs`, `system_settings`.
- [x] `V3`: reference data (ticket prices, group policies, combos, settings).
- [x] Spring Security:
  - stateless JWT (15 min) and BCrypt;
  - `@PreAuthorize` role checks;
  - CORS and security headers;
  - 401/403 errors in the `ApiError` format.
- [x] Auth flows:
  - register, then verify by email;
  - login with a rate limit (5 failures / 15 min → 429);
  - refresh-token rotation in an HttpOnly cookie, with reuse detection;
  - logout;
  - forgot / reset password.
- [x] Profile (`/users/me`) and admin user management: search/filter/sort/paging, create, role change, deactivate.
- [x] Audit log: service plus admin API.
- [x] Ticket type and combo CRUD:
  - public search with at least 5 filters;
  - manager create, update, soft delete and restore.
- [x] Dev profile seeds `admin@ / manager@ / staff@ / customer@zoopass.test`.
- [x] 56 automated tests (embedded PostgreSQL).

## Week 3 – Business logic (next)

- Orders:
  - individual ticket order with server-side pricing;
  - date rules: up to 30 days ahead, same-day sales until 17:30;
  - infant tickets need an adult ticket;
  - tours need at least 6 tickets.
- Group booking:
  - pricing strategy per group category;
  - guides = ceil(visitors / 50);
  - 30% deposit.
- Order state machine (valid transitions only, history in the audit log).
- Proof-document upload (type/size/magic-byte checks) and the Staff review queue.
- QR e-ticket generation (one QR per transaction, 4 people per scan, valid 30 days).

## Week 4 – Search & advanced features

- `check_ins` table and the gate check-in API.
- `faqs` (full-text search), `animals`, `notifications`, `user_saved_activities`: SQL blocks in `e06fc07`. Add them as new migrations.
- VNPay sandbox, HTML email templates, scheduled jobs, revenue reports.

## Week 5 – Deploy

- `database/seed-data.sql` (100+ records), deployment, docs.
