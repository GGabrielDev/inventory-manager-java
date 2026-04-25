# Backend (Spring Boot)

## Stack

- Spring Boot 3 (Web, Security, Validation, JPA)
- PostgreSQL + Flyway
- JWT authentication
- RBAC permission model (`<action>_<entity>`)
- JaVers SQL audit logging

## Run

```bash
mvn spring-boot:run
```

## Default seeded admin

- username: `admin` (overridable via `ADMIN_USERNAME`)
- password: `admin_password` (overridable via `ADMIN_PASSWORD`)

## API base

`http://localhost:4000/api`

## Main routes

- `POST /auth/login`
- `GET /auth/validate`
- `GET /auth/me`
- CRUD: `/users`, `/roles`, `/permissions`, `/departments`, `/categories`, `/items`, `/states`, `/municipalities`, `/parishes`
- Request workflow: `/item-requests`, `/item-requests/{id}/submit`, `/item-requests/{id}/review`, `/item-requests/{id}/execute`
- Changelogs: `/changelogs/{entity}/{id}`

## Inventory operation policy

- Direct item mutations (`POST/PUT/DELETE /items`) are restricted to **admin role**.
- Operators should mutate inventory through **item request forms** and lifecycle actions.
