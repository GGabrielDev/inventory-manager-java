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
- Audit logs: `/audit-logs/{entityName}/{id}`

## Inventory operation policy

- Direct item mutations (`POST/PUT/DELETE /items`) require the matching item permissions (`create_item`, `edit_item`, `delete_item`); the seeded admin role happens to carry them.
- `GET /auth/validate` and `GET /auth/me` require JWT authentication, and operators should mutate inventory through **item request forms** and lifecycle actions.
