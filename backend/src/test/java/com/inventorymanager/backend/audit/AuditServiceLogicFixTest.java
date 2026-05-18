package com.inventorymanager.backend.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import org.javers.core.Javers;
import org.javers.core.commit.CommitMetadata;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.repository.jql.JqlQuery;
import org.junit.jupiter.api.Test;
import com.inventorymanager.backend.repository.UserRepository;
import com.inventorymanager.backend.domain.User;

public class AuditServiceLogicFixTest {

    @Test
    public void testFindByIdIsCalledWhenCacheMisses() {
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

        when(javers.findSnapshots(any(JqlQuery.class))).thenReturn(List.of(snapshot));
        
        // Mock bulk resolve to return empty (cache miss)
        when(userRepository.findAllById(any())).thenReturn(List.of());
        
        User user = new User();
        user.setUsername("ActualUser");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        AuditService auditService = new AuditService(javers, registry, userRepository);
        AuditService.PageResponse response = auditService.auditTrail("Item", null, 0, 10);
        
        assertEquals("ActualUser", response.data().get(0).changedByUsername());
        verify(userRepository).findById(1L);
    }
}
