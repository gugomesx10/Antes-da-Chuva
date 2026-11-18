package br.com.fiap.gs.mapper;

import br.com.fiap.gs.dto.IncidentRequest;
import br.com.fiap.gs.dto.IncidentResponse;
import br.com.fiap.gs.entity.Category;
import br.com.fiap.gs.entity.Incident;
import br.com.fiap.gs.entity.User;
import org.springframework.stereotype.Component;

@Component
public class IncidentMapper {

    public Incident toEntity(
            IncidentRequest request,
            User user,
            Category category
    ) {
        Incident incident = new Incident();

        incident.setUser(user);
        incident.setCategory(category);

        updateEntity(incident, request, category);

        return incident;
    }

    public void updateEntity(
            Incident incident,
            IncidentRequest request,
            Category category
    ) {
        incident.setCategory(category);
        incident.setTitle(request.title().trim());
        incident.setDescription(request.description().trim());
        incident.setAddress(request.address().trim());
        incident.setNeighborhood(request.neighborhood().trim());
        incident.setLatitude(request.latitude());
        incident.setLongitude(request.longitude());
    }

    public IncidentResponse toResponse(Incident incident) {
        return new IncidentResponse(
                incident.getId(),
                incident.getUser().getId(),
                incident.getCategory().getId(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getAddress(),
                incident.getNeighborhood(),
                incident.getLatitude(),
                incident.getLongitude(),
                incident.getStatus(),
                incident.getUrgency(),
                incident.getAiSummary(),
                incident.getAiGuidance(),
                incident.getTriageStatus(),
                incident.getCreatedAt(),
                incident.getResolvedAt()
        );
    }
}
