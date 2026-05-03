package com.inventorymanager.backend.web;

import com.inventorymanager.backend.audit.AuditService;
import com.inventorymanager.backend.auth.CurrentUser;
import com.inventorymanager.backend.common.ApiException;
import com.inventorymanager.backend.common.PageResponse;
import com.inventorymanager.backend.domain.Branch;
import com.inventorymanager.backend.repository.BranchRepository;
import com.inventorymanager.backend.repository.MunicipalityRepository;
import com.inventorymanager.backend.repository.ParishRepository;
import com.inventorymanager.backend.repository.StateRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/branches")
@Tag(name = "Branches", description = "Management of physical offices and warehouses")
public class BranchController {
    private final BranchRepository repository;
    private final StateRepository stateRepository;
    private final MunicipalityRepository municipalityRepository;
    private final ParishRepository parishRepository;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    public BranchController(
            BranchRepository repository,
            StateRepository stateRepository,
            MunicipalityRepository municipalityRepository,
            ParishRepository parishRepository,
            CurrentUser currentUser,
            AuditService auditService
    ) {
        this.repository = repository;
        this.stateRepository = stateRepository;
        this.municipalityRepository = municipalityRepository;
        this.parishRepository = parishRepository;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('get_branch')")
    @Operation(summary = "List all branches", description = "Retrieves a paginated list of physical locations. Supports hierarchical filtering.")
    public PageResponse<Branch> list(
            @Parameter(description = "Filter by state ID") @RequestParam(required = false) Long stateId,
            @Parameter(description = "Filter by municipality ID") @RequestParam(required = false) Long municipalityId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        org.springframework.data.jpa.domain.Specification<Branch> spec = org.springframework.data.jpa.domain.Specification
                .where(com.inventorymanager.backend.repository.specification.BranchSpecification.hasState(stateId))
                .and(com.inventorymanager.backend.repository.specification.BranchSpecification.hasMunicipality(municipalityId));

        return PageUtil.from(repository.findAll(spec, PageRequest.of(Math.max(0, page - 1), pageSize)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('get_branch')")
    public Branch get(@PathVariable Long id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Branch not found"));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('create_branch')")
    public Branch create(@Valid @RequestBody CrudRequest.BranchUpsert request) {
        Branch entity = new Branch();
        mapRequestToEntity(request, entity);
        Branch saved = repository.save(entity);
        auditService.commitCreate(currentUser.id(), saved);
        return saved;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('edit_branch')")
    public Branch update(@PathVariable Long id, @Valid @RequestBody CrudRequest.BranchUpsert request) {
        Branch entity = repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Branch not found"));
        mapRequestToEntity(request, entity);
        Branch saved = repository.save(entity);
        auditService.commitUpdate(currentUser.id(), saved);
        return saved;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('delete_branch')")
    public void delete(@PathVariable Long id) {
        Branch entity = repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Branch not found"));
        repository.delete(entity);
        auditService.commitDelete(currentUser.id(), entity);
    }

    private void mapRequestToEntity(CrudRequest.BranchUpsert request, Branch entity) {
        entity.setName(request.name());
        entity.setAddress(request.address());
        entity.setState(stateRepository.findById(request.stateId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "State not found")));
        entity.setMunicipality(municipalityRepository.findById(request.municipalityId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Municipality not found")));
        entity.setParish(parishRepository.findById(request.parishId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Parish not found")));
    }
}
