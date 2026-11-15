package br.com.fiap.gs.dto;

public record CategoryResponse(
        Long id,
        String name,
        String description,
        Short baseWeight,
        Boolean active
) {
}
