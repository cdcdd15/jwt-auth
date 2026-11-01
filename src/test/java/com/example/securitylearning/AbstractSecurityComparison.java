package com.example.securitylearning;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.example.securitylearning.tracing.TraceEventSink;
import io.restassured.response.Response;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.web.FilterChainProxy;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

abstract class AbstractSecurityComparison {
    @LocalServerPort private int port;
    @Value("${app.security-mode}") private String mode;
    @Autowired private TraceEventSink sink;
    @Autowired private ApplicationContext context;

    @ParameterizedTest
    @ValueSource(strings = {"public-anonymous", "user-anonymous", "admin-anonymous", "user-USER", "admin-USER",
        "admin-ADMIN", "user-basic", "user-invalid-bearer", "login-valid", "login-invalid",
        "public-invalid-basic", "public-invalid-bearer", "missing-authenticated"})
    void equivalentRequestsExposeModeDifferences(String scenario) {
        boolean login = scenario.startsWith("login-");
        String endpoint = scenario.startsWith("public-") ? "public" : scenario.startsWith("admin-") ? "admin"
            : scenario.startsWith("missing-") ? "not-mapped" : "user";
        var specification = given().baseUri("http://localhost").port(port).accept("application/json");
        if (scenario.endsWith("invalid-bearer")) {
            specification.header("Authorization", "Bearer not.a.jwt");
        } else if (scenario.equals("public-invalid-basic")) {
            specification.auth().preemptive().basic("user", "incorrect");
        } else if (scenario.equals("user-basic") || login && mode.equals("basic")) {
            specification.auth().preemptive().basic("user", "user123");
        } else if (scenario.endsWith("USER") || scenario.endsWith("ADMIN") || scenario.startsWith("missing-")) {
            String role = scenario.endsWith("ADMIN") ? "ADMIN" : "USER";
            if (mode.equals("basic")) {
                String username = role.equals("ADMIN") ? "admin" : "user";
                specification.auth().preemptive().basic(username, username + "123");
            } else {
                specification.header("Authorization", "Bearer " + (mode.equals("jwt") ? token(role) : "ignored-demo-value"));
            }
        }
        Response response = login ? specification.contentType("application/json")
            .body(Map.of("username", "user", "password", scenario.equals("login-valid") ? "user123" : "incorrect"))
            .when().post("/auth/login") : specification.when().get("/api/" + endpoint);
        int status = expectedStatus(scenario);
        response.then().statusCode(status).header("Set-Cookie", nullValue());
        if (status == 200 && !login) response.then().body("", equalTo(Map.of("message", endpoint)));
        if (status == 404) response.then().body("status", equalTo(404));
        if (status == 401 && !(login && mode.equals("jwt"))) {
            assertThat(response.header("WWW-Authenticate")).startsWith(mode.equals("basic") ? "Basic" : "Bearer");
        }
        if (login && mode.equals("jwt")) {
            response.then().header("WWW-Authenticate", nullValue());
            if (status == 200) {
                assertThat(response.jsonPath().getMap("").keySet()).containsExactlyInAnyOrder("accessToken", "tokenType", "expiresIn");
                assertThat(response.jsonPath().getString("accessToken") != null).isTrue();
            } else response.then().body("", equalTo(Map.of("error", "invalid_credentials")));
        }
        if (status == 403) {
            if (mode.equals("basic")) response.then().header("WWW-Authenticate", nullValue());
            else assertThat(response.header("WWW-Authenticate")).contains("insufficient_scope");
        }
        String id = response.header(TraceEventSink.HEADER);
        assertThat(id).isNotBlank();
        await().untilAsserted(() -> assertThat(sink.events(id)).isNotEmpty());
        List<String> events = sink.events(id);
        assertThat(events).startsWith("filter.entry").endsWith("filter.exit");
        if (mode.equals("none")) {
            assertThat(context.getBeansOfType(FilterChainProxy.class)).isEmpty();
            assertThat(events).noneMatch(event -> event.startsWith("security."));
        } else {
            assertThat(context.getBeansOfType(FilterChainProxy.class)).hasSize(1);
            if (status == 403) assertThat(events).containsSubsequence("security.authenticated", "security.denied", "security.authorization.exit");
            if (status == 401 && !login) assertThat(events).contains("security.challenge").doesNotContain("interceptor.preHandle");
        }
        if (status == 200 && !login) assertThat(events).containsSubsequence("interceptor.preHandle", "controller." + endpoint,
            "interceptor.postHandle", "interceptor.afterCompletion");
        if (status == 404) assertThat(events).contains("interceptor.preHandle", "interceptor.afterCompletion")
            .doesNotContain("interceptor.postHandle");
        System.out.println("COMPARISON " + mode + " " + scenario + " status=" + status + " " + id + " " + events);
    }

    private int expectedStatus(String scenario) {
        if (scenario.startsWith("login-")) return !mode.equals("jwt") ? 404 : scenario.equals("login-valid") ? 200 : 401;
        if (scenario.startsWith("missing-")) return 404;
        if (mode.equals("none")) return 200;
        if (scenario.equals("public-invalid-basic")) return mode.equals("basic") ? 401 : 200;
        if (scenario.endsWith("invalid-bearer")) return mode.equals("jwt") || scenario.startsWith("user-") ? 401 : 200;
        if (scenario.equals("public-anonymous")) return 200;
        if (scenario.endsWith("anonymous")) return 401;
        if (scenario.equals("user-basic") && mode.equals("jwt")) return 401;
        return scenario.equals("admin-USER") ? 403 : 200;
    }

    private String token(String role) {
        Instant now = Instant.parse("2030-01-01T12:00:00Z");
        var claims = JwtClaimsSet.builder().subject(role.toLowerCase(java.util.Locale.ROOT))
            .issuer(com.example.securitylearning.security.JwtClaimContractValidator.ISSUER)
            .audience(List.of(com.example.securitylearning.security.JwtClaimContractValidator.AUDIENCE))
            .issuedAt(now).notBefore(now).expiresAt(now.plusSeconds(300)).claim("roles", List.of(role)).build();
        return context.getBean(JwtEncoder.class).encode(JwtEncoderParameters.from(
            JwsHeader.with(SignatureAlgorithm.RS256).keyId("local-issuer").build(), claims)).getTokenValue();
    }
}
