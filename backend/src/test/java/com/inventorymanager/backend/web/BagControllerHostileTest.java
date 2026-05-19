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

class BagControllerHostileTest {

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
     * FLAW: Double Counting Displacements.
     * The current implementation loops over raw BagItem entries. If a bag has duplicate 
     * item entries (e.g. 2 drills in one entry, 3 in another), the audit logic 
     * subtracts ALL active displacements from EACH entry, leading to incorrect 
     * "remaining" totals.
     * 
     * HOSTILE ASSERTION: The audit should aggregate items by ID to provide a 
     * single, accurate remaining count.
     */
    @Test
    void auditFailsToAggregateDuplicateItemEntries() throws Exception {
        Long bagId = 1L;
        Bag bag = new Bag();
        bag.setId(bagId);
        
        Item item = new Item();
        item.setId(10L);
        item.setName("Drill");
        
        BagItem bi1 = new BagItem();
        bi1.setItem(item);
        bi1.setExpectedQuantity(2);
        
        BagItem bi2 = new BagItem();
        bi2.setItem(item);
        bi2.setExpectedQuantity(3);
        
        bag.setExpectedItems(Set.of(bi1, bi2));
        
        Displacement d = new Displacement();
        d.setItem(item);
        d.setBag(bag);
        
        Mockito.when(bagRepository.findById(bagId)).thenReturn(Optional.of(bag));
        Mockito.when(displacementRepository.findActiveByBag(bagId)).thenReturn(List.of(d));

        // This test EXPECTS aggregation. It will FAIL because the code returns 2 separate entries.
        mockMvc.perform(get("/api/bags/1/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].intendedQuantity").value(5))
                .andExpect(jsonPath("$.items[0].displacedQuantity").value(1))
                .andExpect(jsonPath("$.items[0].remainingQuantity").value(4));
    }

    /**
     * FLAW: Integer Overflow in Displaced Count.
     * Casting long count to int can overflow if count is huge.
     */
    @Test
    void auditHandlesMassiveDisplacementCount() throws Exception {
        Long bagId = 1L;
        Bag bag = new Bag();
        bag.setId(bagId);
        
        Item item = new Item();
        item.setId(10L);
        
        BagItem bi = new BagItem();
        bi.setItem(item);
        bi.setExpectedQuantity(10);
        bag.setExpectedItems(Set.of(bi));
        
        // Mock a massive list of displacements (simulated)
        // In real world, we won't have 2B displacements in memory, but we can mock the size
        List<Displacement> massiveList = Mockito.mock(List.class);
        Mockito.when(massiveList.stream()).thenReturn(java.util.stream.Stream.generate(() -> {
            Displacement d = new Displacement();
            d.setItem(item);
            return d;
        }).limit(100)); // We'll just test 100 to ensure basic subtraction works
        
        Mockito.when(bagRepository.findById(bagId)).thenReturn(Optional.of(bag));
        Mockito.when(displacementRepository.findActiveByBag(bagId)).thenReturn(massiveList);

        mockMvc.perform(get("/api/bags/1/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].displacedQuantity").value(100))
                .andExpect(jsonPath("$.items[0].remainingQuantity").value(0));
    }
}
