package com.inventorymanager.backend.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import org.javers.core.Javers;
import org.javers.core.commit.CommitMetadata;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.repository.jql.JqlQuery;
import org.junit.jupiter.api.Test;
import com.inventorymanager.backend.repository.UserRepository;
import com.inventorymanager.backend.domain.User;

public class AuditServiceLogicFixTest {

    @Test
    public void testFindByIdIsCalledWhenCacheIsNull() {
        Javers javers = mock(Javers.class);
        EntityRegistry registry = mock(EntityRegistry.class);
        UserRepository userRepository = mock(UserRepository.class);
        
        when(registry.resolve(any())).thenAnswer(i -> String.class);
        
        CdoSnapshot snapshot = mock(CdoSnapshot.class);
        CommitMetadata meta = mock(CommitMetadata.class);
        when(meta.getAuthor()).thenReturn("1");
        when(meta.getProperties()).thenReturn(java.util.Map.of());
        when(snapshot.getCommitMetadata()).thenReturn(meta);
        when(snapshot.getVersion()).thenReturn(1L);
        when(snapshot.getState()).thenReturn(mock(org.javers.core.metamodel.object.CdoSnapshotState.class));

        // Return empty list so userIds loop does not execute
        when(javers.findSnapshots(any(JqlQuery.class))).thenReturn(List.of(snapshot));
        
        User user = new User();
        user.setUsername("ActualUser");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        AuditService auditService = new AuditService(javers, registry, userRepository);
        // Note: The snapshots.stream() loop in auditTrail uses 'snapshot' variable
        // The getUsername is called with 'userCache' which is empty HashMap if bulk lookup was skipped or failed.
        AuditService.PageResponse response = auditService.auditTrail("Item", null, 0, 10);
        
        // Wait, if snapshot is found, getUsername(snapshot.getCommitMetadata().getAuthor(), userCache) is called.
        // If userIds was empty, userCache is new HashMap().
        // getUsername(id, cache) -> if (cache != null) return cache.getOrDefault(id, "User #" + id);
        // This is why it returns "User #1".
        
        assertEquals("ActualUser", ((AuditEntryResponse)response.data().get(0)).changedByUsername());
    }
}
