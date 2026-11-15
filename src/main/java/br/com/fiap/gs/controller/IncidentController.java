package br.com.fiap.gs.controller;

import br.com.fiap.gs.dto.IncidentRequest;
import br.com.fiap.gs.dto.IncidentResponse;
import br.com.fiap.gs.enums.IncidentStatus;
import br.com.fiap.gs.service.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.net.URI;

@RestController
@RequestMapping("/incidents")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class IncidentController {

    private final IncidentService service;

    @GetMapping
    public ResponseEntity<Page<IncidentResponse>> findAll(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String neighborhood,
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                service.findAll(
                        status,
                        categoryId,
                        neighborhood,
                        pageable,
                        authentication
                )
        );
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('CIDADAO')")
    public ResponseEntity<Page<IncidentResponse>> findMine(
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                service.findMine(pageable, authentication)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentResponse> findById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                service.findById(id, authentication)
        );
    }

    @PostMapping
    @PreAuthorize("hasRole('CIDADAO')")
    public ResponseEntity<IncidentResponse> create(
            @Valid @RequestBody IncidentRequest request,
            Authentication authentication
    ) {
        IncidentResponse response = service.create(
                request,
                authentication
        );

        return ResponseEntity
                .created(URI.create("/incidents/" + response.id()))
                .body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CIDADAO')")
    public ResponseEntity<IncidentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody IncidentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                service.update(id, request, authentication)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CIDADAO')")
    public ResponseEntity<Void> cancel(
            @PathVariable Long id,
            Authentication authentication
    ) {
        service.cancel(id, authentication);
        return ResponseEntity.noContent().build();
    }
}
