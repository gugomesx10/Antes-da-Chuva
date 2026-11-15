package br.com.fiap.gs.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtRoleMappingTest {

    private final JwtAuthenticationConverter converter =
            new SecurityConfig().jwtAuthenticationConverter();

    @Test
    void shouldMapCitizenRole() {
        var authentication = converter.convert(jwt("CIDADAO"));

        assertNotNull(authentication);

        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        "ROLE_CIDADAO".equals(authority.getAuthority())
                ));

        assertTrue(authentication.getAuthorities().stream()
                .noneMatch(authority ->
                        "ROLE_GESTOR".equals(authority.getAuthority())
                ));
    }

    @Test
    void shouldMapManagerRole() {
        var authentication = converter.convert(jwt("GESTOR"));

        assertNotNull(authentication);

        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        "ROLE_GESTOR".equals(authority.getAuthority())
                ));
    }

    @Test
    void shouldNotGrantRoleWhenMissing() {
        var authentication = converter.convert(jwt(null));

        assertNotNull(authentication);

        assertTrue(authentication.getAuthorities().stream()
                .noneMatch(authority ->
                        authority.getAuthority() != null
                                && authority.getAuthority().startsWith("ROLE_")
                ));
    }

    private Jwt jwt(String role) {
        var builder = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .claim("sub", "usuario@teste.com");

        if (role != null) {
            builder.claim("role", role);
        }

        return builder.build();
    }
}
