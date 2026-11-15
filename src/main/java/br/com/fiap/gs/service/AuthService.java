package br.com.fiap.gs.service;

import br.com.fiap.gs.dto.AuthResponse;
import br.com.fiap.gs.dto.LoginRequest;
import br.com.fiap.gs.dto.RegisterRequest;
import br.com.fiap.gs.dto.UserResponse;
import br.com.fiap.gs.entity.User;
import br.com.fiap.gs.enums.Role;
import br.com.fiap.gs.exception.ConflictException;
import br.com.fiap.gs.repository.UserRepository;
import br.com.fiap.gs.security.JwtTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("E-mail já cadastrado");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPasswordHash(
                passwordEncoder.encode(request.password())
        );
        user.setRole(Role.CIDADAO);
        user.setNeighborhood(
                request.neighborhood() == null
                        ? null
                        : request.neighborhood().trim()
        );
        user.setActive(true);

        User saved = userRepository.saveAndFlush(user);

        return authenticate(saved);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository
                .findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new BadCredentialsException(
                        "Credenciais inválidas"
                ));

        if (!Boolean.TRUE.equals(user.getActive())
                || !passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )) {
            throw new BadCredentialsException(
                    "Credenciais inválidas"
            );
        }

        return authenticate(user);
    }

    public UserResponse me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException(
                    "Autenticação obrigatória"
            );
        }

        User user = userRepository
                .findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException(
                        "Usuário não autorizado"
                ));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new AccessDeniedException(
                    "Usuário inativo"
            );
        }

        return toResponse(user);
    }

    private AuthResponse authenticate(User user) {
        return new AuthResponse(
                tokenService.generate(user),
                "Bearer",
                tokenService.expiresIn(),
                toResponse(user)
        );
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getNeighborhood()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
