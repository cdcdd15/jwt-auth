package com.example.securitylearning.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;

import com.example.securitylearning.security.JwtClaimContractValidator;
import com.example.securitylearning.security.StrictJwtDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.security-mode", havingValue = "jwt")
public class JwtDecoderConfig {

    @Bean
    RSAPublicKey jwtVerificationKey(@Value("${app.jwt.public-key-path}") String path) throws IOException {
        try (var input = Files.newInputStream(Path.of(path))) {
            RSAPublicKey key = RsaKeyConverters.x509().convert(input);
            if (key == null || key.getModulus().bitLength() < 2048) {
                throw new IllegalArgumentException("RSA verification key must be at least 2048 bits");
            }
            return key;
        }
    }

    @Bean
    Clock jwtValidationClock() {
        return Clock.systemUTC();
    }

    @Bean
    JwtDecoder jwtDecoder(RSAPublicKey jwtVerificationKey, Clock clock) {
        var decoder = NimbusJwtDecoder.withPublicKey(jwtVerificationKey)
            .signatureAlgorithm(SignatureAlgorithm.RS256).build();
        var timestamps = new JwtTimestampValidator(Duration.ofSeconds(60));
        timestamps.setClock(clock);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            new JwtClaimContractValidator(), new JwtIssuerValidator(JwtClaimContractValidator.ISSUER), timestamps));
        return new StrictJwtDecoder(decoder);
    }
}
