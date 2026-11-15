package br.com.fiap.gs.controller;

import br.com.fiap.gs.dto.IncidentUpdateRequest;
import br.com.fiap.gs.dto.IncidentUpdateResponse;
import br.com.fiap.gs.service.IncidentUpdateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.List;

@RestController
@RequestMapping("/incidents/{incidentId}/updates")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class IncidentUpdateController {

    private final IncidentUpdateService service;

    @GetMapping
    public ResponseEntity<List<IncidentUpdateResponse>> findAll(
            @PathVariable Long incidentId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                service.findAll(incidentId, authentication)
        );
    }

    @PostMapping
    @PreAuthorize("hasRole('GESTOR')")
    public ResponseEntity<IncidentUpdateResponse> changeStatus(
            @PathVariable Long incidentId,
            @Valid @RequestBody IncidentUpdateRequest request,
            Authentication authentication
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.changeStatus(
                        incidentId,
                        request,
                        authentication
                ));
    }
}
