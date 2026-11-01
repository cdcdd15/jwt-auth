package com.example.securitylearning.security;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/** Preserves the local JSON type contract after trusted Nimbus verification. */
public final class StrictJwtDecoder implements JwtDecoder {
    private final JwtDecoder delegate;

    public StrictJwtDecoder(JwtDecoder delegate) {
        this.delegate = delegate;
    }

    @Override
    public Jwt decode(String token) {
        Jwt jwt = delegate.decode(token);
        try {
            Map<String, Object> raw = SignedJWT.parse(token).getPayload().toJSONObject();
            if (raw == null || !(raw.get("sub") instanceof String subject) || subject.isBlank()
                || !(raw.get("iss") instanceof String)
                || !audienceIsValid(raw.get("aud"))
                || !(raw.get("exp") instanceof Number)
                || !optionalNumericDate(raw, "iat") || !optionalNumericDate(raw, "nbf")
                || raw.containsKey("roles") && (!(raw.get("roles") instanceof List<?> roles)
                    || !roles.stream().allMatch(value -> value instanceof String role && !role.isBlank()))) {
                throw new BadJwtException("JWT does not satisfy the local JSON claim contract");
            }
            return jwt;
        } catch (ParseException exception) {
            throw new BadJwtException("Cannot inspect verified JWT claims", exception);
        }
    }

    private static boolean optionalNumericDate(Map<String, Object> claims, String name) {
        return !claims.containsKey(name) || claims.get(name) instanceof Number;
    }

    private static boolean audienceIsValid(Object audience) {
        return audience instanceof String text && !text.isBlank()
            || audience instanceof List<?> values && !values.isEmpty()
                && values.stream().allMatch(value -> value instanceof String text && !text.isBlank());
    }
}
