package br.com.fiap.gs.ai;

import br.com.fiap.gs.enums.Urgency;

public record AiTriageResult(
        Urgency urgency,
        String summary,
        String guidance
) {
}
