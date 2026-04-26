package com.inventorymanager.backend.web;

import com.inventorymanager.backend.domain.DisplacementStatus;
import com.inventorymanager.backend.domain.Item;
import com.inventorymanager.backend.domain.ItemRequestType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;

public final class CrudRequest {
    private CrudRequest() {}

    public record UserUpsert(
            @NotBlank String username,
            @NotBlank String password,
            List<Long> roleIds,
            Long branchId
    ) {}

    public record RoleUpsert(
            @NotBlank String name,
            @NotBlank String description,
            List<Long> permissionIds
    ) {}

    public record PermissionUpsert(@NotBlank String name, @NotBlank String description) {}
    public record NamedUpsert(@NotBlank String name) {}
    public record DepartmentUpsert(@NotBlank String name, @NotNull Long branchId) {}
    public record MunicipalityUpsert(@NotBlank String name, @NotNull Long stateId) {}
    public record ParishUpsert(@NotBlank String name, @NotNull Long municipalityId) {}

    public record BranchUpsert(
            @NotBlank String name,
            @NotBlank String address,
            @NotNull Long stateId,
            @NotNull Long municipalityId,
            @NotNull Long parishId
    ) {}

    public record BagItemUpsert(
            @NotNull Long itemId,
            @Min(1) Integer expectedQuantity
    ) {}

    public record BagUpsert(
            @NotBlank String name,
            @NotBlank String barcode,
            @NotNull Long branchId,
            @NotNull Long assignedDepartmentId,
            List<BagItemUpsert> expectedItems
    ) {}

    public record DisplacementUpsert(
            Long bagId,
            @NotNull Long itemId,
            @NotBlank String reason,
            @NotBlank String borrowerName,
            OffsetDateTime expectedReturnDate
    ) {}

    public record ItemUpsert(
            @NotBlank String name,
            @Min(1) Integer quantity,
            @NotNull Item.UnitType unit,
            String observations,
            String characteristicsJson,
            Long categoryId,
            @NotNull Long branchId,
            @NotNull Long departmentId
    ) {}

    public record ItemRequestEntryUpsert(
            Long itemId,
            String requestedItemName,
            Integer requestedQuantity,
            Item.UnitType requestedUnit,
            Long requestedCategoryId,
            Long sourceDepartmentId,
            Long targetDepartmentId,
            String observations,
            String characteristicsJson
    ) {}

    public record ItemRequestUpsert(
            @NotNull ItemRequestType requestType,
            @NotBlank String title,
            @NotBlank String justification,
            @NotNull List<ItemRequestEntryUpsert> entries,
            Long targetBranchId
    ) {}

    public record ItemRequestReview(
            @NotBlank String decision,
            String comment
    ) {}
}
