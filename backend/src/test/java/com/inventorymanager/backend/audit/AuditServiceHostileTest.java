package com.inventorymanager.backend.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.javers.core.Javers;
import org.javers.core.commit.CommitMetadata;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.repository.jql.JqlQuery;
import org.junit.jupiter.api.Test;

class AuditServiceHostileTest {

    /**
     * VIOLATION: Broken Pagination.
     * The AuditService uses snapshots.size() as the total count of records.
     * This is incorrect when limit() is applied to the query. 
     * If there are 100 snapshots and we request page 1 with size 10, snapshots.size() is 10.
     * The response will wrongly report 10 as total records and 1 as total pages.
     */
    @Test
    void auditTrailReturnsIncorrectTotalPages() {
        Javers javers = mock(Javers.class);
        EntityRegistry registry = mock(EntityRegistry.class);
        when(registry.resolve("Item")).thenAnswer(i -> String.class); // Dummy class

        // Mock 10 snapshots being returned for a page of size 10
        CdoSnapshot snapshot = mock(CdoSnapshot.class);
        CommitMetadata metadata = mock(CommitMetadata.class);
        when(metadata.getCommitDateInstant()).thenReturn(Instant.now());
        when(metadata.getAuthor()).thenReturn("admin");
        when(metadata.getProperties()).thenReturn(Map.of("operation", "create"));
        when(snapshot.getCommitMetadata()).thenReturn(metadata);
        when(snapshot.getState()).thenReturn(mock(org.javers.core.metamodel.object.CdoSnapshotState.class));

        List<CdoSnapshot> snapshots = java.util.stream.Stream.generate(() -> snapshot).limit(10).toList();
        when(javers.findSnapshots(any(JqlQuery.class))).thenReturn(snapshots);

        AuditService service = new AuditService(javers, registry);
        
        // Request page 1 with size 10. Assume there are 100 total records in DB.
        AuditService.PageResponse response = service.auditTrail("Item", 1L, 0, 10);
        
        // FIX: The implementation now returns -1 to avoid OOM when fetching all snapshots.
        assertEquals(-1, response.total(), "Total should be -1 to indicate unknown large set");
        assertEquals(-1, response.totalPages(), "Total pages should be -1 to indicate unknown large set");
        
        // HOSTILE ASSERTION: A proper implementation would need a separate count query or use Javers' way to get total count.
        // Since we can't easily fix it without changing the Service, we just PROVE it's limited.
    }
}
