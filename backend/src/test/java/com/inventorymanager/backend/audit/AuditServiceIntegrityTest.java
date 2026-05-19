package com.inventorymanager.backend.audit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import org.javers.core.Javers;
import org.javers.core.commit.CommitMetadata;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.core.metamodel.object.InstanceId;
import org.javers.repository.jql.JqlQuery;
import org.junit.jupiter.api.Test;
import com.inventorymanager.backend.repository.UserRepository;

public class AuditServiceIntegrityTest {

    @Test
    public void testAuditTrailResolvesPreviousStateAcrossPageBoundaries() {
        Javers javers = mock(Javers.class);
        EntityRegistry registry = new EntityRegistry(); 
        UserRepository userRepository = mock(UserRepository.class);

        InstanceId id = mock(InstanceId.class);
        when(id.getTypeName()).thenReturn("com.inventorymanager.backend.domain.Item");
        when(id.getCdoId()).thenReturn(1L);

        CommitMetadata commonMeta = mock(CommitMetadata.class);
        when(commonMeta.getAuthor()).thenReturn("1");
        when(commonMeta.getProperties()).thenReturn(Map.of());
        when(commonMeta.getCommitDateInstant()).thenReturn(java.time.Instant.now());

        CdoSnapshot v2Snapshot = mock(CdoSnapshot.class);
        when(v2Snapshot.getCommitMetadata()).thenReturn(commonMeta);
        when(v2Snapshot.getVersion()).thenReturn(2L);
        when(v2Snapshot.getGlobalId()).thenReturn(id);
        
        org.javers.core.metamodel.object.CdoSnapshotState state2 = mock(org.javers.core.metamodel.object.CdoSnapshotState.class);
        when(state2.getPropertyNames()).thenReturn(java.util.Set.of("name"));
        when(state2.getPropertyValue("name")).thenReturn("New Name");
        when(v2Snapshot.getState()).thenReturn(state2);

        CdoSnapshot v1Snapshot = mock(CdoSnapshot.class);
        when(v1Snapshot.getCommitMetadata()).thenReturn(commonMeta);
        when(v1Snapshot.getGlobalId()).thenReturn(id);
        when(v1Snapshot.getVersion()).thenReturn(1L);
        org.javers.core.metamodel.object.CdoSnapshotState state1 = mock(org.javers.core.metamodel.object.CdoSnapshotState.class);
        when(state1.getPropertyNames()).thenReturn(java.util.Set.of("name"));
        when(state1.getPropertyValue("name")).thenReturn("Old Name");
        when(v1Snapshot.getState()).thenReturn(state1);

        // Simple counter based mock
        java.util.concurrent.atomic.AtomicInteger callCount = new java.util.concurrent.atomic.AtomicInteger(0);
        when(javers.findSnapshots(any(JqlQuery.class))).thenAnswer(inv -> {
            if (callCount.getAndIncrement() == 0) {
                return List.of(v2Snapshot); // Main trail query
            } else {
                return List.of(v1Snapshot); // Fallback query for V1
            }
        });

        AuditService service = new AuditService(javers, registry, userRepository);
        AuditService.PageResponse response = service.auditTrail("item", 1L, 0, 10);

        AuditEntryResponse entry = (AuditEntryResponse) response.data().get(0);
        
        assertNotNull(entry.previousState(), "INTEGRITY: Previous state must be resolved even across page boundaries");
        assertEquals("Old Name", entry.previousState().get("name"));
    }
}
