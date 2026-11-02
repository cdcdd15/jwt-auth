package com.example.securitylearning.security;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private final AuthenticationManager manager;
    private final JwtEncoder encoder;
    private final Clock clock;

    public JwtTokenService(@Qualifier("loginAuthenticationManager") AuthenticationManager manager,
                           JwtEncoder encoder, Clock clock) {
        this.manager = manager;
        this.encoder = encoder;
        this.clock = clock;
    }

    public TokenResponse issue(String username, String password) {
        var authentication = manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(username, password));
        var roles = authentication.getAuthorities().stream().map(authority -> authority.getAuthority())
            .filter(authority -> authority.startsWith("ROLE_")).map(authority -> authority.substring(5)).toList();
        var now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        var claims = JwtClaimsSet.builder().subject(authentication.getName())
            .issuer(JwtClaimContractValidator.ISSUER).audience(List.of(JwtClaimContractValidator.AUDIENCE))
            .claim("roles", roles).issuedAt(now).notBefore(now).expiresAt(now.plusSeconds(300)).build();
        String token = encoder.encode(JwtEncoderParameters.from(
            JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").keyId("local-issuer").build(), claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", 300);
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
        @Override
        public String toString() {
            return "TokenResponse[redacted]";
        }
    }
}
