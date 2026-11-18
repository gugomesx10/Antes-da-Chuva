package br.com.fiap.gs.dto;

import br.com.fiap.gs.enums.Role;

public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
        String neighborhood
) {
}
