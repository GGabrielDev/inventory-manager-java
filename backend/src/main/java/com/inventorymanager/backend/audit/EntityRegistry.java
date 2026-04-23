package com.inventorymanager.backend.audit;

import com.inventorymanager.backend.domain.Category;
import com.inventorymanager.backend.domain.Department;
import com.inventorymanager.backend.domain.Item;
import com.inventorymanager.backend.domain.Municipality;
import com.inventorymanager.backend.domain.Parish;
import com.inventorymanager.backend.domain.Permission;
import com.inventorymanager.backend.domain.Role;
import com.inventorymanager.backend.domain.State;
import com.inventorymanager.backend.domain.User;
import com.inventorymanager.backend.common.ApiException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class EntityRegistry {
    private final Map<String, Class<?>> entities = Map.of(
            "user", User.class,
            "role", Role.class,
            "permission", Permission.class,
            "department", Department.class,
            "category", Category.class,
            "item", Item.class,
            "state", State.class,
            "municipality", Municipality.class,
            "parish", Parish.class
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
