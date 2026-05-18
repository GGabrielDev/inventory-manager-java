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

public class AuditServiceAdversaryTest {

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

    @Test
    void testPreviousStateResolutionOnlyOccursInMemory() {
        String entityName = "Item";
        Long entityId = 1L;
        int pageSize = 10;
        
        when(entityRegistry.resolve(anyString())).thenAnswer(i -> Object.class);
        
        List<CdoSnapshot> snapshots = new ArrayList<>();
        // Create a sequence of snapshots: version 1, 2, 3...
        for (int i = 0; i < pageSize; i++) {
            snapshots.add(createSnapshot(entityId, i + 1));
        }
        
        // Reverse so we see 10, 9, 8...
        Collections.reverse(snapshots);
        
        when(javers.findSnapshots(any(JqlQuery.class))).thenReturn((List)snapshots);

        auditService.auditTrail(entityName, entityId, 0, pageSize);

        // Verification: Exactly ONE query. Previous versions are in the same batch, so no N+1 lookups.
        verify(javers, times(1)).findSnapshots(any(JqlQuery.class));
    }

    @Test
    void testPaginationTotalEstimation() {
        int pageSize = 5;
        when(entityRegistry.resolve(anyString())).thenAnswer(i -> Object.class);
        
        List<CdoSnapshot> snapshots = new ArrayList<>();
        for (int i = 0; i < pageSize; i++) snapshots.add(createSnapshot(1L, 1));
        
        when(javers.findSnapshots(any())).thenReturn((List)snapshots);

        AuditService.PageResponse response = auditService.auditTrail("Item", 1L, 0, pageSize);

        // total is -1 if snapshots.size() == pageSize (potentially more)
        assertEquals(-1, response.total());
    }

    private CdoSnapshot createSnapshot(Long id, int version) {
        CdoSnapshot snapshot = mock(CdoSnapshot.class);
        CommitMetadata metadata = mock(CommitMetadata.class);
        InstanceId globalId = mock(InstanceId.class);
        
        when(globalId.getCdoId()).thenReturn(id);
        when(globalId.getTypeName()).thenReturn("com.example.Item");
        
        org.javers.core.metamodel.object.CdoSnapshotState state = mock(org.javers.core.metamodel.object.CdoSnapshotState.class);
        when(snapshot.getState()).thenReturn(state);
        when(state.getPropertyNames()).thenReturn(new java.util.HashSet<>());
        
        when(snapshot.getCommitMetadata()).thenReturn(metadata);
        when(snapshot.getVersion()).thenReturn((long)version);
        when(snapshot.getGlobalId()).thenReturn(globalId);
        when(metadata.getAuthor()).thenReturn("1");
        when(metadata.getCommitDateInstant()).thenReturn(Instant.now());
        when(metadata.getProperties()).thenReturn(Collections.emptyMap());
        
        return snapshot;
    }
}
