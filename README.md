# SBA-Backend

BookVerse backend — a full-featured e-commerce platform for books with AI-powered search and recommendations.

**Stack:** Java 21 · Spring Boot 3.4.6 · Maven · PostgreSQL · Redis · MinIO · JWT · OpenAPI/Swagger

## Quick start

```bash
docker compose up -d
./mvnw test
```

API docs available at `http://localhost:8080/swagger-ui.html` after startup.

## MinIO setup (required before first run)

`docker compose up -d` starts MinIO, but the backend does **not** create buckets on its own — `MinioConfig` only builds a client and expects `bookverse-books` / `bookverse-thumbnails` to already exist. On a new machine, after MinIO is up, run once:

```bash
python scripts/set_minio_policy.py
```

This creates the `bookverse-thumbnails` bucket (used for book covers, banners, and gift-wrap images) and sets a public-read policy on it, so `imageUrl` values returned by the API are directly loadable in a browser. Requires Python with the `minio` package (`pip install minio`), and reads endpoint/credentials from `.env`.

Then seed the gift-wrap pattern images (kraft, floral, checker, metallic — referenced by `db/seed/06_gift_wraps.sql`):

```powershell
./scripts/seed-gift-wrap-images.ps1
```

This script only needs Docker (no Python/uv) — it runs the official `minio/mc` image against the running `bookverse-minio` container to create the bucket (if `set_minio_policy.py` wasn't run yet), set the public-read policy, and upload the four images from `assets/gift-wrap/`. Re-running it is safe (idempotent overwrite).

Book cover/PDF assets follow the same pattern via `scripts/seed-minio.ps1`, but that script requires `uv` + the `rag` Python project and local files under `rag/assets/books` (gitignored — populate it yourself; not bundled in the repo).

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

## Features

- **Authentication** — register, email OTP verification, login (JWT), token refresh, logout, forgot/reset password
- **Books** — search, filter, pagination; admin CRUD with change logs and stock adjustments
- **Categories** — public listing; admin CRUD with activate/deactivate
- **Cart** — per-user cart with guest-to-user merge
- **Orders & Checkout** — authenticated and guest checkout, order status lifecycle, idempotent order creation
- **Payments** — VNPAY integration with webhook handling
- **Reviews** — per-book ratings and comments; admin moderation with history
- **Vouchers** — discount codes (percentage or fixed); admin management
- **Gift Wrap** — catalog of wrap-paper patterns with per-pattern image and fee; customer picks one at checkout, admin manages the catalog
- **Addresses** — multiple delivery addresses with default selection
- **AI Chat & Recommendations** — conversational book search using RAG; per-user chat sessions
- **RAG Management** — admin ingest/delete book content into vector store; bulk operations
- **File Uploads** — book content (PDF/EPUB) and cover images via MinIO
- **Statistics** — admin business overview (revenue, orders, users, top books)
- **AI Usage Monitoring** — per-user and daily AI usage logs for admins

## API overview

Base URL: `/api/v1`

| Group | Prefix | Public | Auth |
|-------|--------|--------|------|
| Auth | `/auth` | all endpoints | — |
| Users | `/users` | — | `CUSTOMER`, `ADMIN` |
| Books | `/books` | GET list & detail | `ADMIN` for write |
| Categories | `/categories` | GET list | `ADMIN` for write |
| Cart | `/cart` | — | `CUSTOMER` |
| Orders | `/orders` | guest preview & checkout | `CUSTOMER`, `ADMIN` |
| Payments | `/payments` | VNPAY webhook | — |
| Reviews | `/books/{id}/reviews` | GET list & summary | `CUSTOMER` for write |
| Vouchers | `/vouchers` | — | `CUSTOMER`, `ADMIN` |
| Gift Wraps | `/gift-wraps` | GET list | `ADMIN` for write |
| AI Chat | `/ai` | — | `CUSTOMER`, `ADMIN` |
| Admin AI Usage | `/admin/ai/usage` | — | `ADMIN` |
| Admin RAG | `/admin/rag` | — | `ADMIN` |
| Admin Uploads | `/admin/uploads` | — | `ADMIN` |
| Statistics | `/statistics` | — | `ADMIN` |
| Stock Movements | `/stock-movements` | — | `ADMIN` |

### Auth endpoints (`/api/v1/auth`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/register` | Register new customer account |
| POST | `/verify-email` | Verify email with OTP |
| POST | `/resend-verification` | Resend verification OTP |
| POST | `/login` | Authenticate and receive JWT token pair |
| POST | `/refresh` | Exchange refresh token for new token pair |
| POST | `/logout` | Revoke refresh token |
| POST | `/forgot-password` | Request password reset OTP |
| POST | `/reset-password` | Reset password using OTP |

**Login response:**
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "abc123...",
  "tokenType": "Bearer",
  "accessExpiresIn": 86400000,
  "refreshExpiresIn": 2592000000,
  "user": { "id", "email", "fullName", "role", "enabled", "emailVerified" }
}
```

Include the access token in subsequent requests:
```
Authorization: Bearer <accessToken>
```

## Configuration

Key settings in `application.yml` (override via environment variables or `application-local.yml`):

| Property | Default | Description |
|----------|---------|-------------|
| `bookverse.security.jwt-secret` | `${JWT_SECRET}` | JWT signing secret (min 32 bytes) |
| `bookverse.security.jwt.access-expiration-ms` | `86400000` | Access token TTL (24 h) |
| `bookverse.security.refresh-token.expiration-ms` | `2592000000` | Refresh token TTL (30 days) |
| `bookverse.cors.allowed-origins` | `http://localhost:5173` | Comma-separated CORS origins |
| `bookverse.otp.expiration-ms` | `600000` | OTP validity window (10 min) |
| `bookverse.order.shipping-fee-vnd` | `30000` | Fixed shipping fee (VND) |
| `bookverse.order.expiration-minutes` | `15` | Pending order auto-cancel timeout |
| `bookverse.rag.base-url` | `http://localhost:8000` | RAG service URL |
| `bookverse.minio.endpoint` | `http://localhost:9000` | MinIO endpoint |
| `bookverse.vnpay.payment-url` | VNPAY sandbox URL | VNPAY payment gateway |

## RAG service

The AI chat and recommendation features require the local RAG service. See [`rag/README.md`](rag/README.md) for setup instructions (FastAPI + Qdrant + MongoDB).

## Infrastructure (Docker Compose)

| Service | Port | Purpose |
|---------|------|---------|
| PostgreSQL | 5432 | Primary database |
| Redis | 6379 | OTP storage, caching |
| MinIO | 9000 / 9001 | File storage (book files, covers) |
| Qdrant | 6333 | Vector store for RAG |
| MongoDB | 27017 | RAG chunk/metadata store |
| Mongo Express | 8081 | MongoDB UI |
