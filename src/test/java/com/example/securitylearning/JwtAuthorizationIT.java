package com.example.securitylearning;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import com.example.securitylearning.security.JwtClaimContractValidator;
import com.example.securitylearning.tracing.TraceEventSink;
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
import org.springframework.security.web.FilterChainProxy;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({JwtAuthorizationIT.FixedTimeConfiguration.class, JwtAuthorizationIT.IdentityProbe.class})
class JwtAuthorizationIT {

    private static final Instant NOW = Instant.parse("2030-01-01T12:00:00Z");
    private static final com.example.securitylearning.support.JwtKeyFixture KEY_FIXTURE =
        new com.example.securitylearning.support.JwtKeyFixture();

    @LocalServerPort
    private int port;

    @Autowired
    private TraceEventSink sink;

    @Autowired
    private JwtEncoder encoder;

    @Autowired
    private FilterChainProxy proxy;

    @Autowired
    private ApplicationContext context;

    @DynamicPropertySource
    static void keyPaths(DynamicPropertyRegistry properties) {
        properties.add("app.jwt.public-key-path", () -> KEY_FIXTURE.publicPath().toString());
        properties.add("app.jwt.private-key-path", () -> KEY_FIXTURE.privatePath().toString());
    }

    @AfterAll
    static void removeKeyFiles() throws IOException {
        KEY_FIXTURE.close();
    }

    @Test
    void publicEndpointDoesNotRequireToken() {
        String id = given().baseUri("http://localhost").port(port)
            .when().get("/api/public").then().statusCode(200)
            .contentType(ContentType.JSON).header("Set-Cookie", nullValue())
            .body("", equalTo(Map.of("message", "public"))).extract().header(TraceEventSink.HEADER);
        assertTrace(id, List.of("filter.entry", "security.anonymous", "interceptor.preHandle",
            "controller.public", "interceptor.postHandle", "interceptor.afterCompletion",
            "security.authorization.exit", "filter.exit"));
        List<String> filters = proxy.getFilterChains().get(0).getFilters().stream()
            .map(filter -> filter.getClass().getSimpleName()).toList();
        assertThat(filters).contains("BearerTokenAuthenticationFilter", "AuthorizationFilter")
            .doesNotContain("BasicAuthenticationFilter");
        assertThat(context.getBeansOfType(JwtEncoder.class)).hasSize(1);
        assertThat(context.getBeansOfType(org.springframework.security.core.userdetails.UserDetailsService.class)).hasSize(1);
        assertThat(context.getBeansOfType(org.springframework.security.crypto.password.PasswordEncoder.class)).hasSize(1);
        assertThat(context.getBeansOfType(org.springframework.security.authentication.dao.DaoAuthenticationProvider.class)).hasSize(1);
        assertThat(context.getBeansOfType(org.springframework.security.authentication.AuthenticationManager.class)).hasSize(1);

        assertThat(context.getEnvironment().getProperty("app.security-mode")).isNull();
        assertThat(context.getBeansOfType(org.springframework.security.web.SecurityFilterChain.class)).hasSize(1);
        assertThat(context.getBeansOfType(FilterChainProxy.class)).hasSize(1);
        assertThat(context.getBeansOfType(org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean.class)).hasSize(1);
        var registrations = context.getBean(jakarta.servlet.ServletContext.class).getFilterRegistrations();
        assertThat(registrations.values().stream()
            .filter(registration -> registration.getClassName().contains("DelegatingFilterProxy")).count()).isEqualTo(1);

        assertThat(context.getBean(RequestMappingHandlerMapping.class).getHandlerMethods().keySet())
            .anyMatch(mapping -> mapping.getPatternValues().contains("/auth/login"));
        System.out.println("JWT SECURITY FILTERS " + filters);
    }

    @ParameterizedTest
    @ValueSource(strings = {"user", "admin"})
    void basicCredentialsCannotAuthenticateProtectedEndpoint(String endpoint) {
        String id = given().baseUri("http://localhost").port(port)
            .auth().preemptive().basic("user", "user123")
            .when().get("/api/" + endpoint).then().statusCode(401)
            .header("Set-Cookie", nullValue())
            .header("WWW-Authenticate", equalTo("Bearer"))
            .extract().header(TraceEventSink.HEADER);
        assertTrace(id, List.of("filter.entry", "security.anonymous", "security.challenge",
            "security.authorization.exit", "filter.exit"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "ADMIN", "missing", "empty", "OTHER"})
    void validatedRolesBecomeRequestAuthorities(String role) {
        var response = given().baseUri("http://localhost").port(port)
            .header("Authorization", "Bearer " + roleToken(role))
            .when().get("/test/identity");
        response.then().statusCode(200).header("Set-Cookie", nullValue())
            .body("name", equalTo("role-subject"))
            .body("authorities", equalTo(role.equals("missing") || role.equals("empty")
                ? List.of() : List.of("ROLE_" + role)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER:user:200", "USER:admin:403", "ADMIN:user:200", "ADMIN:admin:200",
        "missing:user:200", "missing:admin:403", "empty:user:200", "empty:admin:403", "OTHER:admin:403", "OTHER:user:200", "USER:public:200", "ADMIN:public:200"})
    void endpointPolicyDistinguishesAuthenticationFromAdminRole(String scenario) {
        String[] parts = scenario.split(":");
        int status = Integer.parseInt(parts[2]);
        var response = given().baseUri("http://localhost").port(port)
            .header("Authorization", "Bearer " + roleToken(parts[0]))
            .when().get("/api/" + parts[1]);
        response.then().statusCode(status).header("Set-Cookie", nullValue());
        if (status == 200) {
            response.then().contentType(ContentType.JSON).header("WWW-Authenticate", nullValue())
                .body("", equalTo(Map.of("message", parts[1])));
        } else {
            response.then().header("WWW-Authenticate", org.hamcrest.Matchers.containsString("insufficient_scope"));
        }
        assertTrace(response.header(TraceEventSink.HEADER), status == 200
            ? List.of("filter.entry", "security.authenticated", "interceptor.preHandle", "controller." + parts[1],
                "interceptor.postHandle", "interceptor.afterCompletion", "security.authorization.exit", "filter.exit")
            : List.of("filter.entry", "security.authenticated", "security.denied", "security.authorization.exit", "filter.exit"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"user", "admin"})
    void missingBearerReturns401BeforeMvc(String endpoint) {
        var response = given().baseUri("http://localhost").port(port).when().get("/api/" + endpoint);
        response.then().statusCode(401).header("WWW-Authenticate", equalTo("Bearer"))
            .header("Set-Cookie", nullValue());
        assertTrace(response.header(TraceEventSink.HEADER), List.of("filter.entry", "security.anonymous", "security.challenge",
            "security.authorization.exit", "filter.exit"));
    }

    @Test
    void bearerRequestDoesNotEstablishSessionAuthentication() {
        var first = given().baseUri("http://localhost").port(port)
            .header("Authorization", "Bearer " + roleToken("ADMIN"))
            .when().get("/api/admin");
        first.then().statusCode(200).header("Set-Cookie", nullValue());
        given().baseUri("http://localhost").port(port).cookie("JSESSIONID", "untrusted-demo-session")
            .when().get("/api/admin").then().statusCode(401)
            .header("WWW-Authenticate", equalTo("Bearer")).header("Set-Cookie", nullValue());
    }

    private String roleToken(String role) {
        var claims = JwtClaimsSet.builder().subject("role-subject").issuer(JwtClaimContractValidator.ISSUER)
            .audience(List.of(JwtClaimContractValidator.AUDIENCE)).issuedAt(NOW).notBefore(NOW)
            .expiresAt(NOW.plusSeconds(300));
        if (!role.equals("missing")) {
            claims.claim("roles", role.equals("empty") ? List.of() : List.of(role));
        }
        return encoder.encode(JwtEncoderParameters.from(
            JwsHeader.with(SignatureAlgorithm.RS256).keyId("local-issuer").build(), claims.build())).getTokenValue();
    }

    @org.springframework.boot.test.context.TestComponent
    @org.springframework.web.bind.annotation.RestController
    static class IdentityProbe {
        @org.springframework.web.bind.annotation.GetMapping("/test/identity")
        Map<String, Object> identity(org.springframework.security.core.Authentication authentication) {
            return Map.of("name", authentication.getName(), "authorities", authentication.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority).sorted().toList());
        }
    }

    private void assertTrace(String id, List<String> expected) {
        assertThat(id).isNotBlank();
        await().untilAsserted(() -> assertThat(sink.events(id)).isEqualTo(expected));
        System.out.println("JWT TRACE " + id + " " + sink.events(id));
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
