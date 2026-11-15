package br.com.fiap.gs.service;

import br.com.fiap.gs.dto.IncidentUpdateRequest;
import br.com.fiap.gs.dto.IncidentUpdateResponse;
import br.com.fiap.gs.entity.Incident;
import br.com.fiap.gs.entity.IncidentUpdate;
import br.com.fiap.gs.entity.User;
import br.com.fiap.gs.enums.IncidentStatus;
import br.com.fiap.gs.enums.Role;
import br.com.fiap.gs.exception.ConflictException;
import br.com.fiap.gs.exception.ResourceNotFoundException;
import br.com.fiap.gs.repository.IncidentRepository;
import br.com.fiap.gs.repository.IncidentUpdateRepository;
import br.com.fiap.gs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncidentUpdateService {

    private static final Map<IncidentStatus, Set<IncidentStatus>> TRANSITIONS =
            Map.of(
                    IncidentStatus.OPEN,
                    Set.of(IncidentStatus.IN_REVIEW, IncidentStatus.REJECTED),

                    IncidentStatus.IN_REVIEW,
                    Set.of(IncidentStatus.IN_PROGRESS, IncidentStatus.REJECTED),

                    IncidentStatus.IN_PROGRESS,
                    Set.of(IncidentStatus.RESOLVED, IncidentStatus.REJECTED)
            );

    private final IncidentRepository incidentRepository;
    private final IncidentUpdateRepository updateRepository;
    private final UserRepository userRepository;

    public List<IncidentUpdateResponse> findAll(
            Long incidentId,
            Authentication authentication
    ) {
        currentUser(authentication);

        if (!incidentRepository.existsById(incidentId)) {
            throw new ResourceNotFoundException("Ocorrência não encontrada");
        }

        return updateRepository.findByIncident_IdOrderByCreatedAtAsc(incidentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public IncidentUpdateResponse changeStatus(
            Long incidentId,
            IncidentUpdateRequest request,
            Authentication authentication
    ) {
        User gestor = currentUser(authentication);

        if (gestor.getRole() != Role.GESTOR) {
            throw new AccessDeniedException("Acesso permitido apenas para gestores");
        }

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ocorrência não encontrada"
                ));

        IncidentStatus previousStatus = incident.getStatus();
        IncidentStatus newStatus = request.newStatus();

        if (!TRANSITIONS
                .getOrDefault(previousStatus, Set.of())
                .contains(newStatus)) {
            throw new ConflictException("Transição de status não permitida");
        }

        incident.setStatus(newStatus);

        if (newStatus == IncidentStatus.RESOLVED) {
            incident.setResolvedAt(Instant.now());
        }

        IncidentUpdate update = new IncidentUpdate();
        update.setIncident(incident);
        update.setUser(gestor);
        update.setPreviousStatus(previousStatus);
        update.setNewStatus(newStatus);
        update.setNote(request.note().trim());

        IncidentUpdate saved = updateRepository.saveAndFlush(update);

        return toResponse(saved);
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Autenticação obrigatória");
        }

        User user = userRepository
                .findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException(
                        "Usuário não autorizado"
                ));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new AccessDeniedException("Usuário inativo");
        }

        return user;
    }

    private IncidentUpdateResponse toResponse(IncidentUpdate update) {
        return new IncidentUpdateResponse(
                update.getId(),
                update.getIncident().getId(),
                update.getUser().getId(),
                update.getPreviousStatus(),
                update.getNewStatus(),
                update.getNote(),
                update.getCreatedAt()
        );
    }
}
