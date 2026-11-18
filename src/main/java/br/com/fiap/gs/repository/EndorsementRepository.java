package br.com.fiap.gs.repository;

import br.com.fiap.gs.entity.Endorsement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EndorsementRepository extends JpaRepository<Endorsement, Long> {

    boolean existsByIncident_IdAndUser_Id(
            Long incidentId,
            Long userId
    );

    Optional<Endorsement> findByIncident_IdAndUser_Id(
            Long incidentId,
            Long userId
    );

    long countByIncident_Id(Long incidentId);
}
