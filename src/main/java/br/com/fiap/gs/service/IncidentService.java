package br.com.fiap.gs.service;

import br.com.fiap.gs.dto.IncidentRequest;
import br.com.fiap.gs.dto.IncidentResponse;
import br.com.fiap.gs.entity.Category;
import br.com.fiap.gs.entity.Incident;
import br.com.fiap.gs.entity.IncidentUpdate;
import br.com.fiap.gs.entity.User;
import br.com.fiap.gs.enums.IncidentStatus;
import br.com.fiap.gs.enums.Role;
import br.com.fiap.gs.enums.TriageStatus;
import br.com.fiap.gs.exception.ConflictException;
import br.com.fiap.gs.exception.ResourceNotFoundException;
import br.com.fiap.gs.mapper.IncidentMapper;
import br.com.fiap.gs.repository.CategoryRepository;
import br.com.fiap.gs.repository.IncidentRepository;
import br.com.fiap.gs.repository.IncidentUpdateRepository;
import br.com.fiap.gs.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.fiap.gs.messaging.IncidentTriageRequested;
import org.springframework.context.ApplicationEventPublisher;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentUpdateRepository updateRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final IncidentMapper mapper;
    private final ApplicationEventPublisher eventPublisher;


    public Page<IncidentResponse> findAll(
            IncidentStatus status,
            Long categoryId,
            String neighborhood,
            Pageable pageable,
            Authentication authentication
    ) {
        currentUser(authentication);

        Specification<Incident> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (categoryId != null) {
                predicates.add(cb.equal(
                        root.get("category").get("id"),
                        categoryId
                ));
            }

            if (neighborhood != null && !neighborhood.isBlank()) {
                predicates.add(cb.equal(
                        cb.lower(root.get("neighborhood")),
                        neighborhood.trim().toLowerCase(Locale.ROOT)
                ));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };

        return incidentRepository.findAll(specification, pageable)
                .map(mapper::toResponse);
    }

    public IncidentResponse findById(
            Long id,
            Authentication authentication
    ) {
        currentUser(authentication);

        return mapper.toResponse(findIncident(id));
    }

    public Page<IncidentResponse> findMine(
            Pageable pageable,
            Authentication authentication
    ) {
        User user = currentCitizen(authentication);

        return incidentRepository.findByUser_Id(user.getId(), pageable)
                .map(mapper::toResponse);
    }

    @Transactional
    public IncidentResponse create(
            IncidentRequest request,
            Authentication authentication
    ) {
        User user = currentCitizen(authentication);
        Category category = findActiveCategory(request.categoryId());

        Incident incident = mapper.toEntity(request, user, category);

        incident.setStatus(IncidentStatus.OPEN);
        incident.setTriageStatus(TriageStatus.PENDING);

        Incident saved = incidentRepository.saveAndFlush(incident);

        eventPublisher.publishEvent(
                new IncidentTriageRequested(saved.getId())
        );

        return mapper.toResponse(saved);
    }

    @Transactional
    public IncidentResponse update(
            Long id,
            IncidentRequest request,
            Authentication authentication
    ) {
        User user = currentCitizen(authentication);
        Incident incident = findIncident(id);

        validateOwnership(incident, user);
        validateOpen(incident);

        Category category = findActiveCategory(request.categoryId());

        mapper.updateEntity(incident, request, category);

        incident.setUrgency(null);
        incident.setAiSummary(null);
        incident.setAiGuidance(null);
        incident.setTriageStatus(TriageStatus.PENDING);

        return mapper.toResponse(incidentRepository.save(incident));
    }

    @Transactional
    public void cancel(
            Long id,
            Authentication authentication
    ) {
        User user = currentCitizen(authentication);
        Incident incident = findIncident(id);

        validateOwnership(incident, user);
        validateOpen(incident);

        IncidentStatus previousStatus = incident.getStatus();

        incident.setStatus(IncidentStatus.CANCELLED);
        incidentRepository.save(incident);

        IncidentUpdate update = new IncidentUpdate();
        update.setIncident(incident);
        update.setUser(user);
        update.setPreviousStatus(previousStatus);
        update.setNewStatus(IncidentStatus.CANCELLED);
        update.setNote("Ocorrência cancelada pelo cidadão");

        updateRepository.save(update);
    }

    private Incident findIncident(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ocorrência não encontrada"
                ));
    }

    private Category findActiveCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Categoria não encontrada"
                ));

        if (!Boolean.TRUE.equals(category.getActive())) {
            throw new ConflictException("Categoria inativa");
        }

        return category;
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

    private User currentCitizen(Authentication authentication) {
        User user = currentUser(authentication);

        if (user.getRole() != Role.CIDADAO) {
            throw new AccessDeniedException(
                    "Operação permitida apenas para cidadãos"
            );
        }

        return user;
    }

    private void validateOwnership(Incident incident, User user) {
        if (!incident.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException(
                    "Você não pode modificar esta ocorrência"
            );
        }
    }

    private void validateOpen(Incident incident) {
        if (incident.getStatus() != IncidentStatus.OPEN) {
            throw new ConflictException(
                    "A ocorrência não está aberta"
            );
        }
    }
}
