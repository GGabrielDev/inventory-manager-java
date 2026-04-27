package com.inventorymanager.backend.repository.specification;

import com.inventorymanager.backend.domain.Branch;
import org.springframework.data.jpa.domain.Specification;

public class BranchSpecification {
    public static Specification<Branch> hasState(Long stateId) {
        return (root, query, cb) -> stateId == null ? null : 
            cb.equal(root.get("state").get("id"), stateId);
    }

    public static Specification<Branch> hasMunicipality(Long municipalityId) {
        return (root, query, cb) -> municipalityId == null ? null : 
            cb.equal(root.get("municipality").get("id"), municipalityId);
    }
}
