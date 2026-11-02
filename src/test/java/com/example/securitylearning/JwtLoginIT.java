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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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
    void loginIssuesTrustedTokenForEndpointPolicy(String username) {
        Response login = login(Map.of("username", username, "password", username + "123", "roles", List.of("ADMIN")));
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(login.header("Cache-Control")).contains("no-store");
        assertThat(login.header("Set-Cookie")).isNull();
        assertThat(login.header("WWW-Authenticate")).isNull();
        assertThat(login.contentType()).startsWith("application/json");
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

        for (String endpoint : List.of("public", "user", "admin")) {
            int status = endpoint.equals("admin") && username.equals("user") ? 403 : 200;
            Response resource = given().baseUri("http://localhost").port(port).accept(ContentType.JSON)
                .header("Authorization", "Bearer " + token).when().get("/api/" + endpoint);
            resource.then().statusCode(status).header("Set-Cookie", nullValue());
            if (status == 200) {
                resource.then().contentType(ContentType.JSON).header("WWW-Authenticate", nullValue())
                    .body("", equalTo(Map.of("message", endpoint)));
            } else {
                resource.then().header("WWW-Authenticate", org.hamcrest.Matchers.containsString("insufficient_scope"));
            }
            assertTrace(resource, status == 200
                ? List.of("filter.entry", "security.authenticated", "interceptor.preHandle", "controller." + endpoint,
                    "interceptor.postHandle", "interceptor.afterCompletion", "security.authorization.exit", "filter.exit")
                : List.of("filter.entry", "security.authenticated", "security.denied", "security.authorization.exit", "filter.exit"));
        }
        Response withoutBearer = given().baseUri("http://localhost").port(port).accept(ContentType.JSON)
            .cookie("JSESSIONID", "untrusted-session").when().get("/api/user");
        withoutBearer.then().statusCode(401).header("WWW-Authenticate", equalTo("Bearer"))
            .header("Set-Cookie", nullValue());
        assertTrace(withoutBearer, List.of("filter.entry", "security.anonymous", "security.challenge",
            "security.authorization.exit", "filter.exit"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"wrong-password", "unknown-user"})
    void badCredentialsReturnGeneric401WithoutToken(String scenario) {
        Response response = login(Map.of("username", scenario.equals("unknown-user") ? "unknown" : "user", "password", "incorrect"));
        response.then().statusCode(401).contentType(ContentType.JSON).body("", equalTo(Map.of("error", "invalid_credentials")))
            .header("WWW-Authenticate", nullValue()).header("Set-Cookie", nullValue());
        assertThat(response.header("Cache-Control")).contains("no-store");
        assertTrace(response, List.of("filter.entry", "security.anonymous", "interceptor.preHandle", "controller.login",
            "login.credentials.rejected", "interceptor.afterCompletion", "security.authorization.exit", "filter.exit"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing-username", "blank-username", "missing-password", "empty-password",
        "null-username", "null-password", "invalid-json", "empty-body"})
    void malformedLoginReturns400WithoutToken(String scenario) {
        String body = switch (scenario) {
            case "missing-username" -> "{\"password\":\"incorrect\"}";
            case "blank-username" -> "{\"username\":\" \",\"password\":\"incorrect\"}";
            case "missing-password" -> "{\"username\":\"user\"}";
            case "empty-password" -> "{\"username\":\"user\",\"password\":\"\"}";
            case "null-username" -> "{\"username\":null,\"password\":\"incorrect\"}";
            case "null-password" -> "{\"username\":\"user\",\"password\":null}";
            case "invalid-json" -> "{";
            case "empty-body" -> "";
            default -> throw new IllegalArgumentException("Unknown test scenario");
        };
        Response response = login(body);
        response.then().statusCode(400).contentType(ContentType.JSON)
            .header("Set-Cookie", nullValue()).header("WWW-Authenticate", nullValue());
        assertThat(response.jsonPath().getMap("").keySet()).doesNotContain("accessToken", "password", "exception", "trace", "message");
        String id = response.header(TraceEventSink.HEADER);
        assertThat(id).isNotBlank();
        await().untilAsserted(() -> assertThat(sink.events(id)).isNotEmpty());
        assertThat(sink.events(id)).doesNotContain("login.token.issued", "login.credentials.rejected");
        if (scenario.equals("invalid-json") || scenario.equals("empty-body")) {
            assertThat(sink.events(id)).doesNotContain("controller.login");
        } else {
            assertThat(sink.events(id)).contains("controller.login");
        }
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
