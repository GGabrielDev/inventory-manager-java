package com.inventorymanager.backend.audit;

import com.inventorymanager.backend.domain.*;
import com.inventorymanager.backend.common.ApiException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class EntityRegistry {
    private final Map<String, Class<?>> entities = Map.ofEntries(
            Map.entry("user", User.class),
            Map.entry("role", Role.class),
            Map.entry("permission", Permission.class),
            Map.entry("department", Department.class),
            Map.entry("category", Category.class),
            Map.entry("item", Item.class),
            Map.entry("itemrequest", ItemRequest.class),
            Map.entry("item_request", ItemRequest.class),
            Map.entry("state", State.class),
            Map.entry("municipality", Municipality.class),
            Map.entry("parish", Parish.class),
            Map.entry("branch", Branch.class),
            Map.entry("bag", Bag.class),
            Map.entry("bag_item", BagItem.class),
            Map.entry("displacement", Displacement.class)
    );

    public Class<?> resolve(String entityName) {
        Class<?> type = entities.get(entityName.toLowerCase());
        if (type == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported entity for audit: " + entityName);
        }
        return type;
    }

    public Long entityId(Object entity) {
        try {
            return (Long) entity.getClass().getMethod("getId").invoke(entity);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to infer entity id");
        }
    }
}
