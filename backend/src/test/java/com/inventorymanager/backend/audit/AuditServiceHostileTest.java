package com.inventorymanager.backend.audit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.inventorymanager.backend.repository.UserRepository;
import org.javers.core.Javers;
import org.javers.core.metamodel.object.InstanceId;
import org.javers.repository.jql.JqlQuery;
import org.javers.core.commit.CommitMetadata;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;

public class AuditServiceHostileTest {

    private Javers javers;
    private EntityRegistry entityRegistry;
    private UserRepository userRepository;
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        javers = mock(Javers.class);
        entityRegistry = mock(EntityRegistry.class);
        userRepository = mock(UserRepository.class);
        auditService = new AuditService(javers, entityRegistry, userRepository);
    }

    /**
     * VIOLATION: Integrity Loss.
     * AuditService only resolves previous state if it exists in the current page result.
     * If version N-1 is on page 2 and version N is on page 1, version N's previous state is lost.
     */
    @Test
    void testCrossPageBoundaryLosesPreviousState() {
        String entityName = "Item";
        Long entityId = 1L;
        
        when(entityRegistry.resolve(anyString())).thenAnswer(i -> Object.class);
        
        // Version 2 is the ONLY record on this page. Version 1 exists but is on "Page 2".
        CdoSnapshot v2 = createSnapshot(entityId, 2);
        when(javers.findSnapshots(any(JqlQuery.class))).thenReturn(List.of(v2));

        AuditService.PageResponse response = auditService.auditTrail(entityName, entityId, 0, 1);

        assertEquals(1, response.data().size());
        // This will FAIL because current implementation returns null if v1 is not in the same batch
        assertNotNull(response.data().get(0).previousState(), 
            "AuditService MUST resolve previous state even if it's not in the current page batch.");
    }

    @Test
    void testDeletedUserAvoidsNPlusOneQueries() {
        String entityName = "Item";
        Long entityId = 1L;
        String deletedUserId = "999";
        
        when(entityRegistry.resolve(anyString())).thenAnswer(i -> Object.class);
        
        List<CdoSnapshot> snapshots = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            CdoSnapshot s = createSnapshot(entityId, i + 1);
            when(s.getCommitMetadata().getAuthor()).thenReturn(deletedUserId);
            snapshots.add(s);
        }
        
        when(javers.findSnapshots(any(JqlQuery.class))).thenReturn(snapshots);
        when(userRepository.findAllById(any())).thenReturn(Collections.emptyList());
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        auditService.auditTrail(entityName, entityId, 0, 10);

        // This will FAIL because current implementation calls findById for every entry
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void testPaginationTotalIsAccurate() {
        when(entityRegistry.resolve(anyString())).thenAnswer(i -> Object.class);
        CdoSnapshot s = createSnapshot(1L, 1);
        when(javers.findSnapshots(any())).thenReturn(List.of(s));

        AuditService.PageResponse response = auditService.auditTrail("Item", 1L, 0, 1);

        // This will FAIL because current implementation returns -1
        assertNotEquals(-1, response.total(), "AuditService must return an accurate total, not -1.");
    }

    private CdoSnapshot createSnapshot(Long id, int version) {
        CdoSnapshot snapshot = mock(CdoSnapshot.class);
        CommitMetadata metadata = mock(CommitMetadata.class);
        InstanceId globalId = mock(InstanceId.class);
        
        when(globalId.getCdoId()).thenReturn(id);
        when(globalId.getTypeName()).thenReturn("com.example.Item");
        
        org.javers.core.metamodel.object.CdoSnapshotState state = mock(org.javers.core.metamodel.object.CdoSnapshotState.class);
        when(snapshot.getState()).thenReturn(state);
        when(state.getPropertyNames()).thenReturn(new HashSet<>());
        
        when(snapshot.getCommitMetadata()).thenReturn(metadata);
        when(snapshot.getVersion()).thenReturn((long)version);
        when(snapshot.getGlobalId()).thenReturn(globalId);
        when(metadata.getAuthor()).thenReturn("1");
        when(metadata.getCommitDateInstant()).thenReturn(Instant.now());
        when(metadata.getProperties()).thenReturn(Collections.emptyMap());
        
        return snapshot;
    }
}
