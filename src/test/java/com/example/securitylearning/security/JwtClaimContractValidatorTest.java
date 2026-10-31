package com.example.securitylearning.security;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;

class JwtClaimContractValidatorTest {

    private static final Instant NOW = Instant.parse("2030-01-01T12:00:00Z");
    private final JwtClaimContractValidator validator = new JwtClaimContractValidator();

    @Test
    void validNormalizedClaimsSatisfyContract() {
        assertThat(validator.validate(jwt(claims())).hasErrors()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing-exp", "blank-sub", "wrong-audience", "scalar-roles", "invalid-iat"})
    void invalidClaimShapeOrRequiredValueFails(String scenario) {
        var claims = claims();
        switch (scenario) {
            case "missing-exp" -> claims.remove("exp");
            case "blank-sub" -> claims.put("sub", " ");
            case "wrong-audience" -> claims.put("aud", List.of("other-api"));
            case "scalar-roles" -> claims.put("roles", "ADMIN");
            case "invalid-iat" -> claims.put("iat", "not-a-timestamp");
            default -> throw new IllegalArgumentException("Unknown test scenario");
        }
        assertThat(validator.validate(jwt(claims)).hasErrors()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "empty"})
    void absentOrEmptyRolesRemainAnAuthenticationCandidate(String scenario) {
        var claims = claims();
        if (scenario.equals("missing")) {
            claims.remove("roles");
        } else {
            claims.put("roles", List.of());
        }
        assertThat(validator.validate(jwt(claims)).hasErrors()).isFalse();
    }

    private static Map<String, Object> claims() {
        var claims = new HashMap<String, Object>();
        claims.put("sub", "user");
        claims.put("iss", JwtClaimContractValidator.ISSUER);
        claims.put("aud", List.of(JwtClaimContractValidator.AUDIENCE));
        claims.put("exp", NOW.plusSeconds(300));
        claims.put("iat", NOW);
        claims.put("nbf", NOW);
        claims.put("roles", List.of("USER"));
        return claims;
    }

    private static Jwt jwt(Map<String, Object> claims) {
        return new Jwt("test-placeholder", NOW, NOW.plusSeconds(300), Map.of("alg", "RS256"), claims);
    }
}
