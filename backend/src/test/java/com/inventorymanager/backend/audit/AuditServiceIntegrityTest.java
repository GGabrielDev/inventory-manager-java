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
        EntityRegistry registry = new EntityRegistry(); // Use real registry for resolution
        UserRepository userRepository = mock(UserRepository.class);

        // Version 2 on current page
        CdoSnapshot v2Snapshot = mock(CdoSnapshot.class);
        CommitMetadata v2Meta = mock(CommitMetadata.class);
        when(v2Meta.getAuthor()).thenReturn("1");
        when(v2Meta.getProperties()).thenReturn(Map.of());
        when(v2Snapshot.getCommitMetadata()).thenReturn(v2Meta);
        when(v2Snapshot.getVersion()).thenReturn(2L);
        
        InstanceId id = mock(InstanceId.class);
        when(id.getTypeName()).thenReturn("com.inventorymanager.backend.domain.Item");
        when(id.getCdoId()).thenReturn(1L);
        when(v2Snapshot.getGlobalId()).thenReturn(id);
        
        org.javers.core.metamodel.object.CdoSnapshotState state2 = mock(org.javers.core.metamodel.object.CdoSnapshotState.class);
        when(state2.getPropertyNames()).thenReturn(java.util.Set.of("name"));
        when(state2.getPropertyValue("name")).thenReturn("New Name");
        when(v2Snapshot.getState()).thenReturn(state2);

        // Version 1 (the predecessor, NOT in current batch)
        CdoSnapshot v1Snapshot = mock(CdoSnapshot.class);
        org.javers.core.metamodel.object.CdoSnapshotState state1 = mock(org.javers.core.metamodel.object.CdoSnapshotState.class);
        when(state1.getPropertyNames()).thenReturn(java.util.Set.of("name"));
        when(state1.getPropertyValue("name")).thenReturn("Old Name");
        when(v1Snapshot.getState()).thenReturn(state1);

        // Mock Javers to return V1 when queried specifically for it
        when(javers.findSnapshots(any(JqlQuery.class))).thenAnswer(inv -> {
            JqlQuery q = inv.getArgument(0);
            if (q.toString().contains("withVersion 1")) {
                return List.of(v1Snapshot);
            }
            return List.of(v2Snapshot);
        });

        AuditService service = new AuditService(javers, registry, userRepository);
        AuditService.PageResponse response = service.auditTrail("item", 1L, 0, 10);

        AuditEntryResponse entry = (AuditEntryResponse) response.data().get(0);
        
        assertNotNull(entry.previousState(), "INTEGRITY: Previous state must be resolved even across page boundaries");
        assertEquals("Old Name", entry.previousState().get("name"));
    }
}
