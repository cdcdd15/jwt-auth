package com.example.securitylearning;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.securitylearning.tracing.TraceEventSink;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "app.security-mode=basic")
@Import(BasicSecurityIT.ProbeConfiguration.class)
class BasicSecurityIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TraceEventSink sink;

    @Autowired
    private PrincipalProbe principalProbe;

    @Autowired
    private UserDetailsService users;

    @AfterEach
    void clearPrincipalProbe() {
        principalProbe.identities.clear();
    }

    @MockitoSpyBean(name = "springSecurityFilterChain")
    private FilterChainProxy proxy;

    @Test
    void publicRequestTraversesSecurityAndReachesController() throws Exception {
        String id = given().baseUri("http://localhost").port(port).accept(ContentType.JSON)
            .when().get("/api/public")
            .then().statusCode(200).body("", equalTo(Map.of("message", "public")))
            .header("Set-Cookie", nullValue())
            .extract().header(TraceEventSink.HEADER);
        assertTrace(id, List.of("filter.entry", "interceptor.preHandle", "controller.public",
            "interceptor.postHandle", "interceptor.afterCompletion", "filter.exit"));
        assertProxyInvoked();
        List<String> filters = proxy.getFilterChains().getFirst().getFilters().stream()
            .map(filter -> filter.getClass().getSimpleName()).toList();
        assertThat(filters).contains("AuthorizationFilter", "ExceptionTranslationFilter", "BasicAuthenticationFilter")
            .doesNotContain("CsrfFilter", "UsernamePasswordAuthenticationFilter");
        System.out.println("BASIC SECURITY FILTERS " + filters);
    }

    @ParameterizedTest
    @ValueSource(strings = {"user", "admin"})
    void unauthenticatedProtectedRequestReturns401WithoutApplicationHandler(String endpoint) throws Exception {
        String id = given().baseUri("http://localhost").port(port).accept(ContentType.JSON)
            .when().get("/api/" + endpoint)
            .then().statusCode(401).body("status", equalTo(401))
            .header("WWW-Authenticate", equalTo("Basic realm=\"spring-security-learning\"")).header("Set-Cookie", nullValue())
            .extract().header(TraceEventSink.HEADER);
        assertTrace(id, List.of("filter.entry", "filter.exit"));
        assertProxyInvoked();
    }

    @ParameterizedTest
    @ValueSource(strings = {"user", "admin"})
    void validCredentialsExposeExpectedPrincipalAndAuthority(String username) throws Exception {
        Response response = given().baseUri("http://localhost").port(port).accept(ContentType.JSON)
            .auth().preemptive().basic(username, username + "123")
            .when().get("/api/user");
        response.then().statusCode(200).contentType(ContentType.JSON)
            .body("", equalTo(Map.of("message", "user"))).header("Set-Cookie", nullValue());
        String id = response.header(TraceEventSink.HEADER);
        assertTrace(id, List.of("filter.entry", "interceptor.preHandle", "controller.user",
            "interceptor.postHandle", "interceptor.afterCompletion", "filter.exit"));
        Identity identity = principalProbe.identities.get(id);
        assertThat(identity).isEqualTo(new Identity(username, true,
            List.of("ROLE_" + username.toUpperCase(java.util.Locale.ROOT)), true));
        assertThat(users.loadUserByUsername(username).getPassword()).startsWith("$2a$");
        assertProxyInvoked();
        System.out.println("BASIC PRINCIPAL " + id + " " + identity);
    }

    @ParameterizedTest
    @ValueSource(strings = {"wrong-password", "unknown-user", "malformed-header", "public-invalid"})
    void invalidBasicAuthenticationReturnsChallengeWithoutApplicationHandler(String scenario) throws Exception {
        var request = given().baseUri("http://localhost").port(port).accept(ContentType.JSON);
        if (scenario.equals("malformed-header")) {
            request.header("Authorization", "Basic !!!");
        } else {
            request.auth().preemptive().basic(scenario.equals("unknown-user") ? "unknown" : "user", "incorrect");
        }
        String path = scenario.equals("public-invalid") ? "/api/public" : "/api/user";
        Response response = request.when().get(path);
        response.then().statusCode(401).contentType(ContentType.JSON)
            .header("WWW-Authenticate", equalTo("Basic realm=\"spring-security-learning\""))
            .header("Set-Cookie", nullValue()).body("status", equalTo(401));
        assertThat(response.jsonPath().getMap("")).doesNotContainKeys("message", "exception", "trace");
        String id = response.header(TraceEventSink.HEADER);
        assertTrace(id, List.of("filter.entry", "filter.exit"));
        assertThat(principalProbe.identities).doesNotContainKey(id);
        assertProxyInvoked();
    }

    private void assertTrace(String id, List<String> expected) {
        assertThat(id).isNotBlank();
        await().untilAsserted(() -> assertThat(sink.events(id)).isEqualTo(expected));
        System.out.println("BASIC TRACE " + id + " " + sink.events(id));
    }

    private void assertProxyInvoked() throws Exception {
        verify(proxy, atLeastOnce()).doFilter(any(ServletRequest.class), any(ServletResponse.class),
            any(FilterChain.class));
    }

    record Identity(String name, boolean authenticated, List<String> authorities, boolean credentialsErased) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ProbeConfiguration {

        @Bean
        PrincipalProbe principalProbe() {
            return new PrincipalProbe();
        }
    }

    static class PrincipalProbe implements WebMvcConfigurer, HandlerInterceptor {

        private final ConcurrentHashMap<String, Identity> identities = new ConcurrentHashMap<>();

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(this);
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            if (request.getDispatcherType() == DispatcherType.REQUEST) {
                var authentication = SecurityContextHolder.getContext().getAuthentication();
                identities.put(response.getHeader(TraceEventSink.HEADER), new Identity(
                    authentication.getName(), authentication.isAuthenticated(),
                    authentication.getAuthorities().stream().map(authority -> authority.getAuthority()).toList(),
                    authentication.getCredentials() == null));
            }
            return true;
        }
    }
}
