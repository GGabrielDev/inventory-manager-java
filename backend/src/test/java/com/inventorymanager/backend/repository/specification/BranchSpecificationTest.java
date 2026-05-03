package com.inventorymanager.backend.repository.specification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.inventorymanager.backend.domain.Branch;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

class BranchSpecificationTest {

    private Root<Branch> root;
    private CriteriaQuery<?> query;
    private CriteriaBuilder cb;
    private Path<Object> statePath;
    private Path<Object> idPath;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        root = mock(Root.class);
        query = mock(CriteriaQuery.class);
        cb = mock(CriteriaBuilder.class);
        statePath = mock(Path.class);
        idPath = mock(Path.class);

        when(root.get("state")).thenReturn((Path)statePath);
        when(statePath.get("id")).thenReturn((Path)idPath);
    }

    @Test
    void hasStateReturnsPredicate() {
        Specification<Branch> spec = BranchSpecification.hasState(1L);
        spec.toPredicate(root, query, cb);
        
        verify(root).get("state");
        verify(statePath).get("id");
        verify(cb).equal(idPath, 1L);
    }

    @Test
    void hasStateReturnsNullIfIdIsNull() {
        Specification<Branch> spec = BranchSpecification.hasState(null);
        Predicate p = spec.toPredicate(root, query, cb);
        assertNull(p);
    }
}
