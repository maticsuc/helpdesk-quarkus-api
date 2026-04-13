package si.helpdesk.operator;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import si.helpdesk.TestUtils;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

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
}
