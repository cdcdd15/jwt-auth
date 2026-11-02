package com.example.securitylearning;

import java.io.IOException;
import com.example.securitylearning.support.JwtKeyFixture;
import com.example.securitylearning.support.JwtTestTokens;
import org.junit.jupiter.api.AfterAll;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;


import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.example.securitylearning.tracing.TraceEventSink;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ErrorPathIT.ErrorTestConfiguration.class)
class ErrorPathIT {

    private static final JwtKeyFixture KEYS = new JwtKeyFixture();
    @Autowired
    private JwtEncoder encoder;

    @DynamicPropertySource
    static void keyPaths(DynamicPropertyRegistry properties) {
        properties.add("app.jwt.public-key-path", () -> KEYS.publicPath().toString());
        properties.add("app.jwt.private-key-path", () -> KEYS.privatePath().toString());
    }

    @AfterAll
    static void removeKeys() throws IOException {
        KEYS.close();
    }


    @LocalServerPort
    private int port;

    @Autowired
    private TraceEventSink sink;

    @Autowired
    private DispatchProbe probe;

    @AfterEach
    void clearProbe() {
        probe.events.clear();
    }

    @Test
    void unmappedUrlReturns404ThroughResourceHandlerAndErrorDispatch() {
        Response response = get("/api/not-mapped");
        assertError(response, 404, "Not Found", "/api/not-mapped");
        assertTrace(response, List.of("filter.entry", "security.authenticated", "interceptor.preHandle",
            "interceptor.afterCompletion", "security.authorization.exit", "filter.exit"), List.of(
            "REQUEST:ResourceHttpRequestHandler:preHandle",
            "REQUEST:ResourceHttpRequestHandler:afterCompletion:none",
            "ERROR:BasicErrorController#error:preHandle",
            "ERROR:BasicErrorController#error:postHandle",
            "ERROR:BasicErrorController#error:afterCompletion:none"));
    }

    @Test
    void controllerExceptionReturns500AndNextRequestHasFreshTrace() {
        Response failed = get("/test/throw");
        assertError(failed, 500, "Internal Server Error", "/test/throw");
        assertTrace(failed, List.of("filter.entry", "security.authenticated", "interceptor.preHandle",
            "controller.throw", "interceptor.afterCompletion", "security.authorization.exit", "filter.exit"), List.of(
            "REQUEST:ThrowingController#fail:preHandle",
            "REQUEST:ThrowingController#fail:afterCompletion:IllegalStateException",
            "ERROR:BasicErrorController#error:preHandle",
            "ERROR:BasicErrorController#error:postHandle",
            "ERROR:BasicErrorController#error:afterCompletion:none"));

        Response successful = get("/api/public");
        successful.then().statusCode(200).contentType(ContentType.JSON)
            .body("message", equalTo("public"));
        assertThat(successful.header(TraceEventSink.HEADER))
            .isNotEqualTo(failed.header(TraceEventSink.HEADER));
        assertTrace(successful, List.of("filter.entry", "security.authenticated", "interceptor.preHandle",
            "controller.public", "interceptor.postHandle", "interceptor.afterCompletion",
            "security.authorization.exit", "filter.exit"), List.of("REQUEST:DemoController#publicEndpoint:preHandle",
            "REQUEST:DemoController#publicEndpoint:postHandle",
            "REQUEST:DemoController#publicEndpoint:afterCompletion:none"));
    }

    private Response get(String path) {
        return given().baseUri("http://localhost").port(port).accept(ContentType.JSON)
            .header("Authorization", "Bearer " + JwtTestTokens.admin(encoder))
            .when().get(path);
    }

    private void assertError(Response response, int status, String error, String path) {
        response.then().statusCode(status).contentType(ContentType.JSON)
            .body("status", equalTo(status)).body("error", equalTo(error))
            .body("path", equalTo(path));
        assertThat(response.jsonPath().getMap("")).doesNotContainKeys("exception", "trace", "message");
    }

    private void assertTrace(Response response, List<String> expected, List<String> dispatches) {
        String id = response.header(TraceEventSink.HEADER);
        assertThat(id).isNotBlank();
        await().untilAsserted(() -> {
            assertThat(sink.events(id)).isEqualTo(expected);
            assertThat(probe.events.get(id)).containsExactlyElementsOf(dispatches);
        });
        System.out.println("ERROR-PATH TRACE " + id + " " + sink.events(id));
        System.out.println("DISPATCH TRACE " + id + " " + probe.events.get(id));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ErrorTestConfiguration {

        @Bean
        ThrowingController throwingController(TraceEventSink sink) {
            return new ThrowingController(sink);
        }

        @Bean
        DispatchProbe dispatchProbe() {
            return new DispatchProbe();
        }
    }

    @TestComponent
    @RestController
    static class ThrowingController {

        private final TraceEventSink sink;

        ThrowingController(TraceEventSink sink) {
            this.sink = sink;
        }

        @GetMapping("/test/throw")
        public void fail(HttpServletRequest request) {
            sink.record(request, "controller.throw");
            throw new IllegalStateException("Deliberate test failure");
        }
    }

    static class DispatchProbe implements WebMvcConfigurer, HandlerInterceptor {

        private final ConcurrentHashMap<String, CopyOnWriteArrayList<String>> events =
            new ConcurrentHashMap<>();

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(this);
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            record(request, response, handler, "preHandle");
            return true;
        }

        @Override
        public void postHandle(HttpServletRequest request, HttpServletResponse response,
                               Object handler, ModelAndView modelAndView) {
            record(request, response, handler, "postHandle");
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                    Object handler, Exception exception) {
            record(request, response, handler, "afterCompletion:"
                + (exception == null ? "none" : exception.getClass().getSimpleName()));
        }

        private void record(HttpServletRequest request, HttpServletResponse response,
                            Object handler, String event) {
            String handlerName = handler instanceof HandlerMethod method
                ? method.getBeanType().getSimpleName() + "#" + method.getMethod().getName()
                : handler.getClass().getSimpleName();
            String id = response.getHeader(TraceEventSink.HEADER);
            events.computeIfAbsent(id, key -> new CopyOnWriteArrayList<>())
                .add(request.getDispatcherType() + ":" + handlerName + ":" + event);
        }
    }
}
