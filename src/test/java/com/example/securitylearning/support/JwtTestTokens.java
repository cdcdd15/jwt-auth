package com.example.securitylearning.support;

import java.time.Instant;
import java.util.List;

import com.example.securitylearning.security.JwtClaimContractValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;

/** Real signed credentials for MVC tests using the production UTC clock. */
public final class JwtTestTokens {
    private JwtTestTokens() { }

    public static String admin(JwtEncoder encoder) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().subject("admin").issuer(JwtClaimContractValidator.ISSUER)
            .audience(List.of(JwtClaimContractValidator.AUDIENCE)).claim("roles", List.of("ADMIN"))
            .issuedAt(now).notBefore(now).expiresAt(now.plusSeconds(300)).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256)
            .keyId("local-issuer").build(), claims)).getTokenValue();
    }
}
