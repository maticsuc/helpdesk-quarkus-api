package si.helpdesk;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public class TestUtils {

    public static String loginUser(String username, String password) {
        return RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}")
                .when()
                .post("/auth/login/user")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("token");
    }

    public static String loginOperator(String username, String password) {
        return RestAssured
                .given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}")
                .when()
                .post("/auth/login/operator")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("token");
    }
}
