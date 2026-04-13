package si.helpdesk.auth;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class AuthResourceTest {

    @Test
    void testUserLoginSuccess() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"janez\",\"password\":\"password123\"}")
                .when()
                .post("/auth/login/user")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("expiresIn", equalTo(3600));
    }

    @Test
    void testUserLoginWrongPassword() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"janez\",\"password\":\"wrongpassword\"}")
                .when()
                .post("/auth/login/user")
                .then()
                .statusCode(401)
                .body("error", equalTo("INVALID_CREDENTIALS"));
    }

    @Test
    void testUserLoginUnknownUser() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"nobody\",\"password\":\"password123\"}")
                .when()
                .post("/auth/login/user")
                .then()
                .statusCode(401);
    }

    @Test
    void testOperatorLoginSuccess() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"operator1\",\"password\":\"password123\"}")
                .when()
                .post("/auth/login/operator")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("expiresIn", equalTo(3600));
    }

    @Test
    void testOperatorLoginWrongPassword() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"operator1\",\"password\":\"wrongpassword\"}")
                .when()
                .post("/auth/login/operator")
                .then()
                .statusCode(401);
    }
}
