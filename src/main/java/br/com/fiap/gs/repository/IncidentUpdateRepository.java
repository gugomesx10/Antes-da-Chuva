package br.com.fiap.gs.repository;

import br.com.fiap.gs.entity.IncidentUpdate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentUpdateRepository extends JpaRepository<IncidentUpdate, Long> {

    List<IncidentUpdate> findByIncident_IdOrderByCreatedAtAsc(
            Long incidentId
    );
}
