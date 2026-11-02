package com.example.securitylearning.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration(proxyBeanMethods = false)
public class JwtIssuerConfig {

    @Bean
    JwtEncoder jwtEncoder(@Value("${app.jwt.private-key-path}") String path, RSAPublicKey publicKey) throws IOException {
        try (var input = Files.newInputStream(Path.of(path))) {
            RSAPrivateKey privateKey = RsaKeyConverters.pkcs8().convert(input);
            if (privateKey == null || privateKey.getModulus().bitLength() < 2048
                || !privateKey.getModulus().equals(publicKey.getModulus())
                || privateKey instanceof RSAPrivateCrtKey crt
                    && !crt.getPublicExponent().equals(publicKey.getPublicExponent())) {
                throw new IllegalArgumentException("Signing key must be RSA 2048 or stronger and match the verification key");
            }
            var jwk = new RSAKey.Builder(publicKey).privateKey(privateKey).keyID("local-issuer").build();
            return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
        }
    }
}
