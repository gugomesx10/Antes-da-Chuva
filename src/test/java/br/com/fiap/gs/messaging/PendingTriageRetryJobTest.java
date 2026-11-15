package br.com.fiap.gs.messaging;

import br.com.fiap.gs.entity.Incident;
import br.com.fiap.gs.enums.IncidentStatus;
import br.com.fiap.gs.enums.TriageStatus;
import br.com.fiap.gs.repository.IncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PendingTriageRetryJobTest {

    private IncidentRepository repository;
    private IncidentRabbitPublisher publisher;
    private PendingTriageRetryJob job;

    @BeforeEach
    void setUp() {
        repository = mock(IncidentRepository.class);
        publisher = mock(IncidentRabbitPublisher.class);

        job = new PendingTriageRetryJob(
                repository,
                publisher
        );
    }

    @Test
    void shouldRetryPendingIncident() {
        Incident incident = createIncident(
                100L,
                IncidentStatus.OPEN
        );

        when(repository
                .findTop50ByTriageStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
                        eq(TriageStatus.PENDING),
                        any(Instant.class)
                ))
                .thenReturn(List.of(incident));

        job.retryPending();

        verify(publisher).retry(100L);
    }

    @Test
    void shouldIgnoreClosedIncidents() {
        Incident cancelled = createIncident(
                101L,
                IncidentStatus.CANCELLED
        );

        Incident resolved = createIncident(
                102L,
                IncidentStatus.RESOLVED
        );

        Incident rejected = createIncident(
                103L,
                IncidentStatus.REJECTED
        );

        when(repository
                .findTop50ByTriageStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
                        eq(TriageStatus.PENDING),
                        any(Instant.class)
                ))
                .thenReturn(List.of(
                        cancelled,
                        resolved,
                        rejected
                ));

        job.retryPending();

        verifyNoInteractions(publisher);
    }

    @Test
    void shouldNotRetryWhenNoPendingIncidents() {
        when(repository
                .findTop50ByTriageStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
                        eq(TriageStatus.PENDING),
                        any(Instant.class)
                ))
                .thenReturn(List.of());

        job.retryPending();

        verifyNoInteractions(publisher);
    }

    private Incident createIncident(
            Long id,
            IncidentStatus status
    ) {
        Incident incident = new Incident();
        incident.setId(id);
        incident.setStatus(status);
        incident.setTriageStatus(TriageStatus.PENDING);
        return incident;
    }
}
