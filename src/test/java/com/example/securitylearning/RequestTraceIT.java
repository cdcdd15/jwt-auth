package com.example.securitylearning;

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
        List<String> expected = List.of("filter.entry", "interceptor.preHandle",
            "controller." + endpoint, "interceptor.postHandle",
            "interceptor.afterCompletion", "filter.exit");
        await().untilAsserted(() -> assertThat(sink.events(id)).isEqualTo(expected));
        System.out.println("TRACE " + id + " " + sink.events(id));
    }
}
