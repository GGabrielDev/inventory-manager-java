package com.inventorymanager.backend.web;

import com.inventorymanager.backend.audit.AuditService;
import com.inventorymanager.backend.auth.CurrentUser;
import com.inventorymanager.backend.common.ApiException;
import com.inventorymanager.backend.common.PageResponse;
import com.inventorymanager.backend.domain.Bag;
import com.inventorymanager.backend.domain.BagItem;
import com.inventorymanager.backend.repository.BagRepository;
import com.inventorymanager.backend.repository.BranchRepository;
import com.inventorymanager.backend.repository.DepartmentRepository;
import com.inventorymanager.backend.repository.ItemRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bags")
@Tag(name = "Bags", description = "Management of physical containers and kits")
public class BagController {
    private final BagRepository repository;
    private final BranchRepository branchRepository;
    private final DepartmentRepository departmentRepository;
    private final ItemRepository itemRepository;
    private final com.inventorymanager.backend.repository.DisplacementRepository displacementRepository;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    public BagController(
            BagRepository repository,
            BranchRepository branchRepository,
            DepartmentRepository departmentRepository,
            ItemRepository itemRepository,
            com.inventorymanager.backend.repository.DisplacementRepository displacementRepository,
            CurrentUser currentUser,
            AuditService auditService
    ) {
        this.repository = repository;
        this.branchRepository = branchRepository;
        this.departmentRepository = departmentRepository;
        this.itemRepository = itemRepository;
        this.displacementRepository = displacementRepository;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('get_bag')")
    @Operation(summary = "List all bags", description = "Retrieves a paginated list of all kits/bags in the system.")
    public PageResponse<Bag> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize) {
        return PageUtil.from(repository.findAll(PageRequest.of(Math.max(0, page - 1), pageSize)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('get_bag')")
    @Operation(summary = "Get bag by ID")
    public Bag get(@PathVariable Long id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bag not found"));
    }

    @GetMapping("/barcode/{barcode}")
    @PreAuthorize("hasAuthority('get_bag')")
    @Operation(summary = "Find bag by barcode", description = "Used by the Live Audit scanner to quickly identify a kit.")
    public Bag getByBarcode(@Parameter(description = "Unique barcode of the bag") @PathVariable String barcode) {
        return repository.findByBarcode(barcode).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bag not found"));
    }

    @GetMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('get_bag')")
    @Operation(summary = "Perform live audit", description = "Calculates expected items in a bag by subtracting active displacements.")
    public BagAuditResponse audit(@PathVariable Long id) {
        Bag bag = repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bag not found"));
        List<com.inventorymanager.backend.domain.Displacement> activeDisplacements = Optional.ofNullable(displacementRepository.findActiveByBag(id))
                .orElse(Collections.emptyList());

        // Group expected items by itemId and sum quantities
        Map<Long, Integer> expectedMap = Optional.ofNullable(bag.getExpectedItems())
                .orElse(Collections.emptySet()).stream()
                .filter(bi -> bi != null && bi.getItem() != null)
                .collect(Collectors.groupingBy(
                        bi -> bi.getItem().getId(),
                        Collectors.summingInt(bi -> bi.getExpectedQuantity() != null ? bi.getExpectedQuantity() : 0)
                ));

        // Group active displacements: count by itemId and collect first found name
        Map<Long, Long> displacedMap = new HashMap<>();
        Map<Long, String> displacedNames = new HashMap<>();
        
        activeDisplacements.stream()
                .filter(d -> d != null && d.getItem() != null)
                .forEach(d -> {
                    Long itemId = d.getItem().getId();
                    displacedMap.merge(itemId, 1L, Long::sum);
                    displacedNames.putIfAbsent(itemId, d.getItem().getName() != null ? d.getItem().getName() : "Unknown Item");
                });

        // Get all item IDs involved
        Set<Long> allItemIds = new HashSet<>(expectedMap.keySet());
        allItemIds.addAll(displacedMap.keySet());

        List<BagAuditItem> items = allItemIds.stream()
                .map(itemId -> {
                    int expected = expectedMap.getOrDefault(itemId, 0);
                    long displacedLong = displacedMap.getOrDefault(itemId, 0L);
                    int displaced = (int) Math.min(Integer.MAX_VALUE, displacedLong);
                    int remaining = Math.max(0, expected - displaced);
                    int anomalyCount = Math.max(0, displaced - expected);

                    // Find item name
                    String name = Optional.ofNullable(bag.getExpectedItems()).orElse(Collections.emptySet()).stream()
                            .filter(bi -> bi != null && bi.getItem() != null && bi.getItem().getId().equals(itemId))
                            .map(bi -> bi.getItem().getName())
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElseGet(() -> displacedNames.getOrDefault(itemId, "Unknown Item"));

                    return new BagAuditItem(itemId, name, expected, displaced, remaining, anomalyCount);
                }).collect(Collectors.toList());

        return new BagAuditResponse(bag.getId(), bag.getName(), bag.getBarcode(), items);
    }

    public record BagAuditResponse(Long id, String name, String barcode, List<BagAuditItem> items) {}
    public record BagAuditItem(Long itemId, String itemName, int intendedQuantity, int displacedQuantity, int remainingQuantity, int anomalyCount) {}

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

        if (request.expectedItems() != null) {
            Set<BagItem> bagItems = new HashSet<>();
            for (CrudRequest.BagItemUpsert itemReq : request.expectedItems()) {
                BagItem bagItem = new BagItem();
                bagItem.setBag(entity);
                bagItem.setItem(itemRepository.findById(itemReq.itemId())
                        .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Item not found: " + itemReq.itemId())));
                bagItem.setExpectedQuantity(itemReq.expectedQuantity() == null ? 1 : itemReq.expectedQuantity());
                bagItems.add(bagItem);
            }
            if (entity.getExpectedItems() == null) {
                entity.setExpectedItems(bagItems);
            } else {
                entity.getExpectedItems().clear();
                entity.getExpectedItems().addAll(bagItems);
            }
        }
    }
}
