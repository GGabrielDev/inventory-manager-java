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

class BagControllerTestAdversary {

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
        
        AuditService auditService = new AuditService(null, null, null) {
            @Override public void commitCreate(Long a, Object e) {}
            @Override public void commitUpdate(Long a, Object e) {}
            @Override public void commitDelete(Long a, Object e) {}
        };

        BagController controller = new BagController(bagRepository, branchRepo, deptRepo, itemRepo, displacementRepository, currentUser, auditService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    /**
     * ARCHITECTURAL VIOLATION: Incomplete Audit (Ghost Items).
     * If an item is displaced from a bag but was never in the "expectedItems" list,
     * it is completely omitted from the audit result. 
     * A true audit must report all items currently "out" of the bag.
     */
    @Test
    void auditFailsToReportGhostDisplacements() throws Exception {
        Long bagId = 1L;
        Bag bag = new Bag();
        bag.setId(bagId);
        bag.setExpectedItems(Set.of()); // Empty expected items
        
        Item ghostItem = new Item();
        ghostItem.setId(666L);
        ghostItem.setName("Unexpected Tool");
        
        Displacement d = new Displacement();
        d.setItem(ghostItem);
        d.setBag(bag);
        
        Mockito.when(bagRepository.findById(bagId)).thenReturn(Optional.of(bag));
        Mockito.when(displacementRepository.findActiveByBag(bagId)).thenReturn(List.of(d));

        // Current code will return an empty list because it only iterates over expectedItems.
        // Hostile expectation: Every active displacement MUST be accounted for.
        mockMvc.perform(get("/api/bags/1/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.itemId == 666)]").exists())
                .andExpect(jsonPath("$.items[?(@.itemId == 666)].displacedQuantity").value(1));
    }

    /**
     * LOGICAL VIOLATION: Redundant Subtraction.
     * If there are multiple BagItem entries for the same Item ID, the current code
     * subtracts the total displacement count from EACH entry, rather than 
     * aggregating the expected total first.
     */
    @Test
    void auditRedundantlySubtractsFromDuplicateEntries() throws Exception {
        Long bagId = 1L;
        Bag bag = new Bag();
        bag.setId(bagId);
        
        Item item = new Item();
        item.setId(10L);
        item.setName("Drill");
        
        BagItem bi1 = new BagItem();
        bi1.setItem(item);
        bi1.setExpectedQuantity(1);
        
        BagItem bi2 = new BagItem();
        bi2.setItem(item);
        bi2.setExpectedQuantity(1);
        
        bag.setExpectedItems(Set.of(bi1, bi2));
        
        Displacement d = new Displacement();
        d.setItem(item);
        d.setBag(bag);
        
        Mockito.when(bagRepository.findById(bagId)).thenReturn(Optional.of(bag));
        Mockito.when(displacementRepository.findActiveByBag(bagId)).thenReturn(List.of(d));

        // Total expected = 2. Total displaced = 1. Remaining should be 1.
        // Current code returns TWO entries, each with remaining = 0 (1-1=0, 1-1=0).
        mockMvc.perform(get("/api/bags/1/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].remainingQuantity").value(1));
    }
}
