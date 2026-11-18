package br.com.fiap.gs.dto;

import br.com.fiap.gs.enums.IncidentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IncidentUpdateRequest(
        @NotNull
        IncidentStatus newStatus,

        @NotBlank
        @Size(max = 500)
        String note
) {
}
