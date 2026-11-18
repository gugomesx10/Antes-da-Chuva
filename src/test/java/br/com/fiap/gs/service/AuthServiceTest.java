package br.com.fiap.gs.service;

import br.com.fiap.gs.dto.AuthResponse;
import br.com.fiap.gs.dto.LoginRequest;
import br.com.fiap.gs.dto.RegisterRequest;
import br.com.fiap.gs.entity.User;
import br.com.fiap.gs.enums.Role;
import br.com.fiap.gs.exception.ConflictException;
import br.com.fiap.gs.repository.UserRepository;
import br.com.fiap.gs.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private JwtTokenService tokenService;
    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        tokenService = mock(JwtTokenService.class);
        passwordEncoder = new BCryptPasswordEncoder();

        authService = new AuthService(
                userRepository,
                passwordEncoder,
                tokenService
        );
    }

    @Test
    void shouldRegisterCitizenSuccessfully() {
        RegisterRequest request = new RegisterRequest(
                "João Silva",
                "JOAO@EXEMPLO.COM",
                "SenhaTeste@2026",
                "Mooca"
        );

        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setId(100L);
                    return user;
                });

        when(tokenService.generate(any(User.class)))
                .thenReturn("token-de-teste");

        when(tokenService.expiresIn())
                .thenReturn(3600L);

        AuthResponse response = authService.register(request);

        assertEquals("joao@exemplo.com", response.user().email());
        assertEquals(Role.CIDADAO, response.user().role());
        assertEquals("token-de-teste", response.accessToken());
        assertEquals(3600L, response.expiresIn());

        verify(userRepository).saveAndFlush(
                argThat(user ->
                        user.getRole() == Role.CIDADAO
                                && passwordEncoder.matches(
                                "SenhaTeste@2026",
                                user.getPasswordHash()
                        )
                )
        );
    }

    @Test
    void shouldRejectDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase(
                "joao@exemplo.com"
        )).thenReturn(true);

        RegisterRequest request = new RegisterRequest(
                "João Silva",
                "joao@exemplo.com",
                "SenhaTeste@2026",
                "Mooca"
        );

        assertThrows(
                ConflictException.class,
                () -> authService.register(request)
        );

        verify(userRepository, never())
                .saveAndFlush(any(User.class));
    }

    @Test
    void shouldLoginSuccessfully() {
        User user = createUser();

        when(userRepository.findByEmailIgnoreCase(
                "joao@exemplo.com"
        )).thenReturn(Optional.of(user));

        when(tokenService.generate(user))
                .thenReturn("token-valido");

        when(tokenService.expiresIn())
                .thenReturn(3600L);

        AuthResponse response = authService.login(
                new LoginRequest(
                        "joao@exemplo.com",
                        "SenhaTeste@2026"
                )
        );

        assertEquals("token-valido", response.accessToken());
        assertEquals(Role.CIDADAO, response.user().role());
    }

    @Test
    void shouldRejectInvalidPassword() {
        User user = createUser();

        when(userRepository.findByEmailIgnoreCase(
                "joao@exemplo.com"
        )).thenReturn(Optional.of(user));

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(
                        new LoginRequest(
                                "joao@exemplo.com",
                                "senha-incorreta"
                        )
                )
        );

        verifyNoInteractions(tokenService);
    }

    @Test
    void shouldRejectInactiveUser() {
        User user = createUser();
        user.setActive(false);

        when(userRepository.findByEmailIgnoreCase(
                "joao@exemplo.com"
        )).thenReturn(Optional.of(user));

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(
                        new LoginRequest(
                                "joao@exemplo.com",
                                "SenhaTeste@2026"
                        )
                )
        );

        verifyNoInteractions(tokenService);
    }

    private User createUser() {
        User user = new User();
        user.setId(100L);
        user.setName("João Silva");
        user.setEmail("joao@exemplo.com");
        user.setPasswordHash(
                passwordEncoder.encode("SenhaTeste@2026")
        );
        user.setRole(Role.CIDADAO);
        user.setActive(true);
        user.setNeighborhood("Mooca");

        return user;
    }
}
