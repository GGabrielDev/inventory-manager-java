package com.inventorymanager.backend.repository.specification;

import com.inventorymanager.backend.domain.Item;
import org.springframework.data.jpa.domain.Specification;

public class ItemSpecification {
    public static Specification<Item> hasState(Long stateId) {
        return (root, query, cb) -> stateId == null ? null : 
            cb.equal(root.get("branch").get("state").get("id"), stateId);
    }

    public static Specification<Item> hasMunicipality(Long municipalityId) {
        return (root, query, cb) -> municipalityId == null ? null : 
            cb.equal(root.get("branch").get("municipality").get("id"), municipalityId);
    }

    public static Specification<Item> hasBranch(Long branchId) {
        return (root, query, cb) -> branchId == null ? null : 
            cb.equal(root.get("branch").get("id"), branchId);
    }

    public static Specification<Item> hasCategory(Long categoryId) {
        return (root, query, cb) -> categoryId == null ? null : 
            cb.equal(root.get("category").get("id"), categoryId);
    }
}
