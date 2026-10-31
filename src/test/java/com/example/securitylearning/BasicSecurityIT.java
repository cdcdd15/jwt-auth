package com.example.securitylearning;

import java.util.List;
import java.util.Map;

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
class BasicSecurityIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TraceEventSink sink;

    @MockitoSpyBean(name = "springSecurityFilterChain")
    private FilterChainProxy proxy;

    @Test
    void publicRequestTraversesSecurityAndReachesController() throws Exception {
        String id = given().baseUri("http://localhost").port(port)
            .when().get("/api/public")
            .then().statusCode(200).body("", equalTo(Map.of("message", "public")))
            .header("Set-Cookie", nullValue())
            .extract().header(TraceEventSink.HEADER);
        assertTrace(id, List.of("filter.entry", "interceptor.preHandle", "controller.public",
            "interceptor.postHandle", "interceptor.afterCompletion", "filter.exit"));
        assertProxyInvoked();
        List<String> filters = proxy.getFilterChains().getFirst().getFilters().stream()
            .map(filter -> filter.getClass().getSimpleName()).toList();
        assertThat(filters).contains("AuthorizationFilter", "ExceptionTranslationFilter")
            .doesNotContain("BasicAuthenticationFilter", "CsrfFilter", "UsernamePasswordAuthenticationFilter");
        System.out.println("BASIC SECURITY FILTERS " + filters);
    }

    @ParameterizedTest
    @ValueSource(strings = {"user", "admin"})
    void unauthenticatedProtectedRequestReturns401BeforeMvc(String endpoint) throws Exception {
        String id = given().baseUri("http://localhost").port(port)
            .when().get("/api/" + endpoint)
            .then().statusCode(401).body(equalTo(""))
            .header("WWW-Authenticate", nullValue()).header("Set-Cookie", nullValue())
            .extract().header(TraceEventSink.HEADER);
        assertTrace(id, List.of("filter.entry", "filter.exit"));
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
}
