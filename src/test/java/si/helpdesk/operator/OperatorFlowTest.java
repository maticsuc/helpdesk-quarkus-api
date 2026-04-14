package si.helpdesk.operator;

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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class OperatorFlowTest {

    private String userToken;
    private String operatorToken;
    private int conversationId;

    @BeforeEach
    void setUp() {
        userToken = TestUtils.loginUser("janez", "password123");
        operatorToken = TestUtils.loginOperator("operator1", "password123");

        // Create a fresh conversation for each test
        conversationId = given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body("{\"room\":\"TEHNIKA\",\"message\":\"Operator flow test message.\"}")
                .when()
                .post("/conversations")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getInt("id");
    }

    @Test
    void testListPendingConversations() {
        given()
                .header("Authorization", "Bearer " + operatorToken)
                .queryParam("status", "PENDING")
                .when()
                .get("/operator/conversations")
                .then()
                .statusCode(200)
                .body("$", not(empty()))
                .body("find { it.id == " + conversationId + " }.status", equalTo("PENDING"));
    }

    @Test
    void testTakeConversation() {
        given()
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(ContentType.JSON)
                .when()
                .post("/operator/conversations/" + conversationId + "/take")
                .then()
                .statusCode(200)
                .body("status", equalTo("ACTIVE"))
                .body("operator.username", equalTo("operator1"));
    }

    @Test
    void testTakeConversationTwice() {
        // First take
        given()
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(ContentType.JSON)
                .when()
                .post("/operator/conversations/" + conversationId + "/take")
                .then()
                .statusCode(200);

        // Second take should fail with 409
        given()
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(ContentType.JSON)
                .when()
                .post("/operator/conversations/" + conversationId + "/take")
                .then()
                .statusCode(409)
                .body("error", equalTo("CONVERSATION_NOT_PENDING"));
    }

    @Test
    void testFullConversationFlow() {
        // Take conversation
        given()
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(ContentType.JSON)
                .when()
                .post("/operator/conversations/" + conversationId + "/take")
                .then()
                .statusCode(200);

        // Operator sends message
        given()
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(ContentType.JSON)
                .body("{\"content\":\"Kako vam lahko pomagam?\"}")
                .when()
                .post("/operator/conversations/" + conversationId + "/messages")
                .then()
                .statusCode(201)
                .body("senderType", equalTo("OPERATOR"))
                .body("content", equalTo("Kako vam lahko pomagam?"));

        // User sends message
        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body("{\"content\":\"Printer ne tiska.\"}")
                .when()
                .post("/conversations/" + conversationId + "/messages")
                .then()
                .statusCode(201)
                .body("senderType", equalTo("USER"));

        // Get all messages - should have 3 (initial + operator + user reply)
        given()
                .header("Authorization", "Bearer " + operatorToken)
                .when()
                .get("/operator/conversations/" + conversationId + "/messages")
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(3)));

        // Close conversation
        given()
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(ContentType.JSON)
                .when()
                .put("/operator/conversations/" + conversationId + "/close")
                .then()
                .statusCode(200)
                .body("status", equalTo("CLOSED"));
    }

    @Test
    void testAccessOperatorEndpointWithUserToken() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get("/operator/conversations")
                .then()
                .statusCode(403);
    }

    @Test
    void testAccessOperatorEndpointWithoutToken() {
        given()
                .when()
                .get("/operator/conversations")
                .then()
                .statusCode(401);
    }

    @Test
    void testCloseNonActiveConversation() {
        // Try to close a PENDING conversation
        given()
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(ContentType.JSON)
                .when()
                .put("/operator/conversations/" + conversationId + "/close")
                .then()
                .statusCode(409);
    }

    @Test
    void testOperatorSseMessageBroadcast() throws Exception {
        // 1. Take the conversation
        given()
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(ContentType.JSON)
                .post("/operator/conversations/" + conversationId + "/take")
                .then()
                .statusCode(200);

        // 2. Open SSE stream as operator
        CountDownLatch messageReceived = new CountDownLatch(1);
        List<String> receivedMessages = new ArrayList<>();

        Thread sseThread = new Thread(() -> {
            try {
                URL url = URI.create("http://localhost:8081/operator/conversations/" + conversationId + "/stream").toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Authorization", "Bearer " + operatorToken);
                conn.setRequestProperty("Accept", "text/event-stream");

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data:")) {
                        String data = line.substring(5).trim();
                        receivedMessages.add(data);
                        messageReceived.countDown();
                        break;
                    }
                }
                reader.close();
                conn.disconnect();
            } catch (Exception e) {
                System.err.println("Operator SSE error: " + e.getMessage());
            }
        });
        sseThread.start();

        // 3. Wait for connection to establish
        Thread.sleep(1000);

        // 4. User sends message (operator should receive via SSE)
        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body("{\"content\":\"User message to operator!\"}")
                .post("/conversations/" + conversationId + "/messages")
                .then()
                .statusCode(201);

        // 5. Verify operator receives it via SSE
        boolean received = messageReceived.await(5, TimeUnit.SECONDS);
        assertTrue(received, "Operator should receive user's message via SSE");

        // 6. Verify content
        assertEquals(1, receivedMessages.size());
        String json = receivedMessages.get(0);
        assertTrue(json.contains("User message to operator!"));
        assertTrue(json.contains("\"senderType\":\"USER\""));

        // Cleanup
        sseThread.join(1000);
    }

    @Test
    void testOperatorAndUserBothReceiveSse() throws Exception {
        // 1. Take conversation
        given()
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(ContentType.JSON)
                .post("/operator/conversations/" + conversationId + "/take")
                .then()
                .statusCode(200);

        // 2. Open SSE streams for BOTH user and operator
        CountDownLatch userReceived = new CountDownLatch(1);
        CountDownLatch operatorReceived = new CountDownLatch(1);
        List<String> userMessages = new ArrayList<>();
        List<String> operatorMessages = new ArrayList<>();

        // User SSE listener
        Thread userSseThread = new Thread(() -> {
            try {
                URL url = URI.create("http://localhost:8081/conversations/" + conversationId + "/stream").toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Authorization", "Bearer " + userToken);
                conn.setRequestProperty("Accept", "text/event-stream");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data:")) {
                        userMessages.add(line.substring(5).trim());
                        userReceived.countDown();
                        break;
                    }
                }
                reader.close();
                conn.disconnect();
            } catch (Exception e) {
                System.err.println("User SSE error: " + e.getMessage());
            }
        });

        // Operator SSE listener
        Thread operatorSseThread = new Thread(() -> {
            try {
                URL url = URI.create("http://localhost:8081/operator/conversations/" + conversationId + "/stream").toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Authorization", "Bearer " + operatorToken);
                conn.setRequestProperty("Accept", "text/event-stream");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data:")) {
                        operatorMessages.add(line.substring(5).trim());
                        operatorReceived.countDown();
                        break;
                    }
                }
                reader.close();
                conn.disconnect();
            } catch (Exception e) {
                System.err.println("Operator SSE error: " + e.getMessage());
            }
        });

        userSseThread.start();
        operatorSseThread.start();

        // 3. Wait for connections
        Thread.sleep(1000);

        // 4. Operator sends message (both should receive it)
        given()
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(ContentType.JSON)
                .body("{\"content\":\"Message from operator to user!\"}")
                .post("/operator/conversations/" + conversationId + "/messages")
                .then()
                .statusCode(201);

        // 5. Both should receive the message
        boolean userGot = userReceived.await(5, TimeUnit.SECONDS);
        boolean operatorGot = operatorReceived.await(5, TimeUnit.SECONDS);

        assertTrue(userGot, "User should receive message via SSE");
        assertTrue(operatorGot, "Operator should receive message via SSE");

        // 6. Both should have same message
        assertEquals(1, userMessages.size());
        assertEquals(1, operatorMessages.size());
        assertTrue(userMessages.get(0).contains("Message from operator to user!"));
        assertTrue(operatorMessages.get(0).contains("Message from operator to user!"));

        // Cleanup
        userSseThread.join(1000);
        operatorSseThread.join(1000);
    }
}
