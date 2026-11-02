package com.example.securitylearning;

import java.io.IOException;
import com.example.securitylearning.support.JwtKeyFixture;
import com.example.securitylearning.support.JwtTestTokens;
import org.junit.jupiter.api.AfterAll;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;


import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.example.securitylearning.tracing.TraceEventSink;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RequestTraceIT {

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

    @Autowired
    private TraceEventSink sink;

    @ParameterizedTest
    @ValueSource(strings = {"public", "user", "admin"})
    void successfulRequestRecordsNestedOrder(String endpoint) {
        assertTrace(request(endpoint), endpoint);
    }

    @Test
    void concurrentRequestsKeepIndependentTraces() {
        var publicRequest = CompletableFuture.supplyAsync(() -> request("public"));
        var adminRequest = CompletableFuture.supplyAsync(() -> request("admin"));
        String publicId = publicRequest.join();
        String adminId = adminRequest.join();
        assertThat(publicId).isNotEqualTo(adminId);
        assertTrace(publicId, "public");
        assertTrace(adminId, "admin");
        assertThat(sink.events("unknown-id")).isEmpty();
    }

    private String request(String endpoint) {
        return given()
            .baseUri("http://localhost")
            .port(port)
            .header("Authorization", "Bearer " + JwtTestTokens.admin(encoder))
            .header(TraceEventSink.HEADER, "client-value-is-not-trusted")
        .when()
            .get("/api/" + endpoint)
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("", equalTo(Map.of("message", endpoint)))
            .extract().header(TraceEventSink.HEADER);
    }

    private void assertTrace(String id, String endpoint) {
        assertThat(id).isNotBlank().isNotEqualTo("client-value-is-not-trusted");
        List<String> expected = List.of("filter.entry", "security.authenticated", "interceptor.preHandle",
            "controller." + endpoint, "interceptor.postHandle",
            "interceptor.afterCompletion", "security.authorization.exit", "filter.exit");
        await().untilAsserted(() -> assertThat(sink.events(id)).isEqualTo(expected));
        System.out.println("TRACE " + id + " " + sink.events(id));
    }
}
