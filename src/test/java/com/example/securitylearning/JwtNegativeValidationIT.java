package com.example.securitylearning;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPrivateKey;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.securitylearning.security.JwtClaimContractValidator;
import com.example.securitylearning.security.RsaKeyGenerator;
import com.example.securitylearning.support.JwtKeyFixture;
import com.example.securitylearning.tracing.TraceEventSink;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.Base64URL;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(JwtNegativeValidationIT.FixedTimeConfiguration.class)
class JwtNegativeValidationIT {
    private static final Instant NOW = Instant.parse("2030-01-01T12:00:00Z");
    private static final JwtKeyFixture KEYS = new JwtKeyFixture();
    private static final ObjectMapper JSON = new ObjectMapper();

    @LocalServerPort
    private int port;
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
    @ValueSource(strings = {"tampered", "unknown-key", "hs256", "malformed", "expired", "future-nbf", "issuer", "audience",
        "missing-sub", "missing-iss", "missing-aud", "missing-exp", "null-sub", "null-iss", "null-aud", "null-exp",
        "null-iat", "null-nbf", "null-roles", "number-sub", "number-iss", "number-aud", "text-exp", "text-iat", "text-nbf",
        "boolean-exp", "blank-sub", "empty-aud", "mixed-aud", "scalar-roles", "object-roles", "number-role", "null-role", "blank-role"})
    void invalidRawTokensFailClosedBeforeMvc(String scenario) throws Exception {
        var claims = claims();
        alter(claims, scenario);
        String token = signed(claims, scenario);
        var response = given().baseUri("http://localhost").port(port)
            .header("Authorization", "Bearer " + token).when().get("/api/user");
        response.then().statusCode(401).header("WWW-Authenticate", containsString("invalid_token"))
            .header("Set-Cookie", nullValue());
        assertTrace(response.header(TraceEventSink.HEADER), scenario, List.of("filter.entry", "security.challenge", "filter.exit"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"exp:-61:401", "exp:-60:200", "exp:-59:200", "nbf:61:401", "nbf:60:200", "nbf:59:200"})
    void timestampSkewHasDeterministicBoundaries(String scenario) throws Exception {
        String[] parts = scenario.split(":");
        var claims = claims();
        claims.put(parts[0], NOW.plusSeconds(Long.parseLong(parts[1])).getEpochSecond());
        int status = Integer.parseInt(parts[2]);
        var response = given().baseUri("http://localhost").port(port)
            .header("Authorization", "Bearer " + signed(claims, "valid")).when().get("/api/user");
        response.then().statusCode(status).header("Set-Cookie", nullValue());
        if (status == 200) {
            response.then().body("", equalTo(Map.of("message", "user")));
        } else {
            response.then().header("WWW-Authenticate", containsString("invalid_token"));
        }
        assertTrace(response.header(TraceEventSink.HEADER), scenario, status == 401
            ? List.of("filter.entry", "security.challenge", "filter.exit") : successTrace());
    }

    @ParameterizedTest
    @ValueSource(strings = {"single-audience", "missing-iat", "missing-nbf", "missing-roles", "empty-roles"})
    void permittedRawRepresentationsRemainAuthenticated(String scenario) throws Exception {
        var claims = claims();
        switch (scenario) {
            case "single-audience" -> claims.put("aud", JwtClaimContractValidator.AUDIENCE);
            case "empty-roles" -> claims.put("roles", List.of());
            default -> claims.remove(scenario.substring("missing-".length()));
        }
        var response = given().baseUri("http://localhost").port(port)
            .header("Authorization", "Bearer " + signed(claims, "valid")).when().get("/api/user");
        response.then().statusCode(200).body("", equalTo(Map.of("message", "user")))
            .header("Set-Cookie", nullValue());
        assertTrace(response.header(TraceEventSink.HEADER), scenario, successTrace());
    }

    private static Map<String, Object> claims() {
        var claims = new LinkedHashMap<String, Object>();
        claims.put("sub", "negative-test-user");
        claims.put("iss", JwtClaimContractValidator.ISSUER);
        claims.put("aud", List.of(JwtClaimContractValidator.AUDIENCE));
        claims.put("iat", NOW.minusSeconds(300).getEpochSecond());
        claims.put("nbf", NOW.minusSeconds(300).getEpochSecond());
        claims.put("exp", NOW.plusSeconds(300).getEpochSecond());
        claims.put("roles", List.of("USER"));
        return claims;
    }

    private static void alter(Map<String, Object> claims, String scenario) {
        if (scenario.startsWith("missing-")) {
            claims.remove(scenario.substring(8));
        } else if (scenario.startsWith("null-") && !scenario.equals("null-role")) {
            claims.put(scenario.substring(5), null);
        } else {
            switch (scenario) {
                case "expired" -> claims.put("exp", NOW.minusSeconds(61).getEpochSecond());
                case "future-nbf" -> claims.put("nbf", NOW.plusSeconds(61).getEpochSecond());
                case "issuer" -> claims.put("iss", "urn:other");
                case "audience" -> claims.put("aud", List.of("other-api"));
                case "number-sub" -> claims.put("sub", 42);
                case "number-iss" -> claims.put("iss", 42);
                case "number-aud" -> claims.put("aud", 42);
                case "text-exp" -> claims.put("exp", "1893499500");
                case "text-iat" -> claims.put("iat", "1893499200");
                case "text-nbf" -> claims.put("nbf", "1893499200");
                case "boolean-exp" -> claims.put("exp", true);
                case "blank-sub" -> claims.put("sub", " ");
                case "empty-aud" -> claims.put("aud", List.of());
                case "mixed-aud" -> claims.put("aud", List.of(JwtClaimContractValidator.AUDIENCE, 42));
                case "scalar-roles" -> claims.put("roles", "ADMIN");
                case "object-roles" -> claims.put("roles", Map.of("role", "ADMIN"));
                case "number-role" -> claims.put("roles", List.of(42));
                case "blank-role" -> claims.put("roles", List.of(" "));
                case "null-role" -> {
                    var roles = new ArrayList<>();
                    roles.add(null);
                    claims.put("roles", roles);
                }
                default -> { }
            }
        }
    }

    private static String signed(Map<String, Object> claims, String scenario) throws Exception {
        if (scenario.equals("malformed")) {
            return "not.a.jwt";
        }
        var jws = new JWSObject(new JWSHeader(scenario.equals("hs256") ? JWSAlgorithm.HS256 : JWSAlgorithm.RS256),
            new Payload(JSON.writeValueAsString(claims)));
        if (scenario.equals("hs256")) {
            jws.sign(new MACSigner(new byte[32])); // Test-only symmetric key; never an accepted server algorithm.
        } else {
            var privateKey = scenario.equals("unknown-key")
                ? RsaKeyGenerator.generateDevelopmentKeyPair().getPrivate() : KEYS.keys().getPrivate();
            jws.sign(new RSASSASigner((RSAPrivateKey) privateKey));
        }
        String token = jws.serialize();
        if (scenario.equals("tampered")) {
            String[] parts = token.split("\\.");
            claims.put("sub", "changed-after-signing");
            return parts[0] + "." + Base64URL.encode(JSON.writeValueAsString(claims).getBytes(StandardCharsets.UTF_8))
                + "." + parts[2];
        }
        return token;
    }

    private static List<String> successTrace() {
        return List.of("filter.entry", "security.authenticated", "interceptor.preHandle", "controller.user",
            "interceptor.postHandle", "interceptor.afterCompletion", "security.authorization.exit", "filter.exit");
    }

    private void assertTrace(String id, String scenario, List<String> expected) {
        assertThat(id).isNotBlank();
        await().untilAsserted(() -> assertThat(sink.events(id)).isEqualTo(expected));
        System.out.println("JWT NEGATIVE TRACE " + scenario + " " + id + " " + sink.events(id));
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
