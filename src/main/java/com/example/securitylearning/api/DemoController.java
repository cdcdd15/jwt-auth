package com.example.securitylearning.api;

import java.util.Map;

import com.example.securitylearning.tracing.TraceEventSink;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DemoController {

    private final TraceEventSink sink;

    public DemoController(TraceEventSink sink) {
        this.sink = sink;
    }

    @GetMapping("/public")
    public Map<String, String> publicEndpoint(HttpServletRequest request) {
        sink.record(request, "controller.public");
        return Map.of("message", "public");
    }

    @GetMapping("/user")
    public Map<String, String> userEndpoint(HttpServletRequest request) {
        sink.record(request, "controller.user");
        return Map.of("message", "user");
    }

    @GetMapping("/admin")
    public Map<String, String> adminEndpoint(HttpServletRequest request) {
        sink.record(request, "controller.admin");
        return Map.of("message", "admin");
    }
}
