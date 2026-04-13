# HelpDesk API

Quarkus REST API for a two-sided help desk — mobile users and browser-based operators.

## Setup

**Prerequisites:** Java 21, Quarkus CLI, Docker

```bash
# 1. Generate JWT keys (one-time)
openssl genrsa -out src/main/resources/privateKey.pem 2048
openssl rsa -in src/main/resources/privateKey.pem -pubout -out src/main/resources/publicKey.pem

# 2. Copy the env template
cp .env.example .env
```

### Database

#### Local (Docker)

```bash
# Start PostgreSQL
docker-compose up -d

# Run (live reload, Swagger UI at http://localhost:8080/q/swagger-ui)
quarkus dev
```

Hibernate creates the schema and seeds accounts automatically on every startup (`drop-and-create`).

#### Supabase

1. Schema creation: Run the [`create.sql`](create.sql) file in the Supabase SQL editor to create the necessary tables and seed accounts.
2. Connection string: **Project Settings → Database → Connection string → Session pooler**
3. Update the `.env` file with the connection details:

```
DB_HOST=aws-0-<region>.pooler.supabase.com
DB_PORT=5432
DB_NAME=postgres
DB_USER=postgres.<project-ref>
DB_PASSWORD=<password>
```

5. Run:

```bash
quarkus dev -Dquarkus.profile=supabase
```

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
