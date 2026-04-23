package com.inventorymanager.backend.web;

import com.inventorymanager.backend.domain.Item;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public final class CrudRequest {
    private CrudRequest() {}

    public record UserUpsert(
            @NotBlank String username,
            @NotBlank String password,
            List<Long> roleIds
    ) {}

    public record RoleUpsert(
            @NotBlank String name,
            @NotBlank String description,
            List<Long> permissionIds
    ) {}

    public record PermissionUpsert(@NotBlank String name, @NotBlank String description) {}
    public record NamedUpsert(@NotBlank String name) {}
    public record MunicipalityUpsert(@NotBlank String name, @NotNull Long stateId) {}
    public record ParishUpsert(@NotBlank String name, @NotNull Long municipalityId) {}

    public record ItemUpsert(
            @NotBlank String name,
            @Min(1) Integer quantity,
            @NotNull Item.UnitType unit,
            String observations,
            String characteristicsJson,
            Long categoryId,
            @NotNull Long departmentId
    ) {}
}
