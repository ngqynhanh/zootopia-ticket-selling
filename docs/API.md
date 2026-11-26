# ZooPass API

Base URL: `/api/v1` · JSON only. The full, always-current reference is generated from the code:
Swagger UI at `/swagger-ui.html`, OpenAPI JSON at `/api-docs`.

## Authentication

1. `POST /auth/login` returns two things:
   - an **access token** (JWT, 15 min) in the body;
   - a **refresh token** in the HttpOnly cookie `zoopass_refresh`, sent only to `/api/v1/auth`.
2. Send the access token with every request: `Authorization: Bearer <accessToken>`.
3. When a request returns 401, call `POST /auth/refresh` with the cookie (`credentials: 'include'`). It returns a new access token and replaces the cookie. Each refresh token works only once, and replaying an old one logs the user out everywhere.
4. `POST /auth/logout` revokes the refresh token and clears the cookie.

Roles: `CUSTOMER`, `STAFF`, `MANAGER`, `ADMIN`. Guests are unauthenticated requests.

## Lists: paging, sorting, filters

| Parameter | Meaning |
|---|---|
| `page` | Zero-based page index (default 0) |
| `size` | Page size, 1–100 (the UI offers 10/20/50/100) |
| `sort` | `field:dir` pairs, e.g. `sort=price:desc,name:asc`; only whitelisted fields |

Every list returns:

```json
{ "content": [...], "page": 1, "size": 20, "totalElements": 156, "totalPages": 8, "from": 21, "to": 40 }
```

`from` and `to` are for the "Showing 21–40 of 156 results" line.

## Endpoints (week 2)

| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/auth/email-available?email=` | Public | Registration step 1: is the email free? |
| POST | `/auth/register` | Public | Create a customer account; sends the verification email |
| POST | `/auth/verify-email` | Public | `{token}` from the email |
| POST | `/auth/resend-verification` | Public | `{email}`; always 202 |
| POST | `/auth/login` | Public | `{email, password}` → access token + cookie |
| POST | `/auth/refresh` | Cookie | New access token, rotated cookie |
| POST | `/auth/logout` | Cookie | 204, cookie cleared |
| POST | `/auth/forgot-password` | Public | `{email}`; always 202 |
| POST | `/auth/reset-password` | Public | `{token, newPassword}` |
| GET / PUT | `/users/me` | Logged in | Own profile |
| PUT | `/users/me/password` | Logged in | `{currentPassword, newPassword}` |
| GET | `/users?q=&role=&active=&emailVerified=&createdFrom=&createdTo=` | Admin | Search users |
| GET / PUT | `/users/{id}` | Admin | Read / edit a profile |
| POST | `/users` | Admin | Create a user with any role |
| PATCH | `/users/{id}/role` | Admin | `{role}` (not on yourself) |
| PATCH | `/users/{id}/status` | Admin | `{active}`: deactivate / reactivate |
| GET | `/audit-logs?actorId=&action=&entityType=&entityId=&from=&to=` | Admin | Audit trail |
| GET | `/ticket-types?q=&category=&active=&minPrice=&maxPrice=&heightCm=` | Public | Search ticket types (guests: active only) |
| GET | `/ticket-types/{id}` | Public | One ticket type |
| POST / PUT | `/ticket-types`, `/ticket-types/{id}` | Manager, Admin | Create / update |
| DELETE | `/ticket-types/{id}` | Manager, Admin | Soft delete |
| PATCH | `/ticket-types/{id}/restore` | Manager, Admin | Undo soft delete |
| GET | `/combos?q=&active=&minPrice=&maxPrice=&maxDuration=&groupSize=&age=` | Public | Search combos |
| GET, POST, PUT, DELETE, PATCH restore | `/combos…` | As ticket types | Combo CRUD |
| GET | `/settings/public` | Public | Opening hours, sales cut-off, group size, deposit % |

## Errors

Every error has the same body:

```json
{
  "timestamp": "2026-11-26T06:15:00Z",
  "status": 400,
  "code": "VALIDATION_FAILED",
  "message": "Dữ liệu không hợp lệ",
  "errors": [{ "field": "password", "message": "Mật khẩu phải có 8–64 ký tự, ..." }],
  "path": "/api/v1/auth/register",
  "requestId": "4356e066-5cc0-429f-86d5-ab5e1598414d"
}
```

| Code | HTTP | When |
|---|---|---|
| `VALIDATION_FAILED` | 400 | Invalid input; `errors` lists each field |
| `MALFORMED_REQUEST` | 400 | Body is not valid JSON |
| `INVALID_TOKEN` | 400 | Verification or reset link unknown, used or expired |
| `UNAUTHORIZED` | 401 | Not logged in, wrong credentials, or expired session |
| `FORBIDDEN` | 403 | The role may not do this |
| `EMAIL_NOT_VERIFIED` | 403 | Login before verifying the email |
| `ACCOUNT_DISABLED` | 403 | The account was deactivated |
| `RESOURCE_NOT_FOUND` | 404 | Unknown id or URL |
| `METHOD_NOT_ALLOWED` | 405 | Wrong HTTP method |
| `CONFLICT` | 409 | Duplicate email or code, or the record is in use |
| `PAYLOAD_TOO_LARGE` | 413 | Upload over 5 MB |
| `BUSINESS_RULE_VIOLATION` | 422 | Valid request that breaks a rule (e.g. demoting yourself) |
| `TOO_MANY_REQUESTS` | 429 | Too many failed logins; wait 15 minutes |
| `INTERNAL_ERROR` | 500 | Unexpected; details only in the server log, found via `requestId` |
