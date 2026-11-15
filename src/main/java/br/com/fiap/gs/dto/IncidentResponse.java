package br.com.fiap.gs.dto;

import br.com.fiap.gs.enums.IncidentStatus;
import br.com.fiap.gs.enums.TriageStatus;
import br.com.fiap.gs.enums.Urgency;

import java.math.BigDecimal;
import java.time.Instant;

public record IncidentResponse(
        Long id,
        Long userId,
        Long categoryId,
        String title,
        String description,
        String address,
        String neighborhood,
        BigDecimal latitude,
        BigDecimal longitude,
        IncidentStatus status,
        Urgency urgency,
        String aiSummary,
        String aiGuidance,
        TriageStatus triageStatus,
        Instant createdAt,
        Instant resolvedAt
) {
}
