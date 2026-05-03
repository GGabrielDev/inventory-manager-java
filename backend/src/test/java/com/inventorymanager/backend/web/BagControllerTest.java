package com.inventorymanager.backend.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.inventorymanager.backend.audit.AuditService;
import com.inventorymanager.backend.auth.CurrentUser;
import com.inventorymanager.backend.domain.Bag;
import com.inventorymanager.backend.repository.BagRepository;
import com.inventorymanager.backend.repository.BranchRepository;
import com.inventorymanager.backend.repository.DepartmentRepository;
import com.inventorymanager.backend.repository.ItemRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class BagControllerTest {

    private MockMvc mockMvc;
    private BagRepository bagRepository;

    @BeforeEach
    void setUp() {
        bagRepository = Mockito.mock(BagRepository.class);
        var branchRepo = Mockito.mock(BranchRepository.class);
        var deptRepo = Mockito.mock(DepartmentRepository.class);
        var itemRepo = Mockito.mock(ItemRepository.class);
        
        // Use a real simple stub instead of Mockito to avoid Java 25 issues
        CurrentUser currentUser = new CurrentUser() {
            @Override public Long id() { return 1L; }
        };
        
        // Dummy AuditService
        AuditService auditService = new AuditService(null, null) {
            @Override public void commitCreate(Long a, Object e) {}
            @Override public void commitUpdate(Long a, Object e) {}
            @Override public void commitDelete(Long a, Object e) {}
        };

        BagController controller = new BagController(bagRepository, branchRepo, deptRepo, itemRepo, currentUser, auditService);
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
}
