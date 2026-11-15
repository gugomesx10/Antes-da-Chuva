package br.com.fiap.gs.security;

import br.com.fiap.gs.entity.User;
import br.com.fiap.gs.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenServiceTest {

    private JwtTokenService tokenService;
    private JwtDecoder decoder;

    @BeforeEach
    void setUp() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);

        SecretKey key = new SecretKeySpec(bytes, "HmacSHA256");

        JwtEncoder encoder = NimbusJwtEncoder
                .withSecretKey(key)
                .algorithm(MacAlgorithm.HS256)
                .build();

        decoder = NimbusJwtDecoder
                .withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        ((NimbusJwtDecoder) decoder).setJwtValidator(
                JwtValidators.createDefaultWithIssuer(
                        "https://antes-da-chuva.example"
                )
        );

        tokenService = new JwtTokenService(
                encoder,
                "https://antes-da-chuva.example",
                3600
        );
    }

    @Test
    void shouldGenerateValidJwt() {
        User user = new User();
        user.setId(100L);
        user.setEmail("cidadao@teste.com");
        user.setRole(Role.CIDADAO);

        String token = tokenService.generate(user);

        Jwt jwt = decoder.decode(token);

        assertEquals("cidadao@teste.com", jwt.getSubject());
        assertEquals("CIDADAO", jwt.getClaimAsString("role"));
        assertEquals("https://antes-da-chuva.example", jwt.getIssuer().toString());
        assertEquals(
                100L,
                ((Number) jwt.getClaim("userId")).longValue()
        );
        assertNotNull(jwt.getExpiresAt());
    }

    @Test
    void shouldRejectInvalidSignature() {
        User user = new User();
        user.setId(101L);
        user.setEmail("gestor@teste.com");
        user.setRole(Role.GESTOR);

        String token = tokenService.generate(user);

        byte[] anotherKey = new byte[32];
        new SecureRandom().nextBytes(anotherKey);

        JwtDecoder invalidDecoder = NimbusJwtDecoder
                .withSecretKey(
                        new SecretKeySpec(anotherKey, "HmacSHA256")
                )
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        assertThrows(
                JwtException.class,
                () -> invalidDecoder.decode(token)
        );
    }
}
