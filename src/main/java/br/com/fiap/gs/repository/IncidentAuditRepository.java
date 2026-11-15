package br.com.fiap.gs.repository;

import br.com.fiap.gs.entity.IncidentAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentAuditRepository extends JpaRepository<IncidentAudit, Long> {

    List<IncidentAudit> findByIncidentIdOrderByChangedAtDesc(
            Long incidentId
    );
}
