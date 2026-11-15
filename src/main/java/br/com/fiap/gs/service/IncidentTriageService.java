package br.com.fiap.gs.service;

import br.com.fiap.gs.ai.AiTriageResult;
import br.com.fiap.gs.ai.IncidentAiService;
import br.com.fiap.gs.entity.Incident;
import br.com.fiap.gs.enums.IncidentStatus;
import br.com.fiap.gs.enums.TriageStatus;
import br.com.fiap.gs.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentTriageService {

    private final IncidentRepository repository;
    private final IncidentAiService aiService;

    @Transactional
    public void process(Long incidentId) {
        Incident incident = repository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Ocorrência não encontrada"
                ));

        if (incident.getTriageStatus() != TriageStatus.PENDING) {
            return;
        }

        if (incident.getStatus() == IncidentStatus.CANCELLED
                || incident.getStatus() == IncidentStatus.REJECTED
                || incident.getStatus() == IncidentStatus.RESOLVED) {
            return;
        }

        try {
            AiTriageResult result = aiService.analyze(incident);

            if (result == null
                    || result.urgency() == null
                    || result.summary() == null
                    || result.summary().isBlank()
                    || result.summary().length() > 300
                    || result.guidance() == null
                    || result.guidance().isBlank()
                    || result.guidance().length() > 600) {
                throw new IllegalStateException(
                        "Resposta inválida da inteligência artificial"
                );
            }

            incident.setUrgency(result.urgency());
            incident.setAiSummary(result.summary());
            incident.setAiGuidance(result.guidance());
            incident.setTriageStatus(TriageStatus.DONE);

            log.info(
                    "Triagem concluída para ocorrência {}",
                    incidentId
            );

        } catch (RuntimeException ex) {
            incident.setTriageStatus(TriageStatus.FAILED);

            log.error(
                    "Falha na triagem da ocorrência {}",
                    incidentId,
                    ex
            );
        }
    }
}
