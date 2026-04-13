# HelpDesk API

Quarkus REST API for a two-sided help desk — mobile users and browser-based operators.

## Setup

**Prerequisites:** Java 21, Maven, Docker

```bash
# 1. Start PostgreSQL
docker-compose up -d

# 2. Generate JWT keys (one-time)
openssl genrsa -out src/main/resources/privateKey.pem 2048
openssl rsa -in src/main/resources/privateKey.pem -pubout -out src/main/resources/publicKey.pem

# 3. Run in dev mode
./mvnw quarkus:dev
```

API: `http://localhost:8080` — Swagger UI: `http://localhost:8080/q/swagger-ui`

## Tests

```bash
./mvnw test
```

No database setup needed — tests provision their own PostgreSQL via Testcontainers.

## Seeded accounts

Password for all: `password123`

| Role     | Username    |
|----------|-------------|
| User     | `janez`, `ana` |
| Operator | `operator1`, `operator2` |

## Bruno collection

Open the `bruno/` folder in [Bruno](https://www.usebruno.com/), select the **local** environment, and run **Login User** or **Login Operator** first to populate the `token` variable.
