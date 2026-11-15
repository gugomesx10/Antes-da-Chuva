package br.com.fiap.gs.security;

import br.com.fiap.gs.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class JwtTokenService {

    private final JwtEncoder encoder;
    private final String issuer;
    private final long expirationSeconds;

    public JwtTokenService(
            JwtEncoder encoder,
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.expiration-seconds}") long expirationSeconds
    ) {
        this.encoder = encoder;
        this.issuer = issuer;
        this.expirationSeconds = expirationSeconds;
    }

    public String generate(User user) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expirationSeconds))
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        return encoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }

    public long expiresIn() {
        return expirationSeconds;
    }
}
