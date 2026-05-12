package com.inventorymanager.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.inventorymanager.backend.audit.AuditService;
import com.inventorymanager.backend.domain.*;
import com.inventorymanager.backend.repository.DepartmentRepository;
import com.inventorymanager.backend.repository.ItemRepository;
import com.inventorymanager.backend.repository.ItemRequestRepository;
import com.inventorymanager.backend.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ItemRequestWorkflowServiceTest {

    private ItemRequestWorkflowService service;

    @Mock private ItemRepository itemRepository;
    @Mock private ItemRequestRepository requestRepository;
    @Mock private UserRepository userRepository;
    @Mock private DepartmentRepository departmentRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Dummy AuditService that does nothing to avoid Mockito + Java 25 + Javers issues
        AuditService dummyAudit = new AuditService(null, null) {
            @Override public void commitCreate(Long actorId, Object entity) {}
            @Override public void commitUpdate(Long actorId, Object entity) {}
            @Override public void commitDelete(Long actorId, Object entity) {}
            @Override public void commitLink(Long actorId, String r, Long p, Long c) {}
            @Override public void commitUnlink(Long actorId, String r, Long p, Long c) {}
        };

        service = new ItemRequestWorkflowService(
                itemRepository, requestRepository, userRepository, departmentRepository, dummyAudit);
    }

    @Test
    void executeInboundCreatesNewItem() {
        // Setup
        Branch branch = new Branch();
        branch.setId(10L);
        
        User user = new User();
        user.setId(1L);
        user.setBranch(branch);

        Department inboundDept = new Department();
        inboundDept.setName("Inbound");
        inboundDept.setBranch(branch);

        ItemRequest request = new ItemRequest();
        request.setRequestType(ItemRequestType.INBOUND);
        request.setStatus(ItemRequestStatus.APPROVED);
        request.setRequestedBy(user);
        request.setTargetBranch(branch);

        ItemRequestEntry entry = new ItemRequestEntry();
        entry.setRequestedItemName("Test Item");
        entry.setRequestedQuantity(5);
        entry.setRequestedUnit(Item.UnitType.UND);
        request.addEntry(entry);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(departmentRepository.findByNameAndBranch_Id("Inbound", branch.getId())).thenReturn(Optional.of(inboundDept));
        when(itemRepository.save(any(Item.class))).thenAnswer(i -> i.getArgument(0));
        when(requestRepository.save(any(ItemRequest.class))).thenAnswer(i -> i.getArgument(0));

        // Execute
        ItemRequest result = service.executeRequest(1L, 1L);

        // Verify
        assertEquals(ItemRequestStatus.EXECUTED, result.getStatus());
        verify(itemRepository, times(1)).save(argThat(item -> 
            item.getName().equals("Test Item") && 
            item.getBranch().equals(branch) && 
            item.getDepartment().equals(inboundDept)
        ));
    }

    @Test
    void executeInterBranchTransferUpdatesBranch() {
        // Setup
        Branch sourceBranch = new Branch();
        sourceBranch.setId(1L);
        Branch targetBranch = new Branch();
        targetBranch.setId(2L);

        Department targetInbound = new Department();
        targetInbound.setName("Inbound");
        targetInbound.setBranch(targetBranch);

        Item item = new Item();
        item.setId(50L);
        item.setBranch(sourceBranch);

        ItemRequest request = new ItemRequest();
        request.setRequestType(ItemRequestType.TRANSFER);
        request.setStatus(ItemRequestStatus.APPROVED);
        request.setTargetBranch(targetBranch);

        ItemRequestEntry entry = new ItemRequestEntry();
        entry.setItem(item);
        request.addEntry(entry);

        User actor = new User();
        actor.setId(1L);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(departmentRepository.findByNameAndBranch_Id("Inbound", targetBranch.getId())).thenReturn(Optional.of(targetInbound));
        when(itemRepository.save(any(Item.class))).thenAnswer(i -> i.getArgument(0));
        when(requestRepository.save(any(ItemRequest.class))).thenAnswer(i -> i.getArgument(0));

        // Execute
        service.executeRequest(1L, 1L);

        // Verify
        verify(itemRepository).save(argThat(i -> 
            i.getBranch().equals(targetBranch) && 
            i.getDepartment().equals(targetInbound)
        ));
    }
}
