# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Start the database (required before running the app)
docker-compose up -d

# Run in dev mode (live reload, Swagger UI at http://localhost:8080/q/swagger-ui)
./mvnw quarkus:dev

# Run all tests (Testcontainers provisions its own PostgreSQL — no docker-compose needed)
./mvnw test

# Run a single test class
./mvnw test -Dtest=AuthResourceTest

# Run a single test method
./mvnw test -Dtest=OperatorFlowTest#testTakeConversation

# Build without tests
./mvnw package -DskipTests
```

## Architecture

**Two-role JWT API**: `USER` (mobile) and `OPERATOR` (browser). Tokens are issued by `/auth/login/user` and `/auth/login/operator` and carry either a `userId` or `operatorId` numeric claim alongside the `groups` claim used by `@RolesAllowed`.

**All business logic lives in `ConversationService`**, which handles both the user-facing and operator-facing conversation operations. The two resource classes (`ConversationResource`, `OperatorConversationResource`) are thin wrappers that extract the caller's ID from the JWT and delegate.

**JWT claim extraction gotcha**: `jwt.getClaim("userId")` returns a `jakarta.json.JsonNumber`, not a `Long`. Always call `.longValue()` on it.

**SSE via in-memory broadcaster**: `MessageBroadcaster` holds a `Map<conversationId, List<MultiEmitter>>`. When `ConversationService` persists a message it calls `broadcaster.broadcast()`. SSE stream endpoints must be annotated `@Blocking` because they perform a DB ownership check before registering the emitter — without `@Blocking` this throws on the Vert.x IO thread.

**Error handling**: All 4xx/5xx responses use `ErrorResponse` (`error`, `message`, `timestamp`). Throw `ConversationNotFoundException`, `InvalidStateException`, or `UnauthorizedAccessException` from service code; `GlobalExceptionMapper` converts them to 404/409/403 respectively. `InvalidStateException` carries an `errorCode` string that becomes the `error` field.

**Database**: PostgreSQL on port **5433** (Docker Compose maps `5433:5432` because the host already runs PostgreSQL on 5432). The default port in `application.properties` reflects this. Tests use Quarkus Dev Services (auto-provisioned Testcontainers postgres:18) and ignore the docker-compose instance entirely.

**Seed data**: `import.sql` seeds two users (`janez`, `ana`) and two operators (`operator1`, `operator2`), all with password `password123`. Hashes must use the `$2a$` bcrypt prefix — Wildfly Elytron's `BcryptUtil` does not accept `$2b$`.

**CORS config**: The key is `quarkus.http.cors.enabled=true` (not `quarkus.http.cors=true`, which is unrecognised in this Quarkus version).

## Testing patterns

`TestUtils` provides `loginUser()` and `loginOperator()` helpers that return a Bearer token string. Use `given().header("Authorization", "Bearer " + token)` in RestAssured specs.

POST endpoints with no body (e.g. `/take`, `/close`) still require `.contentType(ContentType.JSON)` in tests because the resource classes carry a class-level `@Consumes(APPLICATION_JSON)`.

SSE endpoints cannot be tested with a normal RestAssured `get()` (it blocks forever). Open a raw `HttpURLConnection` with a short `ReadTimeout` and catch `SocketTimeoutException` as a success signal.
