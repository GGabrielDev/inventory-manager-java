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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class BagControllerTest {

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
        
        // Use a real simple stub instead of Mockito to avoid Java 25 issues
        CurrentUser currentUser = new CurrentUser() {
            @Override public Long id() { return 1L; }
        };
        
        // Dummy AuditService
        AuditService auditService = new AuditService(null, null, null) {
            @Override public void commitCreate(Long a, Object e) {}
            @Override public void commitUpdate(Long a, Object e) {}
            @Override public void commitDelete(Long a, Object e) {}
        };

        BagController controller = new BagController(bagRepository, branchRepo, deptRepo, itemRepo, displacementRepository, currentUser, auditService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getByBarcodeReturnsBag() throws Exception {
        Bag bag = new Bag();
        bag.setId(1L);
        bag.setName("Emergency Kit");
        bag.setBarcode("K-99");

        Mockito.when(bagRepository.findByBarcode("K-99")).thenReturn(Optional.of(bag));

        mockMvc.perform(get("/api/bags/barcode/K-99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Emergency Kit"))
                .andExpect(jsonPath("$.barcode").value("K-99"));
    }

    @Test
    void auditCalculatesRemainingQuantity() throws Exception {
        Long bagId = 1L;
        Bag bag = new Bag();
        bag.setId(bagId);
        bag.setName("Audit Bag");
        
        Item item = new Item();
        item.setId(10L);
        item.setName("Drill");
        
        BagItem bagItem = new BagItem();
        bagItem.setBag(bag);
        bagItem.setItem(item);
        bagItem.setExpectedQuantity(5);
        
        bag.setExpectedItems(Set.of(bagItem));
        
        Displacement d = new Displacement();
        d.setItem(item);
        d.setBag(bag);
        
        Mockito.when(bagRepository.findById(bagId)).thenReturn(Optional.of(bag));
        Mockito.when(displacementRepository.findActiveByBag(bagId)).thenReturn(List.of(d));

        mockMvc.perform(get("/api/bags/1/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].itemName").value("Drill"))
                .andExpect(jsonPath("$.items[0].intendedQuantity").value(5))
                .andExpect(jsonPath("$.items[0].displacedQuantity").value(1))
                .andExpect(jsonPath("$.items[0].remainingQuantity").value(4));
    }
}
