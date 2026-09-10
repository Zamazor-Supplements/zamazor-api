<div align="center">

# Zamazor API

**Spring Boot backend for the Zamazor supplement & wellness store.**

A stateless JWT-secured REST API covering authentication, product catalog, carts, wishlists,
addresses, orders, Stripe payments, and an admin dashboard with Server-Sent Events —
backed by PostgreSQL with Flyway-managed migrations.

![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white&style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.6-6DB33F?logo=springboot&logoColor=white&style=flat-square)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Flyway-4169E1?logo=postgresql&logoColor=white&style=flat-square)
![Maven](https://img.shields.io/badge/Maven-3.9.12-C71A36?logo=apachemaven&logoColor=white&style=flat-square)
![Flyway](https://img.shields.io/badge/Flyway-migrations-red?style=flat-square)
![Stripe](https://img.shields.io/badge/Stripe-Checkout-635BFF?logo=stripe&logoColor=white&style=flat-square)

</div>

---

## Table of Contents

- [Overview](#overview)
- [Key Features & Modules](#key-features--modules)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Getting Started & Configuration](#getting-started--configuration)
- [Running the Application](#running-the-application)
- [Database & Migrations](#database--migrations)
- [API Documentation](#api-documentation)
- [Security Model](#security-model)
- [Project Structure](#project-structure)
- [Testing](#testing)
- [Docker](#docker)

---

## Overview

`zamazor-api` (Maven artifact `com.zamazor:market`) is the server behind the Zamazor
storefront. It exposes a JSON REST API consumed by the React SPA, and owns everything the
frontend cannot: identity, pricing, inventory, payment orchestration, and order lifecycle.

Architectural highlights:

- **Stateless by design** — no HTTP sessions. A short-lived JWT access token (15 min) travels
  in the `Authorization` header; a long-lived refresh token (7 days) lives in an HttpOnly cookie.
- **Schema owned by Flyway** — Hibernate runs with `ddl-auto: validate`, so the database schema
  is never generated from entities. Migrations are the single source of truth.
- **Modular monolith** — business capabilities live under `modules/*`, each self-contained with
  its own controller, service, repository, entity, DTO, and mapper layers. Cross-cutting
  concerns (security, mail, media, payments, pricing) sit in top-level packages.
- **Ports & adapters for external services** — Stripe and Cloudinary are reached through
  interfaces (`PaymentGateway`, `MediaStoragePort`), keeping vendor SDKs out of business logic.
- **Event-driven side effects** — emails and dashboard notifications are published as Spring
  application events and handled by `@TransactionalEventListener(AFTER_COMMIT)`, so nothing
  sends mail for a transaction that later rolls back.

## Key Features & Modules

### Authentication & Identity (`modules/auth`, `security`)

- Register, login, logout, and silent token refresh.
- **Email verification** — token issuance and confirmation, gating access to checkout.
- **Password reset** — request/confirm flow backed by a `password_reset_tokens` table.
- JWT signing with separate access and refresh secrets; issuer `auth-service`.
- Refresh token delivered as an HttpOnly cookie and cleared on logout.

### Catalog (`modules/product`)

- Product CRUD (admin-gated writes, public reads).
- Lookup by ID, by category, and full-text search.
- **Bulk hydration endpoint** — `POST /products/bulk` resolves a list of IDs into full products,
  used by the client to rehydrate locally-stored carts and wishlists.
- JPA Specifications for dynamic filtering, sorting, and pagination.
- Soft deletes via a `deleted_at` column.

### Categories (`modules/product`)

- Category CRUD with admin-gated writes and public reads.

### Cart & Wishlist (`modules/catalog`, `modules/wishlist`)

- Server-side carts with add / update-quantity / remove-item / clear.
- Wishlists with add / remove / clear.
- **Guest merge endpoints** — `POST /carts/sync` and `POST /wishlists/sync` fold a guest's
  locally-stored items into the authenticated user's server-side collection at login.
- Uniqueness constraints prevent duplicate cart items, wishlist entries, and order items.

### Addresses (`modules/catalog`)

- Create, list, and update shipping addresses, linkable to a user profile.

### Orders & Checkout (`modules/catalog`, `modules/billing`)

- Multi-step checkout: order creation → payment link generation → payment verification.
- **Order state machine** (`orders/domain/OrderStateMachine`) rejecting invalid status
  transitions with `InvalidStateTransitionException`.
- Customer cancellation and admin-driven status updates.
- Order history for the authenticated user, plus admin-wide listing.
- Configurable order policy: **48 h** cancellation window, **30 day** refund window,
  **30 min** payment hold.
- Scheduled expiration of unpaid orders (`OrderExpirationScheduler`, `@EnableScheduling`).
- Refund records and receipt generation.

### Payments (`payment`, Stripe)

- Stripe Checkout session creation and redirect-based payment.
- **Signed webhook ingestion** at `POST /webhooks/stripe` with signature verification.
- **Idempotent webhook processing** — every received event is persisted to
  `stripe_webhook_events` (uniquely constrained) before handling, so Stripe retries are safe.
- Typed handlers per event family: `checkout.session.completed`, `payment_intent` terminal
  states, `charge.refunded`, and refund events.

### Admin Dashboard (`modules/dashboard`)

- Aggregated overview metrics, category breakdown, and product performance.
- **Server-Sent Events** at `GET /dashboard/events` (`text/event-stream`) pushing realtime
  invalidation signals to open admin sessions.
- Entire `/dashboard/**` surface restricted to the `ADMIN` role.

### Users (`modules/user`)

- Fetch a user by ID, authorized via a custom security expression allowing self or admin.

### Transactional Email (`mail`)

- Gmail SMTP (STARTTLS, port 587) with async dispatch (`@EnableAsync`).
- Eight Thymeleaf templates: registration, email verification, password reset, password
  changed, checkout success, order receipt, order canceled, order refunded.
- Sent only after commit via `@TransactionalEventListener`.

### Media (`media`, Cloudinary)

- Image upload and storage behind a `MediaStoragePort`, implemented by a Cloudinary adapter.

### Pricing (`pricing`)

- Domain-level order total calculation with a dedicated `TaxCalculator`.

### Platform

- **Bean Validation** on all inbound DTOs (`spring-boot-starter-validation`).
- **Centralized exception handling** — `ExceptionResolver` extends `ResponseEntityExceptionHandler`
  and maps domain exceptions to consistent error responses.
- **MapStruct** for entity ↔ DTO mapping (compiled, not reflective).
- **Actuator** for health monitoring.
- **CORS** locked to the configured frontend origin with credentialed requests allowed.

## Tech Stack

| Concern | Technology |
| --- | --- |
| **Language** | Java 21 |
| **Framework** | Spring Boot 4.0.6 (parent POM) |
| **Web** | `spring-boot-starter-webmvc` (Spring MVC) |
| **Persistence** | Spring Data JPA + Hibernate, PostgreSQL JDBC driver 42.7.10 |
| **Migrations** | Flyway (`spring-boot-starter-flyway` + `flyway-database-postgresql`) |
| **Security** | Spring Security 7.0.5 (`@EnableMethodSecurity`), JJWT 0.13.0 (api/impl/jackson) |
| **Validation** | Jakarta Bean Validation (`spring-boot-starter-validation`) |
| **Mapping** | MapStruct 1.6.3 (+ `lombok-mapstruct-binding` 0.2.0) |
| **Boilerplate** | Lombok — 1.18.46 on the classpath, 1.18.42 pinned as the annotation processor; excluded from the fat JAR |
| **Payments** | Stripe Java SDK 33.1.0 |
| **Media storage** | Cloudinary `cloudinary-http5` 2.4.0 |
| **Email** | `spring-boot-starter-mail` (Gmail SMTP) |
| **Templating** | Thymeleaf + `thymeleaf-extras-springsecurity6` (targets Security 6; Security 7 is on the classpath) |
| **Observability** | `spring-boot-starter-actuator` |
| **Build tool** | Maven — the wrapper pins 3.9.12 (`mvnw` / `mvnw.cmd`) |
| **Runtime** | Spring Framework 7.0.7 |
| **Container** | Multi-stage Dockerfile, Eclipse Temurin 21 JRE (Alpine), non-root user |
| **Annotations** | `org.jetbrains:annotations` 26.1.0 |

## Prerequisites

| Requirement | Version | Notes |
| --- | --- | --- |
| **JDK** | **21** | Set by `<java.version>21</java.version>`; Temurin recommended |
| **Maven** | 3.9+ | Optional — the wrapper downloads a pinned 3.9.12 |
| **PostgreSQL** | Any supported release | Required; no server version is pinned by the project. Flyway migrates the schema on startup |
| **Docker** | Any recent | Optional — for containerized runs |

Third-party accounts (needed for full functionality, not for boot):

- **Stripe** — publishable key, secret key, and a webhook signing secret.
- **Cloudinary** — cloud name, API key, API secret (product imagery).
- **Gmail / SMTP** — an app password for transactional email.

Verify your toolchain:

```bash
java -version    # must report 21
./mvnw -version  # Windows: mvnw.cmd -version
psql --version
```

## Getting Started & Configuration

### 1. Clone the repository

```bash
git clone https://github.com/Ayoubedf/zamazor-api.git
cd zamazor-api
```

### 2. Create the database

```bash
psql -U postgres -c "CREATE DATABASE zamazor;"
```

Flyway creates all tables and seed data on first boot — do **not** hand-write DDL.

### 3. Supply environment variables

All configuration is externalized. `src/main/resources/application.yaml` reads every value
from the environment with `${VAR:default}` placeholders.

| Variable | Required | Default | Purpose |
| --- | :---: | --- | --- |
| `DB_URL` | **Yes** | *(empty)* | PostgreSQL JDBC URL |
| `DB_USER` | **Yes** | *(empty)* | Database username |
| `DB_PASS` | **Yes** | *(empty)* | Database password |
| `ACCESS_TOKEN_SECRET_KEY` | **Yes** | *(empty)* | JWT signing key for access tokens |
| `REFRESH_TOKEN_SECRET_KEY` | **Yes** | *(empty)* | JWT signing key for refresh tokens |
| `SMTP_USER` | **Yes** | *(empty)* | Gmail address; also used as the mail `from` address |
| `SMTP_PASS` | **Yes** | *(empty)* | Gmail app password |
| `STRIPE_SECRET_KEY` | **Yes** | *(empty)* | Stripe API secret key |
| `STRIPE_PUBLISHABLE_KEY` | **Yes** | *(empty)* | Stripe publishable key |
| `STRIPE_WEBHOOK_SECRET` | **Yes** | *(empty)* | Verifies inbound webhook signatures |
| `CLOUDINARY_CLOUDNAME` | **Yes** | *(empty)* | Cloudinary cloud name |
| `CLOUDINARY_APIKEY` | **Yes** | *(empty)* | Cloudinary API key |
| `CLOUDINARY_APISECRET` | **Yes** | *(empty)* | Cloudinary API secret |
| `SUPPORT_EMAIL` | **Yes** | *(empty)* | Support contact surfaced in emails |
| `SUPPORT_PHONE` | **Yes** | *(empty)* | Support phone surfaced in emails |
| `FRONTEND_URL` | No | `http://localhost:5173` | Allowed CORS origin + email link base |
| `BACKEND_URL` | No | `http://localhost:8080` | Base URL used in generated links |

> Variables marked *empty* have no usable default. The context will start but datasource,
> JWT, mail, Stripe, and Cloudinary initialization will fail without them.

**JWT secrets must be strong keys.** JJWT requires at least 256 bits for HS256. Generate one:

```bash
openssl rand -base64 48
```

Use a **different** value for the access and refresh secrets.

### 4. How to actually pass them

> ⚠️ **Spring Boot does not read `.env` files.** There is no dotenv dependency in `pom.xml`, so
> a `.env` in the project root is inert unless something else loads it. Pick one of these:

**Option A — shell export (works with `./mvnw spring-boot:run`)**

```bash
export DB_URL="jdbc:postgresql://localhost:5432/zamazor"
export DB_USER="postgres"
export DB_PASS="postgres"
export ACCESS_TOKEN_SECRET_KEY="$(openssl rand -base64 48)"
export REFRESH_TOKEN_SECRET_KEY="$(openssl rand -base64 48)"
# ...remaining variables
./mvnw spring-boot:run
```

**Option B — load a `.env` into the shell first**

```bash
set -a; source .env; set +a
./mvnw spring-boot:run
```

**Option C — IntelliJ IDEA**

Add each variable under *Run/Debug Configurations → Environment variables*, or install the
**EnvFile** plugin and point it at `.env`.

**Option D — Docker `--env-file`** (see [Docker](#docker))

`.env` is gitignored. Never commit real credentials.

### 5. Non-secret configuration

These live directly in `application.yaml` and rarely need changing:

```yaml
server:
  port: 8080                    # implicit default; not set explicitly

spring:
  jpa:
    hibernate.ddl-auto: validate  # schema comes from Flyway only
    open-in-view: false           # no lazy-loading outside transactions
  flyway:
    baseline-on-migrate: true
    locations: [db/migration, db/seed]

application:
  security.jwt:
    access.expiration: 15m
    refresh.expiration: 7d
  shop.order-policy:
    cancel-window: PT48H          # ISO-8601 duration
    refund-window: P30D
    payment-hold: PT30M
```

## Running the Application

### Development

```bash
# Start with hot restart (spring-boot-devtools is on the classpath)
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

The API listens on **http://localhost:8080**.

### Build a runnable JAR

```bash
./mvnw clean package -DskipTests
java -jar target/market-0.0.1-SNAPSHOT.jar
```

### Useful Maven commands

```bash
./mvnw clean compile          # compile only
./mvnw test                   # run the test suite
./mvnw clean verify           # full build + tests
./mvnw dependency:tree        # inspect resolved dependencies
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

## Database & Migrations

Flyway owns the schema. On every startup it applies pending versioned migrations from
`db/migration`, then repeatable seed scripts from `db/seed`.

```txt
src/main/resources/db/
├── migration/    V1 … V40   # versioned DDL: tables, constraints, foreign keys
└── seed/         R__1 … R__4  # repeatable seeds: stores, categories, users, products
```

Migration history covers the `event_publication` registry, addresses, carts and cart items,
categories, orders, order items, order refunds, products, stores, Stripe webhook events, users,
wishlists, password-reset tokens, email-verification tokens, plus a series of uniqueness
constraints, foreign keys, and audit columns (`created_at`, `updated_at`, `deleted_at`,
`email_verified_at`).

Rules for contributors:

- **Never edit an applied migration.** Add a new `V<n>__description.sql` file instead.
- Hibernate is in `validate` mode — an entity that disagrees with the schema fails startup.
  That is intentional; fix the migration or the entity, not the setting.
- Seed files are prefixed `R__` (repeatable) and re-run whenever their checksum changes.

## API Documentation

> **No Swagger / OpenAPI UI is configured.** `springdoc-openapi` is not a dependency, so there
> is no `/swagger-ui.html` or `/v3/api-docs`. The tables below are generated from the
> `@RequestMapping` annotations in `src/main/java/com/zamazor/market/modules/**/controller/`
> and the authorization rules in `SecurityConfiguration`.

Base URL: `http://localhost:8080`

### Authentication — `/auth`

| Method | Endpoint | Access |
| --- | --- | --- |
| `POST` | `/auth/register` | Public |
| `POST` | `/auth/login` | Public |
| `POST` | `/auth/refresh` | Public |
| `POST` | `/auth/verify-email` | Public |
| `POST` | `/auth/password-reset/request` | Public |
| `POST` | `/auth/password-reset/confirm` | Public |
| `POST` | `/auth/send-verification` | Authenticated |
| `GET` | `/auth/me` | Authenticated |
| `POST` | `/auth/logout` | Authenticated |

### Products — `/products`

| Method | Endpoint | Access |
| --- | --- | --- |
| `GET` | `/products` | Public (paginated, filterable) |
| `GET` | `/products/{id}` | Public |
| `GET` | `/products/search` | Public |
| `GET` | `/products/category/{categoryId}` | Public |
| `POST` | `/products/bulk` | Public (bulk ID lookup) |
| `POST` | `/products` | `ADMIN` |
| `PUT` | `/products/{id}` | `ADMIN` |
| `DELETE` | `/products/{id}` | `ADMIN` |

### Categories — `/categories`

| Method | Endpoint | Access |
| --- | --- | --- |
| `GET` | `/categories` | Public |
| `POST` | `/categories` | `ADMIN` |
| `PUT` | `/categories/{id}` | `ADMIN` |
| `DELETE` | `/categories/{id}` | `ADMIN` |

### Cart — `/carts`

| Method | Endpoint | Access |
| --- | --- | --- |
| `GET` | `/carts` | Authenticated |
| `POST` | `/carts/sync` | Authenticated (merge guest cart) |
| `POST` | `/carts/items` | Authenticated |
| `PATCH` | `/carts/items/{productId}` | Authenticated |
| `DELETE` | `/carts/items/{productId}` | Authenticated |
| `DELETE` | `/carts` | Authenticated (clear) |

### Wishlist — `/wishlists`

| Method | Endpoint | Access |
| --- | --- | --- |
| `GET` | `/wishlists` | Authenticated |
| `POST` | `/wishlists/sync` | Authenticated (merge guest wishlist) |
| `POST` | `/wishlists/{productId}` | Authenticated |
| `DELETE` | `/wishlists/{productId}` | Authenticated |
| `DELETE` | `/wishlists` | Authenticated (clear) |

### Addresses — `/addresses`

| Method | Endpoint | Access |
| --- | --- | --- |
| `GET` | `/addresses` | Authenticated |
| `POST` | `/addresses` | Authenticated |
| `PUT` | `/addresses` | Authenticated |

### Orders — `/orders`

| Method | Endpoint | Access |
| --- | --- | --- |
| `GET` | `/orders` | `ADMIN` |
| `GET` | `/orders/me` | Authenticated + email verified |
| `GET` | `/orders/{orderId}` | Order owner or `ADMIN` |
| `POST` | `/orders/checkout` | Authenticated + email verified |
| `POST` | `/orders/checkout/{orderId}/pay` | Order owner or `ADMIN` |
| `GET` | `/orders/checkout/{orderId}/verify` | Order owner or `ADMIN` |
| `POST` | `/orders/{orderId}/cancel` | Order owner or `ADMIN` |
| `PATCH` | `/orders/{orderId}/status` | `ADMIN` |

### Users — `/users`

| Method | Endpoint | Access |
| --- | --- | --- |
| `GET` | `/users/{id}` | Self or `ADMIN` (`id` is a UUID) |

### Dashboard — `/dashboard`

| Method | Endpoint | Access |
| --- | --- | --- |
| `GET` | `/dashboard/overview` | `ADMIN` |
| `GET` | `/dashboard/category` | `ADMIN` |
| `GET` | `/dashboard/product` | `ADMIN` |
| `GET` | `/dashboard/events` | `ADMIN` — SSE stream (`text/event-stream`) |

### Webhooks & Ops

| Method | Endpoint | Access |
| --- | --- | --- |
| `POST` | `/webhooks/stripe` | Public (Stripe signature verified) |
| `GET` | `/actuator/health` | Authenticated — no `/actuator/**` matcher exists, so it falls through to `anyRequest().authenticated()` |

### Sample request

```bash
# Login — returns an access token and sets the refresh-token cookie
curl -i -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"you@example.com","password":"your-password"}'

# Authenticated call
curl http://localhost:8080/auth/me \
  -H "Authorization: Bearer <ACCESS_TOKEN>"

# Subscribe to admin SSE events
curl -N http://localhost:8080/dashboard/events \
  -H "Authorization: Bearer <ADMIN_ACCESS_TOKEN>"
```

> Adding OpenAPI docs? Introduce `springdoc-openapi-starter-webmvc-ui` in `pom.xml`; the UI
> then appears at `/swagger-ui.html`. Remember to permit or protect that path in
> `SecurityConfiguration`.

## Security Model

Authorization is enforced at **two layers**.

**1. Filter chain** (`security/config/SecurityConfiguration`)

- CSRF disabled (stateless token API), CORS enabled from `FRONTEND_URL` only, with credentials.
- `SessionCreationPolicy.STATELESS` — no server-side session.
- `JwtAuthenticationFilter` runs before `UsernamePasswordAuthenticationFilter`.
- A single `SecurityExceptionHandler` serves as both authentication entry point and access
  denied handler, producing uniform `401` / `403` JSON.
- Logout clears the refresh-token cookie via `CustomLogoutSuccessHandler`.

Match order (first match wins):

```txt
OPTIONS /**                     → permitAll
/webhooks/**                    → permitAll
/auth/me, /auth/send-verification → authenticated
/auth/**                        → permitAll
GET /products/**, /categories/** → permitAll
POST /products/bulk             → permitAll
/dashboard/**                   → hasRole('ADMIN')
everything else                 → authenticated
```

**2. Method security** (`@EnableMethodSecurity`)

Fine-grained rules use `@PreAuthorize` with custom Spring beans:

| Expression | Meaning |
| --- | --- |
| `hasRole('ADMIN')` | Admin-only |
| `@orderSecurity.isEmailVerified(principal)` | Blocks unverified accounts from checkout |
| `@orderSecurity.isOwnerOrAdmin(#orderId, principal)` | Row-level ownership check |
| `@userSecurity.isUserOrAdmin(#id, authentication)` | Self-or-admin profile access |

Implementations live in `modules/catalog/security/OrderSecurity` and
`modules/user/security/UserSecurity`.

**Token handling.** Access tokens are bearer-authenticated. Refresh tokens are stored in an
HttpOnly cookie and are the only way to mint a new access token. SSE clients that cannot set
headers must still authenticate — the stream is served under `/dashboard/**`, which the admin
role gate protects.

## Project Structure

```txt
market/
├── src/main/java/com/zamazor/market/
│   ├── MarketApplication.java      # @SpringBootApplication, scheduling, async, JPA auditing
│   ├── config/                     # ApplicationConfiguration, ApplicationProperties
│   │
│   ├── modules/                    # Business capabilities (modular monolith)
│   │   ├── auth/                   #   controller · service · repository · models · util
│   │   ├── billing/                #   Stripe webhook events, order payment service
│   │   ├── catalog/                #   Cart, Order, Address + security, specifications
│   │   ├── dashboard/              #   Admin aggregates, SSE controller, domain events
│   │   ├── product/                #   Products, categories, specifications
│   │   ├── user/                   #   User profile + security expression
│   │   └── wishlist/               #   Wishlist management
│   │
│   ├── security/                   # Cross-cutting security
│   │   ├── config/                 #   SecurityConfiguration, JwtProperties
│   │   ├── crypto/                 #   JwtService, JwtKeyProvider
│   │   ├── filter/                 #   JwtAuthenticationFilter
│   │   └── handler/                #   SecurityExceptionHandler, logout handler
│   │
│   ├── orders/                     # Order state machine + transition exceptions
│   ├── payment/                    # Stripe config, webhook service, event handlers, scheduler
│   ├── payments/                   # Ports & adapters: PaymentGateway, Stripe adapter  (WIP)
│   ├── pricing/                    # Pricing domain: TaxCalculator, PricingResult       (WIP)
│   ├── media/                      # MediaStoragePort + Cloudinary adapter
│   ├── mail/                       # Event listeners, template factory, sender service
│   └── shared/                     # PageResponse, DomainException, ExceptionResolver, utils
│
├── src/main/resources/
│   ├── application.yaml            # All externalized configuration
│   ├── db/migration/               # V1 … V40 Flyway DDL
│   ├── db/seed/                    # R__1 … R__4 repeatable seed data
│   └── templates/emails/           # 8 Thymeleaf email templates
│
├── src/test/java/                  # MarketApplicationTests (context load)
├── Dockerfile                      # Multi-stage build, Temurin 21 JRE, non-root
├── mvnw / mvnw.cmd / .mvn/         # Maven wrapper
└── pom.xml
```

Each module follows the same internal layout:

```txt
<module>/
├── controller/     # REST endpoints, thin — delegate to services
├── service/        # Business logic and transaction boundaries
├── repository/     # Spring Data JPA interfaces
├── models/
│   ├── entity/     # JPA entities
│   ├── dto/        # Inbound/outbound records with Bean Validation
│   └── mapper/     # MapStruct mappers
├── exception/      # Module-specific exceptions
├── security/       # @PreAuthorize helper beans
└── specification/  # JPA Specifications for dynamic queries
```

## Testing

Test starters are declared for JPA, Security, Validation, and WebMVC, but the suite currently
contains only `MarketApplicationTests` (an application-context load test).

```bash
./mvnw test
```

Treat coverage as effectively absent — verify changes against a running instance and a real
PostgreSQL database.

## Docker

The `Dockerfile` is a two-stage build: Maven compiles on Temurin 21, then the JAR runs on a
Temurin 21 **JRE** Alpine image as an unprivileged `spring` user.

```bash
# Build
docker build -t zamazor-api .

# Run — pass secrets via an env file
docker run --rm -p 8080:8080 --env-file .env zamazor-api
```

JVM defaults are container-aware:

```text
-XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
```

`DB_URL` must point at a reachable host — `localhost` inside a container is the container
itself. Use `host.docker.internal` (Docker Desktop) or a shared network.

---

<div align="center">

Built with Spring Boot 4, Java 21, PostgreSQL, and Flyway.

</div>
