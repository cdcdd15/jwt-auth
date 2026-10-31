package com.example.securitylearning.tracing;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

public class RequestTraceFilter extends OncePerRequestFilter {

    private final TraceEventSink sink;

    public RequestTraceFilter(TraceEventSink sink) {
        this.sink = sink;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain chain) throws ServletException, IOException {
        String id = sink.begin(request);
        response.setHeader(TraceEventSink.HEADER, id);
        sink.record(request, "filter.entry");
        try {
            chain.doFilter(request, response);
        } finally {
            sink.record(request, "filter.exit");
            sink.complete(request, id);
        }
    }
}
