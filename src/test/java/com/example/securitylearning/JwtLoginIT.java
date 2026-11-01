package com.example.securitylearning;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import com.example.securitylearning.support.JwtKeyFixture;
import com.example.securitylearning.tracing.TraceEventSink;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "app.security-mode=jwt")
@Import(JwtLoginIT.FixedTimeConfiguration.class)
class JwtLoginIT {

    private static final Instant NOW = Instant.parse("2030-01-01T12:00:00Z");
    private static final JwtKeyFixture KEYS = new JwtKeyFixture();

    @LocalServerPort
    private int port;

    @Autowired
    private JwtDecoder decoder;

    @Autowired
    private TraceEventSink sink;

    @DynamicPropertySource
    static void keyPaths(DynamicPropertyRegistry properties) {
        properties.add("app.jwt.public-key-path", () -> KEYS.publicPath().toString());
        properties.add("app.jwt.private-key-path", () -> KEYS.privatePath().toString());
    }

    @AfterAll
    static void removeKeys() throws IOException {
        KEYS.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"user", "admin"})
    void loginIssuesSafeTokenAcceptedByProtectedEndpoint(String username) {
        Response login = login(Map.of("username", username, "password", username + "123", "roles", List.of("ADMIN")));
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(login.header("Cache-Control")).contains("no-store");
        assertThat(login.header("Set-Cookie")).isNull();
        assertThat(login.jsonPath().getMap("").keySet()).containsExactlyInAnyOrder("accessToken", "tokenType", "expiresIn");
        assertThat(login.jsonPath().getString("tokenType")).isEqualTo("Bearer");
        assertThat(login.jsonPath().getLong("expiresIn")).isEqualTo(300);
        String token = login.jsonPath().getString("accessToken");
        assertThat(token != null && !token.isBlank()).isTrue();
        var decoded = decoder.decode(token);
        assertThat(decoded.getSubject()).isEqualTo(username);
        assertThat(decoded.getHeaders().get("alg")).isEqualTo("RS256");
        assertThat(decoded.getClaimAsStringList("roles")).containsExactly(username.toUpperCase(java.util.Locale.ROOT));
        assertThat(decoded.getIssuedAt()).isEqualTo(NOW);
        assertThat(decoded.getNotBefore()).isEqualTo(NOW);
        assertThat(decoded.getExpiresAt()).isEqualTo(NOW.plusSeconds(300));
        assertThat(decoded.getClaims().keySet()).containsExactlyInAnyOrder("sub", "iss", "aud", "roles", "iat", "nbf", "exp");
        assertTrace(login, List.of("filter.entry", "security.anonymous", "interceptor.preHandle", "controller.login",
            "login.token.issued", "interceptor.postHandle", "interceptor.afterCompletion", "security.authorization.exit", "filter.exit"));

        String endpoint = username.equals("admin") ? "admin" : "user";
        Response protectedResponse = given().baseUri("http://localhost").port(port)
            .header("Authorization", "Bearer " + token).when().get("/api/" + endpoint);
        protectedResponse.then().statusCode(200).body("", equalTo(Map.of("message", endpoint)))
            .header("Set-Cookie", nullValue());
        assertTrace(protectedResponse, List.of("filter.entry", "security.authenticated", "interceptor.preHandle",
            "controller." + endpoint, "interceptor.postHandle", "interceptor.afterCompletion", "security.authorization.exit", "filter.exit"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"wrong-password", "unknown-user"})
    void badCredentialsReturnGeneric401WithoutToken(String scenario) {
        Response response = login(Map.of("username", scenario.equals("unknown-user") ? "unknown" : "user", "password", "incorrect"));
        response.then().statusCode(401).body("", equalTo(Map.of("error", "invalid_credentials")))
            .header("WWW-Authenticate", nullValue()).header("Set-Cookie", nullValue());
        assertThat(response.header("Cache-Control")).contains("no-store");
        assertTrace(response, List.of("filter.entry", "security.anonymous", "interceptor.preHandle", "controller.login",
            "login.credentials.rejected", "interceptor.afterCompletion", "security.authorization.exit", "filter.exit"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing-username", "empty-password", "invalid-json"})
    void malformedLoginReturns400WithoutToken(String scenario) {
        Response response = scenario.equals("invalid-json")
            ? login("{") : login(scenario.equals("missing-username") ? Map.of("password", "incorrect")
                : Map.of("username", "user", "password", ""));
        response.then().statusCode(400);
        assertThat(response.jsonPath().getMap("").keySet()).doesNotContain("accessToken", "password", "exception", "trace", "message");
    }

    private Response login(Object body) {
        return given().baseUri("http://localhost").port(port).contentType(ContentType.JSON).accept(ContentType.JSON)
            .body(body).when().post("/auth/login");
    }

    private void assertTrace(Response response, List<String> expected) {
        String id = response.header(TraceEventSink.HEADER);
        assertThat(id).isNotBlank();
        await().untilAsserted(() -> assertThat(sink.events(id)).isEqualTo(expected));
        System.out.println("LOGIN TRACE " + id + " " + sink.events(id));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedTimeConfiguration {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
