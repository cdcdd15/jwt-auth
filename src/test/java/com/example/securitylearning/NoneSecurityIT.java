package com.example.securitylearning;

import java.util.Map;

import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "app.security-mode=none")
class NoneSecurityIT {

    @LocalServerPort
    private int port;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private ServletContext servletContext;

    @Test
    void loginEndpointIsAbsentInNoneMode() {
        given().baseUri("http://localhost").port(port).contentType("application/json")
            .body(Map.of("username", "user", "password", "incorrect"))
            .when().post("/auth/login").then().statusCode(404);
    }

    @ParameterizedTest
    @ValueSource(strings = {"public", "user", "admin"})
    void noneHasNoSecurityInfrastructureAndIgnoresAuthorization(String endpoint) {
        given().baseUri("http://localhost").port(port)
            .header("Authorization", "Bearer deliberately-invalid")
        .when().get("/api/" + endpoint)
        .then().statusCode(200).body("", equalTo(Map.of("message", endpoint)));
        assertThat(context.getBeansOfType(UserDetailsService.class)).isEmpty();
        assertThat(context.getBeansOfType(PasswordEncoder.class)).isEmpty();
        assertThat(context.getBeansOfType(FilterChainProxy.class)).isEmpty();
        assertThat(context.getBeansOfType(SecurityFilterChain.class)).isEmpty();
        assertThat(context.containsBean("springSecurityFilterChain")).isFalse();
        assertThat(servletContext.getFilterRegistrations().values())
            .noneMatch(filter -> filter.getClassName().contains("DelegatingFilterProxy")
                || filter.getClassName().contains("FilterChainProxy"));
        System.out.println("NONE FILTERS " + servletContext.getFilterRegistrations().keySet());
    }
}
