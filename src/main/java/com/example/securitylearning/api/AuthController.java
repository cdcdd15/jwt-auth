package com.example.securitylearning.api;

import java.util.Map;

import com.example.securitylearning.security.JwtTokenService;
import com.example.securitylearning.tracing.TraceEventSink;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AuthController {

    private final JwtTokenService tokens;
    private final TraceEventSink sink;

    public AuthController(JwtTokenService tokens, TraceEventSink sink) {
        this.tokens = tokens;
        this.sink = sink;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<JwtTokenService.TokenResponse> login(@RequestBody LoginRequest credentials,
                                                              HttpServletRequest request) {
        sink.record(request, "controller.login");
        if (credentials.username() == null || credentials.username().isBlank()
            || credentials.password() == null || credentials.password().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid login request");
        }
        var response = tokens.issue(credentials.username(), credentials.password());
        sink.record(request, "login.token.issued");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> authenticationFailed(HttpServletRequest request) {
        sink.record(request, "login.credentials.rejected");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).cacheControl(CacheControl.noStore())
            .body(Map.of("error", "invalid_credentials"));
    }

    public record LoginRequest(String username, String password) {
        @Override
        public String toString() {
            return "LoginRequest[redacted]";
        }
    }
}
