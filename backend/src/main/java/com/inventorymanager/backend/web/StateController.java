package com.inventorymanager.backend.web;

import com.inventorymanager.backend.audit.AuditService;
import com.inventorymanager.backend.auth.CurrentUser;
import com.inventorymanager.backend.common.ApiException;
import com.inventorymanager.backend.common.PageResponse;
import com.inventorymanager.backend.domain.State;
import com.inventorymanager.backend.repository.StateRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/states")
public class StateController {
    private final StateRepository repository;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    public StateController(StateRepository repository, CurrentUser currentUser, AuditService auditService) {
        this.repository = repository;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('get_state')")
    public PageResponse<State> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize) {
        return PageUtil.from(repository.findAll(PageRequest.of(Math.max(0, page - 1), pageSize)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('get_state')")
    public State get(@PathVariable Long id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "State not found"));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('create_state')")
    public State create(@Valid @RequestBody CrudRequest.NamedUpsert request) {
        State entity = new State();
        entity.setName(request.name());
        State saved = repository.save(entity);
        auditService.commitCreate(currentUser.id(), saved);
        return saved;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('edit_state')")
    public State update(@PathVariable Long id, @Valid @RequestBody CrudRequest.NamedUpsert request) {
        State entity = repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "State not found"));
        entity.setName(request.name());
        State saved = repository.save(entity);
        auditService.commitUpdate(currentUser.id(), saved);
        return saved;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('delete_state')")
    public void delete(@PathVariable Long id) {
        State entity = repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "State not found"));
        repository.delete(entity);
        auditService.commitDelete(currentUser.id(), entity);
    }
}
