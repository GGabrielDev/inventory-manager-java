package com.inventorymanager.backend.web;

import com.inventorymanager.backend.audit.AuditService;
import com.inventorymanager.backend.auth.CurrentUser;
import com.inventorymanager.backend.common.ApiException;
import com.inventorymanager.backend.common.PageResponse;
import com.inventorymanager.backend.domain.Municipality;
import com.inventorymanager.backend.domain.Parish;
import com.inventorymanager.backend.repository.MunicipalityRepository;
import com.inventorymanager.backend.repository.ParishRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parishes")
public class ParishController {
    private final ParishRepository repository;
    private final MunicipalityRepository municipalityRepository;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    public ParishController(
            ParishRepository repository,
            MunicipalityRepository municipalityRepository,
            CurrentUser currentUser,
            AuditService auditService
    ) {
        this.repository = repository;
        this.municipalityRepository = municipalityRepository;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('get_parish')")
    public PageResponse<Parish> list(
            @RequestParam(required = false) Long municipalityId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        org.springframework.data.jpa.domain.Specification<Parish> spec = (root, query, cb) -> {
            if (municipalityId == null) return null;
            return cb.equal(root.get("municipality").get("id"), municipalityId);
        };
        return PageUtil.from(repository.findAll(spec, PageRequest.of(Math.max(0, page - 1), pageSize)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('get_parish')")
    public Parish get(@PathVariable Long id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Parish not found"));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('create_parish')")
    public Parish create(@Valid @RequestBody CrudRequest.ParishUpsert request) {
        Municipality municipality = municipalityRepository.findById(request.municipalityId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Municipality not found"));
        Parish entity = new Parish();
        entity.setName(request.name());
        entity.setMunicipality(municipality);
        Parish saved = repository.save(entity);
        auditService.commitCreate(currentUser.id(), saved);
        return saved;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('edit_parish')")
    public Parish update(@PathVariable Long id, @Valid @RequestBody CrudRequest.ParishUpsert request) {
        Parish entity = repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Parish not found"));
        Municipality municipality = municipalityRepository.findById(request.municipalityId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Municipality not found"));
        entity.setName(request.name());
        entity.setMunicipality(municipality);
        Parish saved = repository.save(entity);
        auditService.commitUpdate(currentUser.id(), saved);
        return saved;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('delete_parish')")
    public void delete(@PathVariable Long id) {
        Parish entity = repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Parish not found"));
        repository.delete(entity);
        auditService.commitDelete(currentUser.id(), entity);
    }
}
