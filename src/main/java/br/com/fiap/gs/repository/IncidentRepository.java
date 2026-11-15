package br.com.fiap.gs.repository;

import br.com.fiap.gs.entity.Incident;
import br.com.fiap.gs.enums.IncidentStatus;
import br.com.fiap.gs.enums.TriageStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IncidentRepository extends JpaRepository<Incident, Long>,
        JpaSpecificationExecutor<Incident> {

    Page<Incident> findByUser_Id(Long userId, Pageable pageable);

    Page<Incident> findByCategory_Id(Long categoryId, Pageable pageable);

    Page<Incident> findByStatus(IncidentStatus status, Pageable pageable);

    Page<Incident> findByNeighborhoodIgnoreCase(
            String neighborhood,
            Pageable pageable
    );

    Page<Incident> findByUser_IdAndStatus(
            Long userId,
            IncidentStatus status,
            Pageable pageable
    );

    List<Incident> findTop50ByTriageStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
            TriageStatus triageStatus,
            Instant createdAt
    );
}
