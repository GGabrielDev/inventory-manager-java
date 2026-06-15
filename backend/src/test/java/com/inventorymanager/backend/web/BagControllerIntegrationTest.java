package com.inventorymanager.backend.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.inventorymanager.backend.config.AuditingConfig;
import com.inventorymanager.backend.domain.Bag;
import com.inventorymanager.backend.domain.BagItem;
import com.inventorymanager.backend.domain.Branch;
import com.inventorymanager.backend.domain.Department;
import com.inventorymanager.backend.domain.Item;
import com.inventorymanager.backend.domain.Municipality;
import com.inventorymanager.backend.domain.Parish;
import com.inventorymanager.backend.domain.State;
import com.inventorymanager.backend.repository.BagRepository;
import com.inventorymanager.backend.repository.BranchRepository;
import com.inventorymanager.backend.repository.DepartmentRepository;
import com.inventorymanager.backend.repository.ItemRepository;
import com.inventorymanager.backend.repository.MunicipalityRepository;
import com.inventorymanager.backend.repository.ParishRepository;
import com.inventorymanager.backend.repository.StateRepository;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test that reproduces the Bag lazy-init bug.
 * 
 * The bug: GET /api/bags fails with 500 because Jackson tries to serialize
 * Bag.expectedItems (Set<BagItem>) but BagItem has @ManyToOne(fetch=LAZY)
 * references that Hibernate can't initialize outside a session.
 * 
 * The existing BagControllerTest uses mocked repositories and never catches this.
 * This test uses a real Spring Boot context with real H2 database.
 * 
 * WITHOUT @Transactional(readOnly=true) on BagController read endpoints,
 * these tests fail with HttpMessageNotWritableException.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuditingConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class BagControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StateRepository stateRepository;

    @Autowired
    private MunicipalityRepository municipalityRepository;

    @Autowired
    private ParishRepository parishRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private BagRepository bagRepository;

    @BeforeEach
    void setUp() {
        State state = stateRepository.save(createState("Bag Test State"));
        Municipality muni = municipalityRepository.save(createMunicipality("Bag Test Muni", state));
        Parish parish = parishRepository.save(createParish("Bag Test Parish", muni));
        Branch branch = branchRepository.save(createBranch("Bag Test Branch", state, muni, parish));
        Department dept = departmentRepository.save(createDepartment("Bag Test Storage", branch));
        Item item = itemRepository.save(createItem("Bag Test Hammer", 50, branch, dept));

        Bag bag = new Bag();
        bag.setName("Lazy Init Test Bag");
        bag.setBarcode("LAZY-BAG-001");
        bag.setBranch(branch);
        bag.setAssignedDepartment(dept);

        BagItem bagItem = new BagItem();
        bagItem.setBag(bag);
        bagItem.setItem(item);
        bagItem.setExpectedQuantity(3);
        bag.setExpectedItems(Set.of(bagItem));

        bagRepository.save(bag);
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_bag"})
    void listBagsReturnsBagWithExpectedItems() throws Exception {
        mockMvc.perform(get("/api/bags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Lazy Init Test Bag"))
                .andExpect(jsonPath("$.data[0].barcode").value("LAZY-BAG-001"))
                .andExpect(jsonPath("$.data[0].expectedItems[0].expectedQuantity").value(3));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"get_bag"})
    void getByBarcodeReturnsBagWithExpectedItems() throws Exception {
        mockMvc.perform(get("/api/bags/barcode/LAZY-BAG-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lazy Init Test Bag"))
                .andExpect(jsonPath("$.expectedItems[0].expectedQuantity").value(3));
    }

    // --- Helper factory methods ---

    private static State createState(String name) {
        State s = new State();
        s.setName(name);
        return s;
    }

    private static Municipality createMunicipality(String name, State state) {
        Municipality m = new Municipality();
        m.setName(name);
        m.setState(state);
        return m;
    }

    private static Parish createParish(String name, Municipality muni) {
        Parish p = new Parish();
        p.setName(name);
        p.setMunicipality(muni);
        return p;
    }

    private static Branch createBranch(String name, State state, Municipality muni, Parish parish) {
        Branch b = new Branch();
        b.setName(name);
        b.setAddress("123 Test St");
        b.setState(state);
        b.setMunicipality(muni);
        b.setParish(parish);
        return b;
    }

    private static Department createDepartment(String name, Branch branch) {
        Department d = new Department();
        d.setName(name);
        d.setBranch(branch);
        return d;
    }

    private static Item createItem(String name, int quantity, Branch branch, Department dept) {
        Item i = new Item();
        i.setName(name);
        i.setQuantity(quantity);
        i.setUnit(Item.UnitType.UND);
        i.setBranch(branch);
        i.setDepartment(dept);
        return i;
    }
}
