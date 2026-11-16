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
├── docs/         API, deployment, contributing guides (to be added)
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
| A | Backend & database |
| B | Frontend & UI |

## Status

Week 1 (09/11 – 15/11), backend part, is done:

- ERD;
- SQL schema: 13 core tables + 2 junction tables, with constraints, FKs and indexes;
- Spring Boot project skeleton;
- Docker setup.

Next is week 2: authentication (JWT), RBAC, and CRUD for users, ticket types and combos.
See [docs/backend-roadmap.md](docs/backend-roadmap.md).
