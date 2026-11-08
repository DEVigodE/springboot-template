package com.template.auth.infrastructure.security;

import com.template.auth.application.user.TokenServicePort;
import com.template.auth.config.AppProperties;
import com.template.auth.domain.user.UserEntity;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/** HS256 access token: sub = user id, plus email and roles; 1h by default. */
@Component
public class JwtTokenService implements TokenServicePort {

    private final JwtEncoder encoder;
    private final AppProperties properties;

    public JwtTokenService(JwtEncoder encoder, AppProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    @Override
    public String generateToken(UserEntity user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(properties.jwt().accessTokenTtl()))
                .claim("email", user.getEmail())
                .claim("roles", user.getRoles())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
