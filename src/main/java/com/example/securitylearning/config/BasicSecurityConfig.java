package com.example.securitylearning.config;

import java.io.IOException;

import com.example.securitylearning.tracing.SecurityTraceFilter;
import com.example.securitylearning.tracing.TraceEventSink;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.security-mode", havingValue = "basic")
@EnableWebSecurity
@Import(InMemoryIdentityConfig.class)
public class BasicSecurityConfig {

    @Bean
    SecurityFilterChain basicSecurityFilterChain(HttpSecurity http, DaoAuthenticationProvider provider,
                                               BasicAuthenticationEntryPoint entryPoint, TraceEventSink sink) throws Exception {
        var deniedHandler = new AccessDeniedHandlerImpl();
        return http
            .authenticationProvider(provider)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(AbstractHttpConfigurer::disable)
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(basic -> basic.authenticationEntryPoint(entryPoint))
            .logout(AbstractHttpConfigurer::disable)
            .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(entryPoint)
                .accessDeniedHandler((request, response, exception) -> {
                    if (request.getDispatcherType() == DispatcherType.REQUEST) {
                        sink.record(request, "security.denied");
                    }
                    deniedHandler.handle(request, response, exception);
                }))
            .addFilterAfter(new SecurityTraceFilter(sink), AnonymousAuthenticationFilter.class)
            .authorizeHttpRequests(authorize -> authorize
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers("/api/public").permitAll()
                .requestMatchers("/api/admin").hasRole("ADMIN")
                .anyRequest().authenticated())
            .build();
    }

    @Bean
    BasicAuthenticationEntryPoint basicEntryPoint(TraceEventSink sink) {
        var entryPoint = new BasicAuthenticationEntryPoint() {
            @Override
            public void commence(HttpServletRequest request, HttpServletResponse response,
                                 AuthenticationException exception) throws IOException {
                if (request.getDispatcherType() == DispatcherType.REQUEST) {
                    sink.record(request, "security.challenge");
                }
                super.commence(request, response, exception);
            }
        };
        entryPoint.setRealmName("spring-security-learning");
        return entryPoint;
    }

    @Bean
    DelegatingFilterProxyRegistrationBean securityFilterRegistration() {
        var registration = new DelegatingFilterProxyRegistrationBean("springSecurityFilterChain");
        registration.setOrder(-100);
        registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ERROR);
        return registration;
    }
}
