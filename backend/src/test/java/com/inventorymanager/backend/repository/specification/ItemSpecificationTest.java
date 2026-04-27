package com.inventorymanager.backend.repository.specification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.inventorymanager.backend.domain.Item;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

class ItemSpecificationTest {

    private Root<Item> root;
    private CriteriaQuery<?> query;
    private CriteriaBuilder cb;
    private Path<Object> branchPath;
    private Path<Object> statePath;
    private Path<Object> idPath;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        root = mock(Root.class);
        query = mock(CriteriaQuery.class);
        cb = mock(CriteriaBuilder.class);
        branchPath = mock(Path.class);
        statePath = mock(Path.class);
        idPath = mock(Path.class);

        when(root.get("branch")).thenReturn((Path)branchPath);
        when(branchPath.get(anyString())).thenReturn((Path)statePath);
        when(statePath.get("id")).thenReturn((Path)idPath);
    }

    @Test
    void hasStateReturnsPredicate() {
        Specification<Item> spec = ItemSpecification.hasState(1L);
        spec.toPredicate(root, query, cb);
        
        verify(root).get("branch");
        verify(branchPath).get("state");
        verify(statePath).get("id");
        verify(cb).equal(idPath, 1L);
    }

    @Test
    void hasStateReturnsNullIfIdIsNull() {
        Specification<Item> spec = ItemSpecification.hasState(null);
        Predicate p = spec.toPredicate(root, query, cb);
        assertNull(p);
    }
}
