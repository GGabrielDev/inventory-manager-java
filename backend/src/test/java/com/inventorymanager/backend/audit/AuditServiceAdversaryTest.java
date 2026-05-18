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
    void testPreviousStateResolutionCausesNPlusOneQueries() {
        String entityName = "Item";
        Long entityId = 1L;
        int pageSize = 10;
        
        when(entityRegistry.resolve(anyString())).thenAnswer(i -> Object.class);
        
        List<CdoSnapshot> snapshots = new ArrayList<>();
        for (int i = 0; i < pageSize; i++) {
            snapshots.add(createSnapshot(entityId, i + 10));
        }
        
        when(javers.findSnapshots(any(JqlQuery.class))).thenReturn(snapshots).thenReturn(Collections.emptyList());

        auditService.auditTrail(entityName, entityId, 0, pageSize);

        verify(javers, times(1 + pageSize)).findSnapshots(any(JqlQuery.class));
    }

    @Test
    void testPaginationTotalHackBreaksOnExactPageSize() {
        int pageSize = 5;
        when(entityRegistry.resolve(anyString())).thenAnswer(i -> Object.class);
        
        List<CdoSnapshot> snapshots = new ArrayList<>();
        for (int i = 0; i < pageSize; i++) snapshots.add(createSnapshot(1L, 1));
        
        when(javers.findSnapshots(any())).thenReturn(snapshots);

        AuditService.PageResponse response = auditService.auditTrail("Item", 1L, 0, pageSize);

        assertEquals(-1, response.total());
    }

    @Test
    void testEntityResolveFailureSwallowsErrors() {
        when(entityRegistry.resolve("unknown")).thenThrow(new RuntimeException("Registry Down"));
        
        AuditService.PageResponse response = auditService.auditTrail("Unknown", 1L, 0, 10);
        
        assertNotNull(response);
        assertTrue(response.data().isEmpty());
    }

    private CdoSnapshot createSnapshot(Long id, int version) {
        CdoSnapshot snapshot = mock(CdoSnapshot.class);
        CommitMetadata metadata = mock(CommitMetadata.class);
        InstanceId globalId = mock(InstanceId.class);
        
        // Mocking getCdoId to return Long for InstanceId check in AuditService
        when(globalId.getCdoId()).thenReturn(id);
        when(globalId.getTypeName()).thenReturn("com.example.Item");
        
        // Mock state loosely to avoid SnapshotState class issues
        try {
            var state = mock(Class.forName("org.javers.core.metamodel.object.CdoSnapshotState"));
            when(snapshot.getState()).thenReturn((org.javers.core.metamodel.object.CdoSnapshotState)state);
            when(((org.javers.core.metamodel.object.CdoSnapshotState)state).getPropertyNames()).thenReturn(Collections.emptyList());
        } catch (Exception e) {
            // Fallback to generic mock if class not found
            when(snapshot.getState()).thenAnswer(i -> {
               var s = mock(Object.class, withSettings().extraInterfaces(org.javers.core.metamodel.object.GlobalId.class)); // dummy
               return null; 
            });
        }
        
        when(snapshot.getCommitMetadata()).thenReturn(metadata);
        when(snapshot.getVersion()).thenReturn((long)version);
        when(snapshot.getGlobalId()).thenReturn(globalId);
        when(metadata.getAuthor()).thenReturn("1");
        when(metadata.getCommitDateInstant()).thenReturn(Instant.now());
        when(metadata.getProperties()).thenReturn(Collections.emptyMap());
        
        return snapshot;
    }
}
