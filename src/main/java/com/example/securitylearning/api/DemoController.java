package com.example.securitylearning.api;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DemoController {

    @GetMapping("/public")
    public Map<String, String> publicEndpoint() {
        return Map.of("message", "public");
    }

    @GetMapping("/user")
    public Map<String, String> userEndpoint() {
        return Map.of("message", "user");
    }

    @GetMapping("/admin")
    public Map<String, String> adminEndpoint() {
        return Map.of("message", "admin");
    }
}
