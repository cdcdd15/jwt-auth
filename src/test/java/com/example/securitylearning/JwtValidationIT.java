package com.example.securitylearning;

import java.io.IOException;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import com.example.securitylearning.security.JwtClaimContractValidator;
import com.example.securitylearning.security.RsaKeyGenerator;
import com.example.securitylearning.tracing.TraceEventSink;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "app.security-mode=jwt")
@Import(JwtValidationIT.FixedTimeConfiguration.class)
class JwtValidationIT {

    private static final Instant NOW = Instant.parse("2030-01-01T12:00:00Z");
    private static final com.example.securitylearning.support.JwtKeyFixture KEY_FIXTURE =
        new com.example.securitylearning.support.JwtKeyFixture();
    private static final KeyPair SIGNING_KEYS = KEY_FIXTURE.keys();

    @LocalServerPort
    private int port;

    @Autowired
    private TraceEventSink sink;

    @Autowired
    private FilterChainProxy proxy;

    @Autowired
    private ApplicationContext context;

    @DynamicPropertySource
    static void publicKeyLocation(DynamicPropertyRegistry properties) {
        properties.add("app.jwt.public-key-path", () -> KEY_FIXTURE.publicPath().toString());
        properties.add("app.jwt.private-key-path", () -> KEY_FIXTURE.privatePath().toString());
    }

    @AfterAll
    static void removePublicKeyFile() throws IOException {
        KEY_FIXTURE.close();
    }

    @Test
    void publicEndpointDoesNotRequireToken() {
        String id = given().baseUri("http://localhost").port(port)
            .when().get("/api/public").then().statusCode(200)
            .body("", equalTo(Map.of("message", "public"))).extract().header(TraceEventSink.HEADER);
        assertTrace(id, List.of("filter.entry", "security.anonymous", "interceptor.preHandle",
            "controller.public", "interceptor.postHandle", "interceptor.afterCompletion",
            "security.authorization.exit", "filter.exit"));
        List<String> filters = proxy.getFilterChains().getFirst().getFilters().stream()
            .map(filter -> filter.getClass().getSimpleName()).toList();
        assertThat(filters).contains("BearerTokenAuthenticationFilter", "AuthorizationFilter")
            .doesNotContain("BasicAuthenticationFilter");
        assertThat(context.getBeansOfType(JwtEncoder.class)).hasSize(1);
        assertThat(context.getBean(RequestMappingHandlerMapping.class).getHandlerMethods().keySet())
            .anyMatch(mapping -> mapping.getPatternValues().contains("/auth/login"));
        System.out.println("JWT SECURITY FILTERS " + filters);
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "ADMIN"})
    void validTokenReachesExpectedEndpoint(String role) {
        String endpoint = role.equals("ADMIN") ? "admin" : "user";
        String id = given().baseUri("http://localhost").port(port).accept(ContentType.JSON)
            .header("Authorization", "Bearer " + token(role, "valid"))
            .when().get("/api/" + endpoint).then().statusCode(200).contentType(ContentType.JSON)
            .body("", equalTo(Map.of("message", endpoint))).header("Set-Cookie", nullValue())
            .extract().header(TraceEventSink.HEADER);
        assertTrace(id, List.of("filter.entry", "security.authenticated", "interceptor.preHandle",
            "controller." + endpoint, "interceptor.postHandle", "interceptor.afterCompletion",
            "security.authorization.exit", "filter.exit"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"signature", "expired", "issuer", "audience"})
    void invalidTokenReturns401BeforeApplicationHandler(String scenario) {
        String id = given().baseUri("http://localhost").port(port)
            .header("Authorization", "Bearer " + token("USER", scenario))
            .when().get("/api/user").then().statusCode(401)
            .header("WWW-Authenticate", startsWith("Bearer"))
            .header("Set-Cookie", nullValue()).extract().header(TraceEventSink.HEADER);
        assertTrace(id, List.of("filter.entry", "filter.exit"));
    }

    @Test
    void basicCredentialsCannotAuthenticateJwtMode() {
        String id = given().baseUri("http://localhost").port(port)
            .auth().preemptive().basic("user", "user123")
            .when().get("/api/user").then().statusCode(401)
            .header("WWW-Authenticate", startsWith("Bearer"))
            .extract().header(TraceEventSink.HEADER);
        assertTrace(id, List.of("filter.entry", "security.anonymous",
            "security.authorization.exit", "filter.exit"));
    }

    private void assertTrace(String id, List<String> expected) {
        assertThat(id).isNotBlank();
        await().untilAsserted(() -> assertThat(sink.events(id)).isEqualTo(expected));
        System.out.println("JWT TRACE " + id + " " + sink.events(id));
    }

    private static String token(String role, String scenario) {
        KeyPair keys = scenario.equals("signature") ? RsaKeyGenerator.generateDevelopmentKeyPair() : SIGNING_KEYS;
        var jwk = new RSAKey.Builder((RSAPublicKey) keys.getPublic())
            .privateKey((RSAPrivateKey) keys.getPrivate()).build();
        var encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
        var claims = JwtClaimsSet.builder().subject(role.toLowerCase(java.util.Locale.ROOT))
            .issuer(scenario.equals("issuer") ? "urn:other" : JwtClaimContractValidator.ISSUER)
            .audience(List.of(scenario.equals("audience") ? "other-api" : JwtClaimContractValidator.AUDIENCE))
            .claim("roles", List.of(role)).issuedAt(NOW.minusSeconds(300)).notBefore(NOW.minusSeconds(300))
            .expiresAt(scenario.equals("expired") ? NOW.minusSeconds(120) : NOW.plusSeconds(300)).build();
        return encoder.encode(JwtEncoderParameters.from(
            JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").build(), claims)).getTokenValue();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedTimeConfiguration {

        @Bean
        @Primary
        Clock fixedValidationClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
