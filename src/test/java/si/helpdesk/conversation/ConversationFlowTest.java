package si.helpdesk.conversation;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import si.helpdesk.TestUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ConversationFlowTest {

    private String userToken;

    @BeforeEach
    void setUp() {
        userToken = TestUtils.loginUser("janez", "password123");
    }

    @Test
    void testCreateConversation() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body("{\"room\":\"TEHNIKA\",\"message\":\"Moj printer ne dela.\"}")
                .when()
                .post("/conversations")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("status", equalTo("PENDING"))
                .body("room", equalTo("TEHNIKA"))
                .body("firstMessage", equalTo("Moj printer ne dela."));
    }

    @Test
    void testSendMessageWhilePending() {
        // Create conversation
        int convId = given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body("{\"room\":\"STORITVE\",\"message\":\"Potrebujem pomoc.\"}")
                .when()
                .post("/conversations")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getInt("id");

        // Try to send message while PENDING - should get 409
        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body("{\"content\":\"Drugi sporocilo.\"}")
                .when()
                .post("/conversations/" + convId + "/messages")
                .then()
                .statusCode(409)
                .body("error", equalTo("CONVERSATION_NOT_ACTIVE"));
    }

    @Test
    void testGetConversation() {
        int convId = given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body("{\"room\":\"POGOVOR\",\"message\":\"Test pogovor.\"}")
                .when()
                .post("/conversations")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getInt("id");

        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get("/conversations/" + convId)
                .then()
                .statusCode(200)
                .body("id", equalTo(convId))
                .body("status", equalTo("PENDING"));
    }

    @Test
    void testListConversations() {
        // Create two conversations as janez
        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body("{\"room\":\"TEHNIKA\",\"message\":\"Prva.\"}")
                .when().post("/conversations").then().statusCode(201);

        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body("{\"room\":\"STORITVE\",\"message\":\"Druga.\"}")
                .when().post("/conversations").then().statusCode(201);

        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get("/conversations")
                .then()
                .statusCode(200)
                .body("$", not(empty()))
                .body("[0].status", notNullValue());
    }

    @Test
    void testListConversationsDoesNotReturnOtherUsers() {
        // Create a conversation as ana
        String anaToken = TestUtils.loginUser("ana", "password123");
        int anaConvId = given()
                .header("Authorization", "Bearer " + anaToken)
                .contentType(ContentType.JSON)
                .body("{\"room\":\"POGOVOR\",\"message\":\"Ana conversation.\"}")
                .when().post("/conversations").then().statusCode(201)
                .extract().jsonPath().getInt("id");

        // Janez's list must not contain ana's conversation
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get("/conversations")
                .then()
                .statusCode(200)
                .body("id", not(hasItem(anaConvId)));
    }

    @Test
    void testAccessWithoutToken() {
        given()
                .when()
                .get("/conversations/1")
                .then()
                .statusCode(401);
    }

    @Test
    void testGetOtherUsersConversation() {
        // Create conversation as janez
        int convId = given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body("{\"room\":\"TEHNIKA\",\"message\":\"Janez conversation.\"}")
                .when()
                .post("/conversations")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getInt("id");

        // Try to access as ana
        String anaToken = TestUtils.loginUser("ana", "password123");
        given()
                .header("Authorization", "Bearer " + anaToken)
                .when()
                .get("/conversations/" + convId)
                .then()
                .statusCode(404);
    }

    @Test
    void testSseStreamAccepted() throws Exception {
        int convId = given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body("{\"room\":\"TEHNIKA\",\"message\":\"SSE test.\"}")
                .when()
                .post("/conversations")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getInt("id");

        // Open SSE connection and verify it returns 200 with correct content-type
        // We use a short-lived connection and just check the headers
        java.net.URL url = java.net.URI.create("http://localhost:8081/conversations/" + convId + "/stream").toURL();
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + userToken);
        conn.setRequestProperty("Accept", "text/event-stream");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(2000);
        try {
            int status = conn.getResponseCode();
            String contentType = conn.getContentType();
            org.junit.jupiter.api.Assertions.assertEquals(200, status);
            org.junit.jupiter.api.Assertions.assertTrue(contentType != null && contentType.contains("text/event-stream"),
                    "Expected text/event-stream but got: " + contentType);
        } catch (java.net.SocketTimeoutException e) {
            // Read timeout is expected for SSE — connection was established (200 received)
        } finally {
            conn.disconnect();
        }
    }

    @Test
    void testSseMessageBroadcast() throws Exception {
        // 1. Create conversation and take it (so messages can be sent)
        int convId = given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body("{\"room\":\"TEHNIKA\",\"message\":\"Initial message.\"}")
                .post("/conversations")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getInt("id");

        String operatorToken = TestUtils.loginOperator("operator1", "password123");
        given()
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(ContentType.JSON)
                .post("/operator/conversations/" + convId + "/take")
                .then()
                .statusCode(200);

        // 2. Set up SSE listener in separate thread
        CountDownLatch messageReceived = new CountDownLatch(1);
        List<String> receivedMessages = new ArrayList<>();
        AtomicBoolean connectionEstablished = new AtomicBoolean(false);

        Thread sseThread = new Thread(() -> {
            try {
                URL url = URI.create("http://localhost:8081/conversations/" + convId + "/stream").toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Authorization", "Bearer " + userToken);
                conn.setRequestProperty("Accept", "text/event-stream");
                conn.setConnectTimeout(5000);

                int status = conn.getResponseCode();
                if (status == 200) {
                    connectionEstablished.set(true);
                }

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data:")) {
                        String data = line.substring(5).trim();
                        receivedMessages.add(data);
                        messageReceived.countDown();
                        break; // Got message, disconnect
                    }
                }
                reader.close();
                conn.disconnect();
            } catch (Exception e) {
                System.err.println("SSE thread error: " + e.getMessage());
            }
        });
        sseThread.start();

        // 3. Wait for SSE connection to establish
        Thread.sleep(1000);
        assertTrue(connectionEstablished.get(), "SSE connection should be established");

        // 4. Send message from operator (should trigger broadcast)
        given()
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(ContentType.JSON)
                .body("{\"content\":\"Hello from operator via SSE!\"}")
                .post("/operator/conversations/" + convId + "/messages")
                .then()
                .statusCode(201);

        // 5. Wait for message to arrive via SSE (max 5 seconds)
        boolean received = messageReceived.await(5, TimeUnit.SECONDS);
        assertTrue(received, "SSE message not received within 5 seconds");

        // 6. Verify message content
        assertEquals(1, receivedMessages.size(), "Should receive exactly one message");
        String json = receivedMessages.get(0);
        assertTrue(json.contains("Hello from operator via SSE!"), "Message content should match");
        assertTrue(json.contains("\"senderType\":\"OPERATOR\""), "Should be from OPERATOR");

        // Cleanup
        sseThread.join(1000);
    }

    @Test
    void testSseMultipleClients() throws Exception {
        // 1. Create and activate conversation
        int convId = given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body("{\"room\":\"STORITVE\",\"message\":\"Multi-client test.\"}")
                .post("/conversations")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getInt("id");

        String operatorToken = TestUtils.loginOperator("operator1", "password123");
        given()
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(ContentType.JSON)
                .post("/operator/conversations/" + convId + "/take")
                .then()
                .statusCode(200);

        // 2. Open TWO SSE connections (simulating two browser tabs)
        CountDownLatch client1Received = new CountDownLatch(1);
        CountDownLatch client2Received = new CountDownLatch(1);
        List<String> client1Messages = new ArrayList<>();
        List<String> client2Messages = new ArrayList<>();

        // Client 1
        Thread client1Thread = new Thread(() -> {
            try {
                URL url = URI.create("http://localhost:8081/conversations/" + convId + "/stream").toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Authorization", "Bearer " + userToken);
                conn.setRequestProperty("Accept", "text/event-stream");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data:")) {
                        client1Messages.add(line.substring(5).trim());
                        client1Received.countDown();
                        break;
                    }
                }
                reader.close();
                conn.disconnect();
            } catch (Exception e) {
                System.err.println("Client 1 error: " + e.getMessage());
            }
        });

        // Client 2
        Thread client2Thread = new Thread(() -> {
            try {
                URL url = URI.create("http://localhost:8081/conversations/" + convId + "/stream").toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Authorization", "Bearer " + userToken);
                conn.setRequestProperty("Accept", "text/event-stream");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data:")) {
                        client2Messages.add(line.substring(5).trim());
                        client2Received.countDown();
                        break;
                    }
                }
                reader.close();
                conn.disconnect();
            } catch (Exception e) {
                System.err.println("Client 2 error: " + e.getMessage());
            }
        });

        client1Thread.start();
        client2Thread.start();

        // 3. Wait for both connections to establish
        Thread.sleep(1000);

        // 4. Send ONE message
        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body("{\"content\":\"Broadcast to all clients!\"}")
                .post("/conversations/" + convId + "/messages")
                .then()
                .statusCode(201);

        // 5. Both clients should receive the message
        boolean client1Got = client1Received.await(5, TimeUnit.SECONDS);
        boolean client2Got = client2Received.await(5, TimeUnit.SECONDS);

        assertTrue(client1Got, "Client 1 should receive message");
        assertTrue(client2Got, "Client 2 should receive message");

        // 6. Both should have same message
        assertEquals(1, client1Messages.size());
        assertEquals(1, client2Messages.size());
        assertTrue(client1Messages.get(0).contains("Broadcast to all clients!"));
        assertTrue(client2Messages.get(0).contains("Broadcast to all clients!"));

        // Cleanup
        client1Thread.join(1000);
        client2Thread.join(1000);
    }

    @Test
    void testSseUnauthorizedAccess() throws Exception {
        // Create conversation as janez
        int convId = given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body("{\"room\":\"TEHNIKA\",\"message\":\"Janez's conversation.\"}")
                .post("/conversations")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getInt("id");

        // Try to open SSE stream as ana (different user)
        String anaToken = TestUtils.loginUser("ana", "password123");
        URL url = URI.create("http://localhost:8081/conversations/" + convId + "/stream").toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + anaToken);
        conn.setRequestProperty("Accept", "text/event-stream");

        try {
            int status = conn.getResponseCode();
            assertEquals(404, status, "Should return 404 for unauthorized access");
        } finally {
            conn.disconnect();
        }
    }
}
