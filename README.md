# HelpDesk API

Quarkus REST API for a two-sided help desk — mobile users and browser-based operators.

## Live Demo

A production instance is hosted on Hetzner and available for testing:

- **API:** http://178.104.200.66/api/
- **Web Client:** http://178.104.200.66

The web client provides a complete interface for interacting with all API endpoints. Source code: [helpdesk-vite-client](https://github.com/maticsuc/helpdesk-vite-client)

## Local Development

**Prerequisites:** Java 21, Quarkus CLI, Docker

### 1. Clone the repo

```bash
git clone https://github.com/maticsuc/helpdesk-quarkus-api.git
cd helpdesk-quarkus-api
```

### 2. Generate JWT keys

JWT authentication uses RSA keys stored in `src/main/resources`. Generate them with:

```bash
openssl genrsa -out src/main/resources/privateKey.pem 2048
openssl rsa -in src/main/resources/privateKey.pem -pubout -out src/main/resources/publicKey.pem
```

The app uses these files directly from the classpath in dev. No env vars needed.

### 3. Run the app

```bash
quarkus dev
```

Quarkus Dev Services automatically starts a PostgreSQL container (Docker must be running). No manual database setup needed.

Swagger UI: http://localhost:8080/q/swagger-ui

Live reload is enabled — changes are picked up automatically without restarting.

Hibernate drops and recreates the schema on every startup (`drop-and-create`) and seeds accounts from `import.sql`. See [Seeded accounts](#seeded-accounts).

## Deployment

Deploy with Docker Compose on a server or cloud VM. Use Supabase for the managed PostgreSQL database in production.

**Prerequisites:** Docker, Docker Compose, Supabase

### 1. Clone the repo

```bash
git clone https://github.com/maticsuc/helpdesk-quarkus-api.git
cd helpdesk-quarkus-api
```

### 2. Generate JWT keys

Generate a fresh key pair next to `docker-compose.yml` — never reuse dev keys in production:

```bash
openssl genrsa -out privateKey.pem 2048
openssl rsa -in privateKey.pem -pubout -out publicKey.pem
```

`docker-compose.yml` bind-mounts these files into the container at `/app/privateKey.pem` and `/app/publicKey.pem`. They are excluded from the Docker image via `.dockerignore` — they never get baked in.

### 3. Setup Database (one-time)

Run [`create.sql`](create.sql) in the Supabase SQL editor to create the schema.

### 4. Setup Environment Variables

```bash
cp .env.example .env
```

Fill in the Supabase connection details:

```
DB_HOST=aws-0-<region>.pooler.supabase.com
DB_PORT=5432
DB_NAME=postgres
DB_USER=postgres.<project-ref>
DB_PASSWORD=<password>
```

### 5. Build and run

```bash
docker-compose up -d
```

This builds the image from source and starts the container. The app will be available on port 8080.

## Tests

```bash
./mvnw test
```

30 tests covering authentication, conversation flows, and SSE streaming. No database setup needed — Testcontainers auto-provisions PostgreSQL.

**See [src/test/TESTS.md](src/test/TESTS.md) for all commands and test details.**

## Seeded accounts

Password for all: `password123`

| Role     | Username    |
|----------|-------------|
| User     | `janez`, `ana` |
| Operator | `operator1`, `operator2` |

## Bruno API Collection

19 requests covering all endpoints. Open `bruno/`, select `local` or `production` environment, and click "Run Collection" to test the full helpdesk workflow (PENDING → ACTIVE → CLOSED).
