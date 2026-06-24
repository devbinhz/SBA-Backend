# SBA-Backend

BookVerse backend foundation (Java 21 / Spring Boot / Maven).

## Quick start

```powershell
docker compose up -d
.\mvnw.cmd test
```

## Included Phase 1 scaffold

- Spring Boot application bootstrap
- PostgreSQL + Redis Docker Compose
- Flyway initial schema migration
- API response wrappers and global exception handling
- JPA auditing, ModelMapper strict mode, OpenAPI
- Admin seeder and architecture test

