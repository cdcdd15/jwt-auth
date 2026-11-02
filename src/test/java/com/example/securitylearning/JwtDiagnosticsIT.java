package com.example.securitylearning;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import com.example.securitylearning.support.JwtKeyFixture;
import com.example.securitylearning.tracing.TraceEventSink;
import io.restassured.response.Response;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(JwtDiagnosticsIT.ProbeConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class JwtDiagnosticsIT {
    private static final Instant NOW = Instant.parse("2030-01-01T12:00:00Z");
    private static final JwtKeyFixture KEYS = new JwtKeyFixture();
    @LocalServerPort private int port;
    @Autowired private TraceEventSink sink;
    @Autowired private JwtEncoder encoder;
    @Autowired private Probe probe;
    @MockitoSpyBean(name = "springSecurityFilterChain") private FilterChainProxy proxy;

    @DynamicPropertySource
    static void keyPaths(DynamicPropertyRegistry properties) {
        properties.add("app.jwt.public-key-path", () -> KEYS.publicPath().toString());
        properties.add("app.jwt.private-key-path", () -> KEYS.privatePath().toString());
    }

    @AfterAll
    static void removeKeys() throws IOException {
        KEYS.close();
    }

    @BeforeEach
    void observeRealFilterCallsAndContextCleanup() throws Exception {
        probe.calls.clear();
        probe.identities.clear();
        probe.cleared.clear();
        probe.workerThreads.clear();
        probe.concurrentRequests = null;
        proxy.setFilterChainDecorator((original, filters) -> {
            FilterChain next = original;
            for (int index = filters.size() - 1; index >= 0; index--) {
                var filter = filters.get(index);
                FilterChain downstream = next;
                next = (request, response) -> {
                    var http = (HttpServletRequest) request;
                    if (http.getDispatcherType() == DispatcherType.REQUEST) {
                        String id = ((HttpServletResponse) response).getHeader(TraceEventSink.HEADER);
                        probe.calls.computeIfAbsent(id, ignored -> new java.util.concurrent.CopyOnWriteArrayList<>())
                            .add(filter.getClass().getSimpleName());
                    }
                    filter.doFilter(request, response, downstream);
                };
            }
            return next;
        });
        doAnswer(invocation -> {
            try {
                return invocation.callRealMethod();
            } finally {
                var request = (HttpServletRequest) invocation.getArgument(0);
                var response = (HttpServletResponse) invocation.getArgument(1);
                if (request.getDispatcherType() == DispatcherType.REQUEST) {
                    probe.cleared.put(response.getHeader(TraceEventSink.HEADER),
                        SecurityContextHolder.getContext().getAuthentication() == null);
                    SecurityContextHolder.clearContext();
                }
            }
        }).when(proxy).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"public", "anonymous", "invalid", "denied", "allowed"})
    void diagnosticsDistinguishAuthenticationAndAuthorization(String scenario) {
        String endpoint = scenario.equals("public") ? "public" : scenario.equals("denied") ? "admin" : "user";
        String bearer = scenario.equals("invalid") ? "not.a.jwt"
            : scenario.equals("allowed") || scenario.equals("denied") ? token("user", "USER") : null;
        Response response = request(endpoint, bearer);
        int status = scenario.equals("denied") ? 403 : scenario.equals("anonymous") || scenario.equals("invalid") ? 401 : 200;
        response.then().statusCode(status).header("Set-Cookie", nullValue());
        if (status == 200) response.then().body("", equalTo(Map.of("message", endpoint)));
        if (status == 401) assertThat(response.header("WWW-Authenticate")).startsWith("Bearer");
        if (status == 403) assertThat(response.header("WWW-Authenticate")).contains("insufficient_scope");
        String id = completed(response);
        assertThat(sink.events(id)).isEqualTo(expectedTrace(scenario));
        List<String> configured = proxy.getFilterChains().get(0).getFilters().stream()
            .map(filter -> filter.getClass().getSimpleName()).toList();
        assertThat(configured).containsSubsequence("SecurityContextHolderFilter", "BearerTokenAuthenticationFilter",
            "AnonymousAuthenticationFilter", "SecurityTraceFilter", "ExceptionTranslationFilter", "AuthorizationFilter");
        assertThat(configured).doesNotContain("BasicAuthenticationFilter");
        if (scenario.equals("invalid")) {
            assertThat(probe.calls.get(id)).contains("BearerTokenAuthenticationFilter")
                .doesNotContain("SecurityTraceFilter", "ExceptionTranslationFilter", "AuthorizationFilter");
            assertThat(sink.events(id)).containsExactly("filter.entry", "security.challenge", "filter.exit");
        } else {
            assertThat(probe.calls.get(id)).isEqualTo(configured);
            assertThat(sink.events(id)).contains(scenario.equals("anonymous") || scenario.equals("public")
                ? "security.anonymous" : "security.authenticated");
            if (status != 200) assertThat(sink.events(id)).doesNotContain("interceptor.preHandle");
            if (status == 403) assertThat(sink.events(id)).contains("security.denied").doesNotContain("security.challenge");
            if (status == 401) assertThat(sink.events(id)).contains("security.challenge");
        }
        System.out.println("JWT DIAGNOSTIC " + scenario + " " + id + " " + sink.events(id)
            + " invoked=" + probe.calls.get(id) + " contextCleared=" + probe.cleared.get(id));
        System.out.println("JWT CONFIGURED FILTERS " + configured);
    }

    @ParameterizedTest
    @ValueSource(strings = {"allowed", "denied", "invalid"})
    void completedSecurityBranchDoesNotAuthenticateNextRequest(String scenario) {
        String endpoint = scenario.equals("denied") ? "admin" : "user";
        String bearer = scenario.equals("invalid") ? "not.a.jwt"
            : token(scenario.equals("allowed") ? "admin" : "user", scenario.equals("allowed") ? "ADMIN" : "USER");
        Response first = request(endpoint, bearer);
        first.then().statusCode(scenario.equals("allowed") ? 200 : scenario.equals("denied") ? 403 : 401)
            .header("Set-Cookie", nullValue());
        String firstId = completed(first);
        assertThat(sink.events(firstId)).isEqualTo(expectedTrace(scenario));
        Response anonymous = request("user", null);
        anonymous.then().statusCode(401).header("WWW-Authenticate", equalTo("Bearer"))
            .header("Set-Cookie", nullValue());
        String anonymousId = completed(anonymous);
        assertThat(firstId).isNotEqualTo(anonymousId);
        if (scenario.equals("allowed")) {
            assertThat(probe.identities.get(firstId)).isEqualTo(new Identity("admin", List.of("ROLE_ADMIN")));
        } else {
            assertThat(probe.identities).doesNotContainKey(firstId);
        }
        assertThat(probe.identities).doesNotContainKey(anonymousId);
        assertThat(sink.events(anonymousId)).isEqualTo(expectedTrace("anonymous"));
        System.out.println("JWT SEQUENTIAL " + scenario + " first=" + firstId + " next=" + anonymousId + " contextCleared=true");
    }

    @Test
    void concurrentBearerIdentitiesStayCorrelatedAndSeparate() {
        String userToken = token("user", "USER");
        String adminToken = token("admin", "ADMIN");
        probe.concurrentRequests = new CyclicBarrier(2);
        var user = CompletableFuture.supplyAsync(() -> request("user", userToken));
        var admin = CompletableFuture.supplyAsync(() -> request("admin", adminToken));
        CompletableFuture.allOf(user, admin).orTimeout(15, TimeUnit.SECONDS).join();
        Response first = user.join();
        Response second = admin.join();
        first.then().statusCode(200).body("", equalTo(Map.of("message", "user")));
        second.then().statusCode(200).body("", equalTo(Map.of("message", "admin")));
        String userId = completed(first);
        String adminId = completed(second);
        assertThat(userId).isNotEqualTo(adminId);
        assertThat(probe.workerThreads.get(userId)).isNotNull().isNotEqualTo(probe.workerThreads.get(adminId));

        assertThat(probe.identities.get(userId)).isEqualTo(new Identity("user", List.of("ROLE_USER")));
        assertThat(probe.identities.get(adminId)).isEqualTo(new Identity("admin", List.of("ROLE_ADMIN")));
        assertThat(sink.events(userId)).isEqualTo(successTrace("user", "security.authenticated"));
        assertThat(sink.events(adminId)).isEqualTo(successTrace("admin", "security.authenticated"));
        System.out.println("JWT OVERLAP user=" + userId + " admin=" + adminId + " separateWorkers=true contextCleared=true");
    }

    @ParameterizedTest
    @ValueSource(strings = {"allowed", "denied", "invalid", "anonymous"})
    void tracesAndCapturedOutputExcludeCredentialsAndKeyMaterial(String scenario, CapturedOutput output) {
        String bearer = scenario.equals("anonymous") ? null : scenario.equals("invalid") ? "not.a.jwt" : token("user", "USER");
        Response resource = request(scenario.equals("denied") ? "admin" : "user", bearer);
        resource.then().statusCode(scenario.equals("allowed") ? 200 : scenario.equals("denied") ? 403 : 401);
        String id = completed(resource);
        assertThat(sink.events(id)).isEqualTo(expectedTrace(scenario));
        Response login = given().baseUri("http://localhost").port(port).contentType("application/json")
            .body(Map.of("username", "user", "password", "user123")).when().post("/auth/login");
        login.then().statusCode(200);
        String loginId = completed(login);
        String issued = login.jsonPath().getString("accessToken");
        assertThat(issued != null && !issued.isBlank()).isTrue();
        String events = String.join(" ", sink.events(id)) + String.join(" ", sink.events(loginId));
        String logs = output.getAll();
        assertThat((bearer != null && (events.contains(bearer) || logs.contains(bearer)))
            || events.contains(issued) || logs.contains(issued)).isFalse();
        assertThat(events + logs).doesNotContain("user123", "admin123", "BEGIN PRIVATE KEY", "BEGIN RSA PRIVATE KEY");
        if (scenario.equals("allowed")) assertThat(probe.identities.get(id)).isEqualTo(new Identity("user", List.of("ROLE_USER")));
        else assertThat(probe.identities).doesNotContainKey(id);
    }

    private static List<String> expectedTrace(String scenario) {
        return switch (scenario) {
            case "public" -> successTrace("public", "security.anonymous");
            case "allowed" -> successTrace("user", "security.authenticated");
            case "invalid" -> List.of("filter.entry", "security.challenge", "filter.exit");
            case "anonymous" -> List.of("filter.entry", "security.anonymous", "security.challenge", "security.authorization.exit", "filter.exit");
            case "denied" -> List.of("filter.entry", "security.authenticated", "security.denied", "security.authorization.exit", "filter.exit");
            default -> throw new IllegalArgumentException("Unknown diagnostic scenario");
        };
    }

    private static List<String> successTrace(String endpoint, String state) {
        return List.of("filter.entry", state, "interceptor.preHandle", "controller." + endpoint,
            "interceptor.postHandle", "interceptor.afterCompletion", "security.authorization.exit", "filter.exit");
    }

    private Response request(String endpoint, String bearer) {
        var specification = given().baseUri("http://localhost").port(port);
        if (bearer != null) specification.header("Authorization", "Bearer " + bearer);
        return specification.when().get("/api/" + endpoint);
    }

    private String completed(Response response) {
        String id = response.header(TraceEventSink.HEADER);
        assertThat(id).isNotBlank();
        await().untilAsserted(() -> {
            assertThat(sink.events(id)).isNotEmpty();
            assertThat(probe.cleared.get(id)).isTrue();
        });
        return id;
    }

    private String token(String subject, String role) {
        var claims = JwtClaimsSet.builder().subject(subject)
            .issuer(com.example.securitylearning.security.JwtClaimContractValidator.ISSUER)
            .audience(List.of(com.example.securitylearning.security.JwtClaimContractValidator.AUDIENCE))
            .issuedAt(NOW).notBefore(NOW).expiresAt(NOW.plusSeconds(300)).claim("roles", List.of(role)).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256)
            .keyId("local-issuer").build(), claims)).getTokenValue();
    }

    record Identity(String name, List<String> authorities) { }

    static class Probe implements WebMvcConfigurer, HandlerInterceptor {
        final Map<String, List<String>> calls = new ConcurrentHashMap<>();
        final Map<String, Identity> identities = new ConcurrentHashMap<>();
        final Map<String, Boolean> cleared = new ConcurrentHashMap<>();
        final Map<String, Long> workerThreads = new ConcurrentHashMap<>();
        volatile CyclicBarrier concurrentRequests;

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(this);
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            if (request.getDispatcherType() == DispatcherType.REQUEST) {
                var authentication = SecurityContextHolder.getContext().getAuthentication();
                identities.put(response.getHeader(TraceEventSink.HEADER), new Identity(authentication.getName(),
                    authentication.getAuthorities().stream().map(authority -> authority.getAuthority()).sorted().toList()));
                workerThreads.put(response.getHeader(TraceEventSink.HEADER), Thread.currentThread().getId());
                var barrier = concurrentRequests;
                if (barrier != null) barrier.await(10, TimeUnit.SECONDS);
            }
            return true;
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ProbeConfiguration {
        @Bean Probe probe() { return new Probe(); }
        @Bean @Primary Clock fixedClock() { return Clock.fixed(NOW, ZoneOffset.UTC); }
    }
}
