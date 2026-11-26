# ZooPass

Online ticketing and visit planning for Saigon Zoo (Thảo Cầm Viên Sài Gòn). Final project for the
Web Application Development course.

> Course project: the zoo's prices and policies are used as realistic reference data only. ZooPass is
> not an official Saigon Zoo service.

## Tech stack

| Layer | Technology |
|---|---|
| Frontend | React + TypeScript + Vite + TailwindCSS |
| Backend | Java 21, Spring Boot 3.5, Maven, Spring Data JPA, Flyway |
| Database | PostgreSQL 16 |
| Packaging | Docker, Docker Compose |

## Repository layout

```
├── backend/      Spring Boot REST API (see backend/README.md)
├── frontend/     React app (to be added)
├── database/     schema.sql, ER diagram, seed data
├── docs/         API.md, backend roadmap (deployment and contributing guides to come)
├── docker-compose.yml
└── .env.example
```

## Quick start

```bash
cp .env.example .env
docker compose up -d --build
```

The API is then served at http://localhost:8080 and Swagger UI at http://localhost:8080/swagger-ui.html.
Running without Docker is described in [backend/README.md](backend/README.md).

## Team

| Member | Role |
|---|---|
| Nguyễn Quỳnh Anh | Backend & database |
| Nguyễn Ngọc Phương Như | Frontend & UI |

## Status

- **Week 1:**
  - ERD, plus a SQL schema of 13 core tables and 2 junction tables;
  - Spring Boot skeleton;
  - Docker.
- **Week 2:**
  - JWT authentication with email verification, refresh-token rotation and a login rate limit;
  - role-based access;
  - profile and admin user management;
  - audit log;
  - ticket type and combo CRUD with search, filters and paging.

API reference: [docs/API.md](docs/API.md). Next steps: [docs/backend-roadmap.md](docs/backend-roadmap.md).
