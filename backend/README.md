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

- username: `admin`
- password: `admin`

## API base

`http://localhost:4000/api`

## Main routes

- `POST /auth/login`
- `GET /auth/validate`
- `GET /auth/me`
- CRUD: `/users`, `/roles`, `/permissions`, `/departments`, `/categories`, `/items`, `/states`, `/municipalities`, `/parishes`
- Changelogs: `/changelogs/{entity}/{id}`
