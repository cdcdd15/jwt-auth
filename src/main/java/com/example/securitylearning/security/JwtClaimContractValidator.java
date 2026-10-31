package com.example.securitylearning.security;

import java.time.Instant;
import java.util.List;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class JwtClaimContractValidator implements OAuth2TokenValidator<Jwt> {

    public static final String ISSUER = "urn:spring-security-learning:local";
    public static final String AUDIENCE = "spring-security-learning-api";

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        var claims = jwt.getClaims();
        boolean valid = claims.get("sub") instanceof String subject && !subject.isBlank()
            && claims.get("iss") instanceof String
            && claims.get("exp") instanceof Instant
            && claims.get("aud") instanceof List<?> audience && !audience.isEmpty()
            && audience.stream().allMatch(value -> value instanceof String text && !text.isBlank())
            && audience.contains(AUDIENCE)
            && (!claims.containsKey("iat") || claims.get("iat") instanceof Instant)
            && (!claims.containsKey("nbf") || claims.get("nbf") instanceof Instant)
            && (!claims.containsKey("roles") || claims.get("roles") instanceof List<?> roles
                && roles.stream().allMatch(value -> value instanceof String role && !role.isBlank()));
        return valid ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token",
                "JWT does not satisfy the local claim contract", null));
    }
}
