# HelpDesk API

Quarkus REST API for a two-sided help desk — mobile users and browser-based operators.

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

**Prerequisites:** Docker, Docker Compose

### 1. Clone the repo

Same as local dev.

### 2. Generate JWT keys

Generate a fresh key pair next to `docker-compose.yml` — never reuse dev keys in production:

```bash
openssl genrsa -out privateKey.pem 2048
openssl rsa -in privateKey.pem -pubout -out publicKey.pem
```

`docker-compose.yml` bind-mounts these files into the container at `/app/privateKey.pem` and `/app/publicKey.pem`. They are excluded from the Docker image via `.dockerignore` — they never get baked in.

### 3. Set up .env

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

### 4. Set up the database (one-time)

Run [`create.sql`](create.sql) in the Supabase SQL editor to create the schema.

### 5. Build and run

```bash
docker-compose up -d
```

This builds the image from source and starts the container. The app will be available on port 8080.

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
