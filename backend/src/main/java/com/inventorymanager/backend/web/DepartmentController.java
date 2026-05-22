package com.inventorymanager.backend.web;

import com.inventorymanager.backend.audit.AuditService;
import com.inventorymanager.backend.auth.CurrentUser;
import com.inventorymanager.backend.common.ApiException;
import com.inventorymanager.backend.common.PageResponse;
import com.inventorymanager.backend.domain.Department;
import com.inventorymanager.backend.repository.BranchRepository;
import com.inventorymanager.backend.repository.BagRepository;
import com.inventorymanager.backend.repository.DepartmentRepository;
import com.inventorymanager.backend.repository.ItemRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {
    private final DepartmentRepository repository;
    private final BranchRepository branchRepository;
    private final ItemRepository itemRepository;
    private final BagRepository bagRepository;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    public DepartmentController(
            DepartmentRepository repository,
            BranchRepository branchRepository,
            ItemRepository itemRepository,
            BagRepository bagRepository,
            CurrentUser currentUser,
            AuditService auditService
    ) {
        this.repository = repository;
        this.branchRepository = branchRepository;
        this.itemRepository = itemRepository;
        this.bagRepository = bagRepository;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('get_department')")
    public PageResponse<Department> list(
            @RequestParam(required = false) Long branchId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        org.springframework.data.jpa.domain.Specification<Department> spec = org.springframework.data.jpa.domain.Specification
                .where(com.inventorymanager.backend.repository.specification.DepartmentSpecification.hasBranch(branchId));

        return PageUtil.from(repository.findAll(spec, PageRequest.of(Math.max(0, page - 1), pageSize)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('get_department')")
    public Department get(@PathVariable Long id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Department not found"));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('create_department')")
    @Transactional
    public Department create(@Valid @RequestBody CrudRequest.DepartmentUpsert request) {
        Department entity = new Department();
        entity.setName(request.name());
        entity.setBranch(branchRepository.findById(request.branchId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Branch not found")));
        Department saved = repository.save(entity);
        auditService.commitCreate(currentUser.id(), saved);
        return saved;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('edit_department')")
    @Transactional
    public Department update(@PathVariable Long id, @Valid @RequestBody CrudRequest.DepartmentUpsert request) {
        Department entity = repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Department not found"));
        entity.setName(request.name());
        entity.setBranch(branchRepository.findById(request.branchId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Branch not found")));
        Department saved = repository.save(entity);
        auditService.commitUpdate(currentUser.id(), saved);
        return saved;
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('delete_department')")
    @Transactional
    public void delete(@PathVariable Long id) {
        Department entity = repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Department not found"));
        if (itemRepository.existsByDepartment_Id(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Cannot delete Department '" + entity.getName() + "' because it has items associated to it.");
        }
        if (bagRepository.existsByAssignedDepartment_Id(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Cannot delete Department '" + entity.getName() + "' because it has bags assigned to it.");
        }
        repository.delete(entity);
        auditService.commitDelete(currentUser.id(), entity);
    }
}
