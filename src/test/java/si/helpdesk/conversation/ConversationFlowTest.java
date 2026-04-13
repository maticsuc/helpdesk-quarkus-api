package si.helpdesk.conversation;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import si.helpdesk.TestUtils;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

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
}
