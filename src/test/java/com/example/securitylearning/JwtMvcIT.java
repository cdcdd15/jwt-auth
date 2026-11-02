package com.example.securitylearning;

import java.io.IOException;
import com.example.securitylearning.support.JwtKeyFixture;
import com.example.securitylearning.support.JwtTestTokens;
import org.junit.jupiter.api.AfterAll;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;


import java.util.Map;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JwtMvcIT {

    private static final JwtKeyFixture KEYS = new JwtKeyFixture();
    @Autowired
    private JwtEncoder encoder;

    @DynamicPropertySource
    static void keyPaths(DynamicPropertyRegistry properties) {
        properties.add("app.jwt.public-key-path", () -> KEYS.publicPath().toString());
        properties.add("app.jwt.private-key-path", () -> KEYS.privatePath().toString());
    }

    @AfterAll
    static void removeKeys() throws IOException {
        KEYS.close();
    }


    @LocalServerPort
    private int port;

    @Test
    void publicEndpointReturnsPublicJson() {
        assertEndpoint("/api/public", "public");
    }

    @Test
    void userEndpointReturnsUserJsonWithBearer() {
        assertEndpoint("/api/user", "user");
    }

    @Test
    void adminEndpointReturnsAdminJsonWithBearer() {
        assertEndpoint("/api/admin", "admin");
    }

    private void assertEndpoint(String path, String message) {
        given()
            .baseUri("http://localhost")
            .port(port)
            .header("Authorization", "Bearer " + JwtTestTokens.admin(encoder))
        .when()
            .get(path)
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("", equalTo(Map.of("message", message)));
    }
}
