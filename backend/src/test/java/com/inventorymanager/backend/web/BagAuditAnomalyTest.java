package com.inventorymanager.backend.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.inventorymanager.backend.audit.AuditService;
import com.inventorymanager.backend.auth.CurrentUser;
import com.inventorymanager.backend.domain.Bag;
import com.inventorymanager.backend.domain.BagItem;
import com.inventorymanager.backend.domain.Displacement;
import com.inventorymanager.backend.domain.Item;
import com.inventorymanager.backend.repository.BagRepository;
import com.inventorymanager.backend.repository.BranchRepository;
import com.inventorymanager.backend.repository.DepartmentRepository;
import com.inventorymanager.backend.repository.DisplacementRepository;
import com.inventorymanager.backend.repository.ItemRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class BagAuditAnomalyTest {

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
        
        CurrentUser currentUser = new CurrentUser() {
            @Override public Long id() { return 1L; }
        };
        
        AuditService auditService = Mockito.mock(AuditService.class);

        BagController controller = new BagController(bagRepository, branchRepo, deptRepo, itemRepo, displacementRepository, currentUser, auditService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    /**
     * ADVERSARIAL TEST: Verify anomalyCount detection.
     * If 2 items are displaced but only 1 is expected, anomalyCount must be 1.
     */
    @Test
    void auditDetectsAnomalyCountOnOverDisplacement() throws Exception {
        Long bagId = 1L;
        Bag bag = new Bag();
        bag.setId(bagId);
        
        Item item = new Item();
        item.setId(10L);
        item.setName("Water");
        
        BagItem bi = new BagItem();
        bi.setItem(item);
        bi.setExpectedQuantity(1);
        bag.setExpectedItems(Set.of(bi));
        
        Displacement d1 = new Displacement(); d1.setItem(item);
        Displacement d2 = new Displacement(); d2.setItem(item);
        
        Mockito.when(bagRepository.findById(bagId)).thenReturn(Optional.of(bag));
        Mockito.when(displacementRepository.findActiveByBag(bagId)).thenReturn(List.of(d1, d2));

        mockMvc.perform(get("/api/bags/1/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].displacedQuantity").value(2))
                .andExpect(jsonPath("$.items[0].intendedQuantity").value(1))
                .andExpect(jsonPath("$.items[0].remainingQuantity").value(0))
                .andExpect(jsonPath("$.items[0].anomalyCount").value(1));
    }
}
