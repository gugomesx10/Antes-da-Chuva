package br.com.fiap.gs.messaging;

import br.com.fiap.gs.entity.Incident;
import br.com.fiap.gs.enums.IncidentStatus;
import br.com.fiap.gs.enums.TriageStatus;
import br.com.fiap.gs.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PendingTriageRetryJob {

    private final IncidentRepository repository;
    private final IncidentRabbitPublisher publisher;

    @Scheduled(
            fixedDelayString = "${app.triage.retry-delay-ms:60000}",
            initialDelayString = "${app.triage.initial-delay-ms:60000}"
    )
    public void retryPending() {
        Instant cutoff = Instant.now()
                .minus(Duration.ofMinutes(5));

        List<Incident> pending =
                repository
                        .findTop50ByTriageStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
                                TriageStatus.PENDING,
                                cutoff
                        );

        for (Incident incident : pending) {
            if (incident.getStatus() == IncidentStatus.CANCELLED
                    || incident.getStatus() == IncidentStatus.RESOLVED
                    || incident.getStatus() == IncidentStatus.REJECTED) {
                continue;
            }

            log.info(
                    "Reenviando triagem da ocorrência {}",
                    incident.getId()
            );

            publisher.retry(incident.getId());
        }
    }
}
