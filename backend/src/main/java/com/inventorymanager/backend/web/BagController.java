package com.inventorymanager.backend.web;

import com.inventorymanager.backend.audit.AuditService;
import com.inventorymanager.backend.auth.CurrentUser;
import com.inventorymanager.backend.common.ApiException;
import com.inventorymanager.backend.common.PageResponse;
import com.inventorymanager.backend.domain.Bag;
import com.inventorymanager.backend.repository.BagRepository;
import com.inventorymanager.backend.repository.BranchRepository;
import com.inventorymanager.backend.repository.DepartmentRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bags")
public class BagController {
    private final BagRepository repository;
    private final BranchRepository branchRepository;
    private final DepartmentRepository departmentRepository;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    public BagController(
            BagRepository repository,
            BranchRepository branchRepository,
            DepartmentRepository departmentRepository,
            CurrentUser currentUser,
            AuditService auditService
    ) {
        this.repository = repository;
        this.branchRepository = branchRepository;
        this.departmentRepository = departmentRepository;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('get_bag')")
    public PageResponse<Bag> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize) {
        return PageUtil.from(repository.findAll(PageRequest.of(Math.max(0, page - 1), pageSize)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('get_bag')")
    public Bag get(@PathVariable Long id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bag not found"));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('create_bag')")
    public Bag create(@Valid @RequestBody CrudRequest.BagUpsert request) {
        Bag entity = new Bag();
        mapRequestToEntity(request, entity);
        Bag saved = repository.save(entity);
        auditService.commitCreate(currentUser.id(), saved);
        return saved;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('edit_bag')")
    public Bag update(@PathVariable Long id, @Valid @RequestBody CrudRequest.BagUpsert request) {
        Bag entity = repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bag not found"));
        mapRequestToEntity(request, entity);
        Bag saved = repository.save(entity);
        auditService.commitUpdate(currentUser.id(), saved);
        return saved;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('delete_bag')")
    public void delete(@PathVariable Long id) {
        Bag entity = repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bag not found"));
        repository.delete(entity);
        auditService.commitDelete(currentUser.id(), entity);
    }

    private void mapRequestToEntity(CrudRequest.BagUpsert request, Bag entity) {
        entity.setName(request.name());
        entity.setBarcode(request.barcode());
        entity.setBranch(branchRepository.findById(request.branchId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Branch not found")));
        entity.setAssignedDepartment(departmentRepository.findById(request.assignedDepartmentId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Department not found")));
    }
}
