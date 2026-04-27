package com.inventorymanager.backend.web;

import com.inventorymanager.backend.audit.AuditService;
import com.inventorymanager.backend.auth.CurrentUser;
import com.inventorymanager.backend.common.ApiException;
import com.inventorymanager.backend.common.PageResponse;
import com.inventorymanager.backend.domain.Item;
import com.inventorymanager.backend.repository.BranchRepository;
import com.inventorymanager.backend.repository.CategoryRepository;
import com.inventorymanager.backend.repository.DepartmentRepository;
import com.inventorymanager.backend.repository.ItemRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/items")
public class ItemController {
    private final ItemRepository repository;
    private final CategoryRepository categoryRepository;
    private final DepartmentRepository departmentRepository;
    private final BranchRepository branchRepository;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    public ItemController(
            ItemRepository repository,
            CategoryRepository categoryRepository,
            DepartmentRepository departmentRepository,
            BranchRepository branchRepository,
            CurrentUser currentUser,
            AuditService auditService
    ) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
        this.departmentRepository = departmentRepository;
        this.branchRepository = branchRepository;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('get_item')")
    public PageResponse<Item> list(
            @RequestParam(required = false) Long stateId,
            @RequestParam(required = false) Long municipalityId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        org.springframework.data.jpa.domain.Specification<Item> spec = org.springframework.data.jpa.domain.Specification
                .where(com.inventorymanager.backend.repository.specification.ItemSpecification.hasState(stateId))
                .and(com.inventorymanager.backend.repository.specification.ItemSpecification.hasMunicipality(municipalityId))
                .and(com.inventorymanager.backend.repository.specification.ItemSpecification.hasBranch(branchId))
                .and(com.inventorymanager.backend.repository.specification.ItemSpecification.hasCategory(categoryId));

        return PageUtil.from(repository.findAll(spec, PageRequest.of(Math.max(0, page - 1), pageSize)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('get_item')")
    public Item get(@PathVariable Long id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Item not found"));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('create_item')")
    public Item create(@Valid @RequestBody CrudRequest.ItemUpsert request) {
        Item entity = new Item();
        mapRequestToEntity(request, entity);
        Item saved = repository.save(entity);
        auditService.commitCreate(currentUser.id(), saved);
        return saved;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('edit_item')")
    public Item update(@PathVariable Long id, @Valid @RequestBody CrudRequest.ItemUpsert request) {
        Item entity = repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Item not found"));
        mapRequestToEntity(request, entity);
        Item saved = repository.save(entity);
        auditService.commitUpdate(currentUser.id(), saved);
        return saved;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('delete_item')")
    public void delete(@PathVariable Long id) {
        Item entity = repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Item not found"));
        repository.delete(entity);
        auditService.commitDelete(currentUser.id(), entity);
    }

    private void mapRequestToEntity(CrudRequest.ItemUpsert request, Item entity) {
        entity.setName(request.name());
        entity.setQuantity(request.quantity());
        entity.setUnit(request.unit());
        entity.setObservations(request.observations());
        entity.setCharacteristicsJson(request.characteristicsJson());

        if (request.categoryId() != null) {
            entity.setCategory(categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Category not found")));
        } else {
            entity.setCategory(null);
        }

        entity.setBranch(branchRepository.findById(request.branchId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Branch not found")));

        entity.setDepartment(departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Department not found")));
    }
}
