package br.com.fiap.gs.service;

import br.com.fiap.gs.ai.AiTriageResult;
import br.com.fiap.gs.ai.IncidentAiService;
import br.com.fiap.gs.entity.Incident;
import br.com.fiap.gs.enums.IncidentStatus;
import br.com.fiap.gs.enums.TriageStatus;
import br.com.fiap.gs.enums.Urgency;
import br.com.fiap.gs.repository.IncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IncidentTriageServiceTest {

    private IncidentRepository repository;
    private IncidentAiService aiService;
    private IncidentTriageService service;

    @BeforeEach
    void setUp() {
        repository = mock(IncidentRepository.class);
        aiService = mock(IncidentAiService.class);
        service = new IncidentTriageService(repository, aiService);
    }

    @Test
    void shouldCompleteTriageSuccessfully() {
        Incident incident = createIncident();

        when(repository.findById(1L))
                .thenReturn(Optional.of(incident));

        when(aiService.analyze(incident))
                .thenReturn(new AiTriageResult(
                        Urgency.HIGH,
                        "Bueiro obstruído",
                        "Evite a área alagada."
                ));

        service.process(1L);

        assertEquals(TriageStatus.DONE, incident.getTriageStatus());
        assertEquals(Urgency.HIGH, incident.getUrgency());
        assertEquals("Bueiro obstruído", incident.getAiSummary());
        assertEquals("Evite a área alagada.", incident.getAiGuidance());

        verify(aiService).analyze(incident);
    }

    @Test
    void shouldFailWhenAiThrowsException() {
        Incident incident = createIncident();

        when(repository.findById(1L))
                .thenReturn(Optional.of(incident));

        when(aiService.analyze(incident))
                .thenThrow(new RuntimeException("IA indisponível"));

        service.process(1L);

        assertEquals(TriageStatus.FAILED, incident.getTriageStatus());
    }

    @Test
    void shouldIgnoreCancelledIncident() {
        Incident incident = createIncident();
        incident.setStatus(IncidentStatus.CANCELLED);

        when(repository.findById(1L))
                .thenReturn(Optional.of(incident));

        service.process(1L);

        assertEquals(TriageStatus.PENDING, incident.getTriageStatus());
        verifyNoInteractions(aiService);
    }

    private Incident createIncident() {
        Incident incident = new Incident();
        incident.setId(1L);
        incident.setStatus(IncidentStatus.OPEN);
        incident.setTriageStatus(TriageStatus.PENDING);
        return incident;
    }
}
