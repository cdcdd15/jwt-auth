package com.example.securitylearning.tracing;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class SecurityTraceFilter extends OncePerRequestFilter {

    private final TraceEventSink sink;

    public SecurityTraceFilter(TraceEventSink sink) {
        this.sink = sink;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = authentication != null && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken);
        sink.record(request, authenticated ? "security.authenticated" : "security.anonymous");
        try {
            chain.doFilter(request, response);
        } finally {
            sink.record(request, "security.authorization.exit");
        }
    }
}
