package com.example.securitylearning.config;

import com.example.securitylearning.tracing.SecurityTraceFilter;
import com.example.securitylearning.tracing.TraceEventSink;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class JwtSecurityConfig {

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        var authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http, JwtDecoder decoder,
                                               JwtAuthenticationConverter converter, TraceEventSink sink) throws Exception {
        var bearerEntryPoint = new BearerTokenAuthenticationEntryPoint();
        var bearerDeniedHandler = new BearerTokenAccessDeniedHandler();
        AuthenticationEntryPoint entryPoint = (request, response, exception) -> {
            if (request.getDispatcherType() == DispatcherType.REQUEST) {
                sink.record(request, "security.challenge");
            }
            bearerEntryPoint.commence(request, response, exception);
        };
        AccessDeniedHandler deniedHandler = (request, response, exception) -> {
            if (request.getDispatcherType() == DispatcherType.REQUEST) {
                sink.record(request, "security.denied");
            }
            bearerDeniedHandler.handle(request, response, exception);
        };
        return http
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(AbstractHttpConfigurer::disable)
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler))
            .oauth2ResourceServer(resource -> resource.authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler)
                .jwt(jwt -> jwt.decoder(decoder)
                .jwtAuthenticationConverter(converter)))
            .addFilterAfter(new SecurityTraceFilter(sink), AnonymousAuthenticationFilter.class)
            .authorizeHttpRequests(authorize -> authorize
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers("/api/public").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                .requestMatchers("/api/admin").hasRole("ADMIN")
                .anyRequest().authenticated())
            .build();
    }

    @Bean
    DelegatingFilterProxyRegistrationBean jwtSecurityFilterRegistration() {
        var registration = new DelegatingFilterProxyRegistrationBean("springSecurityFilterChain");
        registration.setOrder(-100);
        registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ERROR);
        return registration;
    }
}
