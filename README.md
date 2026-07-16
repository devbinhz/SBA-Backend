# SBA-Backend

BookVerse backend foundation (Java 21 / Spring Boot / Maven).

## Quick start

```powershell
docker compose up -d
.\mvnw.cmd test
```

## Local database modes

On a new machine, run the reset profile once to create and seed the demo dataset. Normal development then keeps existing users, orders, payments, and inventory logs:

```powershell
$env:SPRING_PROFILES_ACTIVE="local"
mvn spring-boot:run
```

```bash
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run
```

Use the reset profile only when the team intentionally wants a clean, deterministic demo dataset:

```powershell
$env:SPRING_PROFILES_ACTIVE="local,reset"
mvn spring-boot:run
```

```bash
SPRING_PROFILES_ACTIVE=local,reset mvn spring-boot:run
```

The reset profile recreates the PostgreSQL schema and runs every SQL seed file. Stop any backend already using port `8080` before starting either mode.

The deterministic reset dataset gives every teammate the same demo state:

- `admin@bookverse.local` / `ChangeMe123!` (reset-only admin)
- `customer2@gmail.com` / `ChangeMe123!` (customer with a saved address)
- all six order states, including pending and cancelled payments
- self and gift delivery, paid-order discount, and payment records
- unused, used, and expired vouchers
- published and hidden reviews with moderation history

These credentials and records are only for the `local,reset` demo profile. Production must not run reset seed and must provide unique admin credentials and secrets through environment variables. Pulling seed files does not modify an existing local database; each teammate must intentionally run the reset profile to load this shared demo state.

After reset, verify the shared dataset with:

```bash
docker compose exec -T postgres psql -U bookverse -d bookverse \
  -f /dev/stdin < scripts/verify-demo-seed.sql
```

Existing databases created before guest checkout support need this one-time compatibility migration:

```bash
docker compose exec -T postgres psql -U bookverse -d bookverse \
  -f /dev/stdin < scripts/migrations/20260715_guest_checkout.sql
```

## Included Phase 1 scaffold

- Spring Boot application bootstrap
- PostgreSQL + Redis Docker Compose
- Profile-based local schema creation and deterministic demo seed
- API response wrappers and global exception handling
- JPA auditing, ModelMapper strict mode, OpenAPI
- Admin seeder and architecture test
