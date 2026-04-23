package com.inventorymanager.backend.web;

import com.inventorymanager.backend.audit.AuditService;
import com.inventorymanager.backend.auth.CurrentUser;
import com.inventorymanager.backend.common.ApiException;
import com.inventorymanager.backend.common.PageResponse;
import com.inventorymanager.backend.domain.Category;
import com.inventorymanager.backend.repository.CategoryRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryRepository repository;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    public CategoryController(CategoryRepository repository, CurrentUser currentUser, AuditService auditService) {
        this.repository = repository;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('get_category')")
    public PageResponse<Category> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize) {
        return PageUtil.from(repository.findAll(PageRequest.of(Math.max(0, page - 1), pageSize)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('get_category')")
    public Category get(@PathVariable Long id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('create_category')")
    public Category create(@Valid @RequestBody CrudRequest.NamedUpsert request) {
        Category entity = new Category();
        entity.setName(request.name());
        Category saved = repository.save(entity);
        auditService.commitCreate(currentUser.id(), saved);
        return saved;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('edit_category')")
    public Category update(@PathVariable Long id, @Valid @RequestBody CrudRequest.NamedUpsert request) {
        Category entity = repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found"));
        entity.setName(request.name());
        Category saved = repository.save(entity);
        auditService.commitUpdate(currentUser.id(), saved);
        return saved;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('delete_category')")
    public void delete(@PathVariable Long id) {
        Category entity = repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found"));
        repository.delete(entity);
        auditService.commitDelete(currentUser.id(), entity);
    }
}
