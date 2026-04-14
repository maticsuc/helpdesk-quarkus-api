# Test Documentation

## Overview

**Test Framework:** Quarkus Test with RestAssured  
**Database:** Testcontainers (postgres:18) via Quarkus Dev Services  
**Isolation:** Tests run on port **8081** and use their own database — completely isolated from docker-compose  
**No setup needed:** Just run `./mvnw test`

## Running Tests

```bash
# All tests
./mvnw test

# Single test class
./mvnw test -Dtest=AuthResourceTest

# Single test method
./mvnw test -Dtest=OperatorFlowTest#testTakeConversation

# Build without tests
./mvnw package -DskipTests
```

## Test Utilities

`TestUtils.java` provides `loginUser()` and `loginOperator()` helpers that return Bearer tokens for use with RestAssured.

## Test Classes

| Test Class | Tests | Coverage |
|------------|-------|----------|
| `AuthResourceTest` | 6 | JWT authentication for users and operators |
| `ConversationFlowTest` | 14 | User conversation lifecycle, ownership, SSE streaming |
| `OperatorFlowTest` | 10 | Operator operations, access control, SSE |

**Total:** 30 tests

---

## AuthResourceTest

Tests JWT authentication for both roles.

- **`testUserLoginSuccess`** — Valid user credentials return token + expiresIn
- **`testUserLoginWrongPassword`** — Returns 401 with `INVALID_CREDENTIALS`
- **`testUserLoginUnknownUser`** — Returns 401 for non-existent user
- **`testOperatorLoginSuccess`** — Valid operator credentials return token
- **`testOperatorLoginWrongPassword`** — Returns 401 for wrong password

---

## ConversationFlowTest

Tests user-facing conversation API and SSE streaming. `@BeforeEach` logs in as `janez`.

### Conversation CRUD

- **`testCreateConversation`** — POST creates conversation with PENDING status
- **`testGetConversation`** — GET returns conversation details
- **`testListConversations`** — GET returns user's conversations
- **`testSendMessageWhilePending`** — Cannot send messages in PENDING state (409)

### Authorization & Ownership

- **`testAccessWithoutToken`** — Returns 401 when no token provided
- **`testGetOtherUsersConversation`** — Returns 404 when accessing another user's conversation
- **`testListConversationsDoesNotReturnOtherUsers`** — List only shows own conversations

### SSE Streaming

- **`testSseStreamAccepted`** — Verifies SSE connection establishes (200, `text/event-stream`)
- **`testSseMessageBroadcast`** — User receives operator's message via SSE
- **`testSseMultipleClients`** — Two SSE connections both receive the same message
- **`testSseUnauthorizedAccess`** — Cannot open SSE stream for another user's conversation (404)

**Note:** SSE tests use `HttpURLConnection` (not RestAssured) with threads + `CountDownLatch` because RestAssured blocks forever on SSE streams.

---

## OperatorFlowTest

Tests operator-facing API and full conversation lifecycle. `@BeforeEach` logs in as both `janez` (user) and `operator1`, then creates a fresh conversation.

### Operator Operations

- **`testListPendingConversations`** — Operators can query by status (`?status=PENDING`)
- **`testTakeConversation`** — Taking assigns operator and changes status to ACTIVE
- **`testTakeConversationTwice`** — Cannot take same conversation twice (409 `CONVERSATION_NOT_PENDING`)
- **`testFullConversationFlow`** — Take → Send messages (both sides) → Close
- **`testCloseNonActiveConversation`** — Cannot close PENDING conversations (409)

### Access Control

- **`testAccessOperatorEndpointWithUserToken`** — User token on `/operator/*` returns 403
- **`testAccessOperatorEndpointWithoutToken`** — No token returns 401

### SSE for Operators

- **`testOperatorSseMessageBroadcast`** — Operator receives user's message via SSE
- **`testOperatorAndUserBothReceiveSse`** — Both user and operator receive same message on their respective SSE streams

**Note:** Operators use `/operator/conversations/:id/stream`, users use `/conversations/:id/stream`. Same message is broadcast to both.

---

## Key Testing Patterns

- **POST with no body** still requires `contentType(ContentType.JSON)` due to class-level `@Consumes(APPLICATION_JSON)`
- **Status codes**: 401 (no/invalid token), 403 (wrong role), 404 (not found/not owned), 409 (invalid state)
- **SSE**: Use `HttpURLConnection` with `setReadTimeout()` and threads with `CountDownLatch`
- **Data isolation**: Each test creates its own data in `@BeforeEach` or test methods

## Seed Data

| Role     | Username | Password |
|----------|----------|----------|
| User     | `janez`, `ana` | `password123` |
| Operator | `operator1`, `operator2` | `password123` |
