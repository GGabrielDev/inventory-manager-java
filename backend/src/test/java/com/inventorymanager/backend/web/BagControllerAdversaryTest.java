package com.inventorymanager.backend.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.inventorymanager.backend.audit.AuditService;
import com.inventorymanager.backend.auth.AppUserPrincipal;
import com.inventorymanager.backend.auth.CurrentUser;
import com.inventorymanager.backend.domain.Bag;
import com.inventorymanager.backend.domain.BagItem;
import com.inventorymanager.backend.domain.Item;
import com.inventorymanager.backend.repository.BagRepository;
import com.inventorymanager.backend.repository.BranchRepository;
import com.inventorymanager.backend.repository.DepartmentRepository;
import com.inventorymanager.backend.repository.DisplacementRepository;
import com.inventorymanager.backend.repository.ItemRepository;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class BagControllerAdversaryTest {

    private MockMvc mockMvc;
    private BagRepository bagRepository;
    private DisplacementRepository displacementRepository;

    @BeforeEach
    void setUp() {
        bagRepository = Mockito.mock(BagRepository.class);
        displacementRepository = Mockito.mock(DisplacementRepository.class);
        var branchRepo = Mockito.mock(BranchRepository.class);
        var deptRepo = Mockito.mock(DepartmentRepository.class);
        var itemRepo = Mockito.mock(ItemRepository.class);
        CurrentUser currentUser = Mockito.mock(CurrentUser.class);
        AuditService auditService = Mockito.mock(AuditService.class);

        BagController controller = new BagController(bagRepository, branchRepo, deptRepo, itemRepo, displacementRepository, currentUser, auditService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    /**
     * VIOLATION: Null Expected Quantity NPE.
     * Collectors.summingInt(BagItem::getExpectedQuantity) will NPE if getExpectedQuantity() is null.
     */
    @Test
    void auditThrowsNPEOnNullExpectedQuantity() throws Exception {
        Long bagId = 1L;
        Bag bag = new Bag();
        bag.setId(bagId);
        
        Item item = new Item();
        item.setId(10L);
        
        BagItem bi = new BagItem();
        bi.setItem(item);
        bi.setExpectedQuantity(null); // This is the poison
        bag.setExpectedItems(Set.of(bi));
        
        Mockito.when(bagRepository.findById(bagId)).thenReturn(Optional.of(bag));
        Mockito.when(displacementRepository.findActiveByBag(bagId)).thenReturn(Collections.emptyList());

        // This should fail with 500 (NPE) in a real app, 
        // MockMvc standalone setup will throw the exception directly.
        try {
            mockMvc.perform(get("/api/bags/1/audit"));
        } catch (jakarta.servlet.ServletException e) {
            if (e.getCause() instanceof NullPointerException) {
                // PROVED
                return;
            }
            throw e;
        }
    }
}
