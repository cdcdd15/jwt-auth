package com.example.securitylearning.security;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class RsaJwtTest {

    private static final Instant ISSUED_AT = Instant.parse("2030-01-01T12:00:00Z");
    private static KeyPair keys;
    private static NimbusJwtEncoder encoder;

    @BeforeAll
    static void createEphemeralIssuer() {
        keys = RsaKeyGenerator.generateDevelopmentKeyPair();
        var signingKey = new RSAKey.Builder(publicKey())
            .privateKey((RSAPrivateKey) keys.getPrivate()).keyID("local-test-key").build();
        encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(signingKey)));
    }

    @Test
    void developmentKeysAreRsa2048WithMatchingPublicAndPrivateModulus() {
        assertThat(publicKey().getModulus().bitLength()).isGreaterThanOrEqualTo(2048);
        assertThat(((RSAPrivateKey) keys.getPrivate()).getModulus()).isEqualTo(publicKey().getModulus());
        System.out.println("RSA FOUNDATION keyBits=" + publicKey().getModulus().bitLength());
    }

    @Test
    void rs256TokenHasExpectedHeaderAndClaimsAndVerifiesWithPublicKey() throws Exception {
        String compact = issueToken();
        assertThat(compact.split("\\.").length).isEqualTo(3);
        SignedJWT signed = SignedJWT.parse(compact);
        assertThat(signed.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.RS256);
        assertThat(signed.getHeader().getType().toString()).isEqualTo("JWT");
        assertThat(signed.getHeader().getKeyID()).isEqualTo("local-test-key");
        var claims = signed.getJWTClaimsSet();
        assertThat(claims.getSubject()).isEqualTo("user");
        assertThat(claims.getIssuer()).isEqualTo("urn:spring-security-learning:local");
        assertThat(claims.getAudience()).containsExactly("spring-security-learning-api");
        assertThat(claims.getStringListClaim("roles")).containsExactly("USER");
        assertThat(claims.getIssueTime().toInstant()).isEqualTo(ISSUED_AT);
        assertThat(claims.getNotBeforeTime().toInstant()).isEqualTo(ISSUED_AT);
        assertThat(claims.getExpirationTime().toInstant()).isEqualTo(ISSUED_AT.plusSeconds(300));
        assertThat(claims.getClaims().keySet())
            .containsExactlyInAnyOrder("sub", "iss", "aud", "roles", "iat", "nbf", "exp");
        assertThat(verifies(signed, publicKey())).isTrue();
        System.out.println("RSA FOUNDATION algorithm=RS256 segments=3 signatureVerified=true claimsChecked=7");
    }

    @Test
    void differentPublicKeyCannotVerifySignature() throws Exception {
        var otherPublicKey = (RSAPublicKey) RsaKeyGenerator.generateDevelopmentKeyPair().getPublic();
        assertThat(verifies(SignedJWT.parse(issueToken()), otherPublicKey)).isFalse();
        System.out.println("RSA FOUNDATION wrongKeyVerified=false");
    }

    @Test
    void changedPayloadCannotVerifyOriginalSignature() throws Exception {
        String[] segments = issueToken().split("\\.");
        String changed = segments[0] + "." + Base64URL.encode("{\"sub\":\"changed\"}") + "." + segments[2];
        assertThat(verifies(SignedJWT.parse(changed), publicKey())).isFalse();
        System.out.println("RSA FOUNDATION changedPayloadVerified=false");
    }

    private static String issueToken() {
        var header = JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").keyId("local-test-key").build();
        var claims = JwtClaimsSet.builder().subject("user").issuer("urn:spring-security-learning:local")
            .audience(List.of("spring-security-learning-api")).claim("roles", List.of("USER"))
            .issuedAt(ISSUED_AT).notBefore(ISSUED_AT).expiresAt(ISSUED_AT.plusSeconds(300)).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private static RSAPublicKey publicKey() {
        return (RSAPublicKey) keys.getPublic();
    }

    private static boolean verifies(SignedJWT signed, RSAPublicKey verificationKey) throws JOSEException {
        return signed.verify(new RSASSAVerifier(verificationKey));
    }
}
