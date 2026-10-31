package com.example.securitylearning;

import java.util.Map;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "app.security-mode=none")
class PlainMvcIT {

    @LocalServerPort
    private int port;

    @Test
    void publicEndpointReturnsPublicJson() {
        assertEndpoint("/api/public", "public");
    }

    @Test
    void userEndpointReturnsUserJsonWithoutCredentials() {
        assertEndpoint("/api/user", "user");
    }

    @Test
    void adminEndpointReturnsAdminJsonWithoutCredentials() {
        assertEndpoint("/api/admin", "admin");
    }

    private void assertEndpoint(String path, String message) {
        given()
            .baseUri("http://localhost")
            .port(port)
        .when()
            .get(path)
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("", equalTo(Map.of("message", message)));
    }
}
