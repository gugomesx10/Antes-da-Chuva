package br.com.fiap.gs.dto;

import br.com.fiap.gs.enums.IncidentStatus;

import java.time.Instant;

public record IncidentUpdateResponse(
        Long id,
        Long incidentId,
        Long userId,
        IncidentStatus previousStatus,
        IncidentStatus newStatus,
        String note,
        Instant createdAt
) {
}
