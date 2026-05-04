package com.inventorymanager.backend.repository.specification;

import com.inventorymanager.backend.domain.Department;
import org.springframework.data.jpa.domain.Specification;

public class DepartmentSpecification {

    public static Specification<Department> hasBranch(Long branchId) {
        return (root, query, cb) -> {
            if (branchId == null) return null;
            return cb.equal(root.get("branch").get("id"), branchId);
        };
    }
}
