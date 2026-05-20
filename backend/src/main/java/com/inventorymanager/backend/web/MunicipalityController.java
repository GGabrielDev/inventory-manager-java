package com.inventorymanager.backend.web;

import com.inventorymanager.backend.audit.AuditService;
import com.inventorymanager.backend.auth.CurrentUser;
import com.inventorymanager.backend.common.ApiException;
import com.inventorymanager.backend.common.PageResponse;
import com.inventorymanager.backend.domain.Municipality;
import com.inventorymanager.backend.domain.State;
import com.inventorymanager.backend.repository.MunicipalityRepository;
import com.inventorymanager.backend.repository.StateRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/municipalities")
public class MunicipalityController {
    private final MunicipalityRepository repository;
    private final StateRepository stateRepository;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    public MunicipalityController(
            MunicipalityRepository repository,
            StateRepository stateRepository,
            CurrentUser currentUser,
            AuditService auditService
    ) {
        this.repository = repository;
        this.stateRepository = stateRepository;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('get_municipality')")
    public PageResponse<Municipality> list(
            @RequestParam(required = false) Long stateId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        org.springframework.data.jpa.domain.Specification<Municipality> spec = (root, query, cb) -> {
            if (stateId == null) return null;
            return cb.equal(root.get("state").get("id"), stateId);
        };
        return PageUtil.from(repository.findAll(spec, PageRequest.of(Math.max(0, page - 1), pageSize)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('get_municipality')")
    public Municipality get(@PathVariable Long id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Municipality not found"));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('create_municipality')")
    @Transactional
    public Municipality create(@Valid @RequestBody CrudRequest.MunicipalityUpsert request) {
        State state = stateRepository.findById(request.stateId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "State not found"));
        Municipality entity = new Municipality();
        entity.setName(request.name());
        entity.setState(state);
        Municipality saved = repository.save(entity);
        auditService.commitCreate(currentUser.id(), saved);
        return saved;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('edit_municipality')")
    @Transactional
    public Municipality update(@PathVariable Long id, @Valid @RequestBody CrudRequest.MunicipalityUpsert request) {
        Municipality entity = repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Municipality not found"));
        State state = stateRepository.findById(request.stateId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "State not found"));
        entity.setName(request.name());
        entity.setState(state);
        Municipality saved = repository.save(entity);
        auditService.commitUpdate(currentUser.id(), saved);
        return saved;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('delete_municipality')")
    @Transactional
    public void delete(@PathVariable Long id) {
        Municipality entity = repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Municipality not found"));
        repository.delete(entity);
        auditService.commitDelete(currentUser.id(), entity);
    }
}
