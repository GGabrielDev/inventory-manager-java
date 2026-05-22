package com.inventorymanager.backend.web;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.inventorymanager.backend.config.AuditingConfig;
import com.inventorymanager.backend.domain.Bag;
import com.inventorymanager.backend.domain.BagItem;
import com.inventorymanager.backend.domain.Branch;
import com.inventorymanager.backend.domain.Category;
import com.inventorymanager.backend.domain.Department;
import com.inventorymanager.backend.domain.Displacement;
import com.inventorymanager.backend.domain.DisplacementStatus;
import com.inventorymanager.backend.domain.Item;
import com.inventorymanager.backend.domain.ItemRequest;
import com.inventorymanager.backend.domain.ItemRequestType;
import com.inventorymanager.backend.domain.Municipality;
import com.inventorymanager.backend.domain.Parish;
import com.inventorymanager.backend.domain.Permission;
import com.inventorymanager.backend.domain.Role;
import com.inventorymanager.backend.domain.State;
import com.inventorymanager.backend.domain.User;
import com.inventorymanager.backend.repository.BagItemRepository;
import com.inventorymanager.backend.repository.BagRepository;
import com.inventorymanager.backend.repository.BranchRepository;
import com.inventorymanager.backend.repository.CategoryRepository;
import com.inventorymanager.backend.repository.DepartmentRepository;
import com.inventorymanager.backend.repository.DisplacementRepository;
import com.inventorymanager.backend.repository.ItemRepository;
import com.inventorymanager.backend.repository.ItemRequestRepository;
import com.inventorymanager.backend.repository.MunicipalityRepository;
import com.inventorymanager.backend.repository.ParishRepository;
import com.inventorymanager.backend.repository.PermissionRepository;
import com.inventorymanager.backend.repository.RoleRepository;
import com.inventorymanager.backend.repository.StateRepository;
import com.inventorymanager.backend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuditingConfig.class)
@Transactional
class DeleteIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager em;

    @Autowired private StateRepository stateRepository;
    @Autowired private MunicipalityRepository municipalityRepository;
    @Autowired private ParishRepository parishRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ItemRepository itemRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PermissionRepository permissionRepository;
    @Autowired private BagRepository bagRepository;
    @Autowired private BagItemRepository bagItemRepository;
    @Autowired private DisplacementRepository displacementRepository;
    @Autowired private ItemRequestRepository itemRequestRepository;

    // ────────────────────────────────────────────────────────────────────────────
    // Helper builders
    // ────────────────────────────────────────────────────────────────────────────

    private State savedState(String name) {
        State s = new State();
        s.setName(name);
        return stateRepository.save(s);
    }

    private Municipality savedMunicipality(String name, State state) {
        Municipality m = new Municipality();
        m.setName(name);
        m.setState(state);
        return municipalityRepository.save(m);
    }

    private Parish savedParish(String name, Municipality municipality) {
        Parish p = new Parish();
        p.setName(name);
        p.setMunicipality(municipality);
        return parishRepository.save(p);
    }

    private Branch savedBranch(String name, State state, Municipality municipality, Parish parish) {
        Branch b = new Branch();
        b.setName(name);
        b.setAddress("Test Address");
        b.setState(state);
        b.setMunicipality(municipality);
        b.setParish(parish);
        return branchRepository.save(b);
    }

    private Department savedDepartment(String name, Branch branch) {
        Department d = new Department();
        d.setName(name);
        d.setBranch(branch);
        return departmentRepository.save(d);
    }

    private Category savedCategory(String name) {
        Category c = new Category();
        c.setName(name);
        return categoryRepository.save(c);
    }

    private Item savedItem(String name, Branch branch, Department department) {
        Item i = new Item();
        i.setName(name);
        i.setBranch(branch);
        i.setDepartment(department);
        return itemRepository.save(i);
    }

    private User savedUser(String username, Branch branch) {
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash("$2a$10$hash");
        u.setBranch(branch);
        return userRepository.save(u);
    }

    private Role savedRole(String name) {
        Role r = new Role();
        r.setName(name);
        r.setDescription("desc");
        return roleRepository.save(r);
    }

    private Permission savedPermission(String name) {
        Permission p = new Permission();
        p.setName(name);
        p.setDescription("desc");
        return permissionRepository.save(p);
    }

    private Bag savedBag(String name, String barcode, Branch branch, Department department) {
        Bag bag = new Bag();
        bag.setName(name);
        bag.setBarcode(barcode);
        bag.setBranch(branch);
        bag.setAssignedDepartment(department);
        return bagRepository.save(bag);
    }

    private void flush() {
        em.flush();
        em.clear();
    }

    private void assertSoftDeleted(String table, Long id) {
        Object deletedAt = em.createNativeQuery(
                "SELECT deleted_at FROM " + table + " WHERE id = ?1")
                .setParameter(1, id)
                .getSingleResult();
        assertNotNull(deletedAt, "deleted_at must be set in table '" + table + "' for id=" + id);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // State tests
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "delete_state")
    void deleteState_noChildren_softDeletes() throws Exception {
        State state = savedState("EmptyState");
        flush();

        mockMvc.perform(delete("/api/states/" + state.getId()))
                .andExpect(status().isNoContent());

        assertSoftDeleted("states", state.getId());
    }

    @Test
    @WithMockUser(authorities = "delete_state")
    void deleteState_withMunicipality_returns409() throws Exception {
        State state = savedState("StateWithMunicipality");
        savedMunicipality("Muni", state);
        flush();

        mockMvc.perform(delete("/api/states/" + state.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("municipalities")));
    }

    @Test
    @WithMockUser(authorities = "delete_state")
    void deleteState_withBranch_returns409() throws Exception {
        // Use a separate state/muni/parish for the branch's geographic refs so that
        // the target state has NO municipalities (only a direct branch reference).
        State stateTarget = savedState("StateWithBranch");
        State stateHelper = savedState("HelperState");
        Municipality muni = savedMunicipality("Muni", stateHelper);
        Parish parish = savedParish("Parish", muni);
        savedBranch("Branch", stateTarget, muni, parish);
        flush();

        mockMvc.perform(delete("/api/states/" + stateTarget.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("branches")));
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Municipality tests
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "delete_municipality")
    void deleteMunicipality_noChildren_softDeletes() throws Exception {
        State state = savedState("State");
        Municipality muni = savedMunicipality("EmptyMuni", state);
        flush();

        mockMvc.perform(delete("/api/municipalities/" + muni.getId()))
                .andExpect(status().isNoContent());

        assertSoftDeleted("municipalities", muni.getId());
    }

    @Test
    @WithMockUser(authorities = "delete_municipality")
    void deleteMunicipality_withParish_returns409() throws Exception {
        State state = savedState("State");
        Municipality muni = savedMunicipality("MuniWithParish", state);
        savedParish("Parish", muni);
        flush();

        mockMvc.perform(delete("/api/municipalities/" + muni.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("parishes")));
    }

    @Test
    @WithMockUser(authorities = "delete_municipality")
    void deleteMunicipality_withBranch_returns409() throws Exception {
        // Use a separate municipality for the parish so that the target municipality
        // has NO parishes (only a direct branch reference).
        State state = savedState("State");
        Municipality muniTarget = savedMunicipality("MuniWithBranch", state);
        Municipality muniHelper = savedMunicipality("HelperMuni", state);
        Parish parish = savedParish("Parish", muniHelper);
        savedBranch("Branch", state, muniTarget, parish);
        flush();

        mockMvc.perform(delete("/api/municipalities/" + muniTarget.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("branches")));
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Parish tests
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "delete_parish")
    void deleteParish_noChildren_softDeletes() throws Exception {
        State state = savedState("State");
        Municipality muni = savedMunicipality("Muni", state);
        Parish parish = savedParish("EmptyParish", muni);
        flush();

        mockMvc.perform(delete("/api/parishes/" + parish.getId()))
                .andExpect(status().isNoContent());

        assertSoftDeleted("parishes", parish.getId());
    }

    @Test
    @WithMockUser(authorities = "delete_parish")
    void deleteParish_withBranch_returns409() throws Exception {
        State state = savedState("State");
        Municipality muni = savedMunicipality("Muni", state);
        Parish parish = savedParish("ParishWithBranch", muni);
        savedBranch("Branch", state, muni, parish);
        flush();

        mockMvc.perform(delete("/api/parishes/" + parish.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("branches")));
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Branch tests
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "delete_branch")
    void deleteBranch_noChildren_softDeletes() throws Exception {
        State state = savedState("State");
        Municipality muni = savedMunicipality("Muni", state);
        Parish parish = savedParish("Parish", muni);
        Branch branch = savedBranch("EmptyBranch", state, muni, parish);
        flush();

        mockMvc.perform(delete("/api/branches/" + branch.getId()))
                .andExpect(status().isNoContent());

        assertSoftDeleted("branches", branch.getId());
    }

    @Test
    @WithMockUser(authorities = "delete_branch")
    void deleteBranch_withDepartment_returns409() throws Exception {
        State state = savedState("State");
        Municipality muni = savedMunicipality("Muni", state);
        Parish parish = savedParish("Parish", muni);
        Branch branch = savedBranch("BranchWithDept", state, muni, parish);
        savedDepartment("Dept", branch);
        flush();

        mockMvc.perform(delete("/api/branches/" + branch.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("departments")));
    }

    @Test
    @WithMockUser(authorities = "delete_branch")
    void deleteBranch_withUser_returns409() throws Exception {
        State state = savedState("State");
        Municipality muni = savedMunicipality("Muni", state);
        Parish parish = savedParish("Parish", muni);
        Branch branch = savedBranch("BranchWithUser", state, muni, parish);
        savedUser("testuser_branch", branch);
        flush();

        mockMvc.perform(delete("/api/branches/" + branch.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("users")));
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Department tests
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "delete_department")
    void deleteDepartment_noChildren_softDeletes() throws Exception {
        State state = savedState("State");
        Municipality muni = savedMunicipality("Muni", state);
        Parish parish = savedParish("Parish", muni);
        Branch branch = savedBranch("Branch", state, muni, parish);
        Department dept = savedDepartment("EmptyDept", branch);
        flush();

        mockMvc.perform(delete("/api/departments/" + dept.getId()))
                .andExpect(status().isNoContent());

        assertSoftDeleted("departments", dept.getId());
    }

    @Test
    @WithMockUser(authorities = "delete_department")
    void deleteDepartment_withItem_returns409() throws Exception {
        State state = savedState("State");
        Municipality muni = savedMunicipality("Muni", state);
        Parish parish = savedParish("Parish", muni);
        Branch branch = savedBranch("Branch", state, muni, parish);
        Department dept = savedDepartment("DeptWithItem", branch);
        savedItem("Item", branch, dept);
        flush();

        mockMvc.perform(delete("/api/departments/" + dept.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("items")));
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Category tests
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "delete_category")
    void deleteCategory_noChildren_softDeletes() throws Exception {
        Category cat = savedCategory("EmptyCat");
        flush();

        mockMvc.perform(delete("/api/categories/" + cat.getId()))
                .andExpect(status().isNoContent());

        assertSoftDeleted("categories", cat.getId());
    }

    @Test
    @WithMockUser(authorities = "delete_category")
    void deleteCategory_withItem_returns409() throws Exception {
        State state = savedState("State");
        Municipality muni = savedMunicipality("Muni", state);
        Parish parish = savedParish("Parish", muni);
        Branch branch = savedBranch("Branch", state, muni, parish);
        Department dept = savedDepartment("Dept", branch);
        Category cat = savedCategory("CatWithItem");

        Item item = savedItem("ItemWithCat", branch, dept);
        item.setCategory(cat);
        itemRepository.save(item);
        flush();

        mockMvc.perform(delete("/api/categories/" + cat.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("items")));
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Item tests
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "delete_item")
    void deleteItem_noChildren_softDeletes() throws Exception {
        State state = savedState("State");
        Municipality muni = savedMunicipality("Muni", state);
        Parish parish = savedParish("Parish", muni);
        Branch branch = savedBranch("Branch", state, muni, parish);
        Department dept = savedDepartment("Dept", branch);
        Item item = savedItem("EmptyItem", branch, dept);
        flush();

        mockMvc.perform(delete("/api/items/" + item.getId()))
                .andExpect(status().isNoContent());

        assertSoftDeleted("items", item.getId());
    }

    @Test
    @WithMockUser(authorities = "delete_item")
    void deleteItem_withBagItem_returns409() throws Exception {
        State state = savedState("State");
        Municipality muni = savedMunicipality("Muni", state);
        Parish parish = savedParish("Parish", muni);
        Branch branch = savedBranch("Branch", state, muni, parish);
        Department dept = savedDepartment("Dept", branch);
        Item item = savedItem("ItemInBag", branch, dept);
        Bag bag = savedBag("Bag", "BAR001", branch, dept);

        BagItem bagItem = new BagItem();
        bagItem.setBag(bag);
        bagItem.setItem(item);
        bagItemRepository.save(bagItem);
        flush();

        mockMvc.perform(delete("/api/items/" + item.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("bags")));
    }

    @Test
    @WithMockUser(authorities = "delete_item")
    void deleteItem_withActiveDisplacement_returns409() throws Exception {
        State state = savedState("State");
        Municipality muni = savedMunicipality("Muni", state);
        Parish parish = savedParish("Parish", muni);
        Branch branch = savedBranch("Branch", state, muni, parish);
        Department dept = savedDepartment("Dept", branch);
        Item item = savedItem("ItemWithDisplacement", branch, dept);
        Bag bag = savedBag("Bag", "BAR002", branch, dept);

        Displacement disp = new Displacement();
        disp.setItem(item);
        disp.setBag(bag);
        disp.setReason("Test reason");
        disp.setBorrowerName("Test Borrower");
        disp.setExpectedReturnDate(java.time.OffsetDateTime.now().plusDays(7));
        displacementRepository.save(disp);
        flush();

        mockMvc.perform(delete("/api/items/" + item.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("displacements")));
    }

    // ────────────────────────────────────────────────────────────────────────────
    // User tests — paranoid soft-delete, no referential guard
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "delete_user")
    void deleteUser_softDeletes_auditTrailPreserved() throws Exception {
        State state = savedState("State");
        Municipality muni = savedMunicipality("Muni", state);
        Parish parish = savedParish("Parish", muni);
        Branch branch = savedBranch("Branch", state, muni, parish);
        User user = savedUser("fired_employee", branch);
        Long userId = user.getId();
        flush();

        mockMvc.perform(delete("/api/users/" + userId))
                .andExpect(status().isNoContent());

        assertSoftDeleted("users", userId);
    }

    @Test
    @WithMockUser(authorities = "delete_user")
    void deleteUser_doesNotAppearInListing() throws Exception {
        State state = savedState("State2");
        Municipality muni = savedMunicipality("Muni2", state);
        Parish parish = savedParish("Parish2", muni);
        Branch branch = savedBranch("Branch2", state, muni, parish);
        User user = savedUser("hidden_user", branch);
        flush();

        mockMvc.perform(delete("/api/users/" + user.getId()))
                .andExpect(status().isNoContent());

        // After soft-delete, user should not appear in queries (SQLRestriction)
        assertNull(userRepository.findByUsername("hidden_user").orElse(null));
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Role tests
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "delete_role")
    void deleteRole_noUsers_softDeletes() throws Exception {
        Role role = savedRole("EmptyRole");
        flush();

        mockMvc.perform(delete("/api/roles/" + role.getId()))
                .andExpect(status().isNoContent());

        assertSoftDeleted("roles", role.getId());
    }

    @Test
    @WithMockUser(authorities = "delete_role")
    void deleteRole_withAssignedUser_returns409() throws Exception {
        State state = savedState("State");
        Municipality muni = savedMunicipality("Muni", state);
        Parish parish = savedParish("Parish", muni);
        Branch branch = savedBranch("Branch", state, muni, parish);
        User user = savedUser("role_user", branch);
        Role role = savedRole("RoleWithUser");
        user.getRoles().add(role);
        userRepository.save(user);
        flush();

        mockMvc.perform(delete("/api/roles/" + role.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("users")));
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Permission tests
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "delete_permission")
    void deletePermission_noRoles_softDeletes() throws Exception {
        Permission perm = savedPermission("empty_permission_" + System.currentTimeMillis());
        flush();

        mockMvc.perform(delete("/api/permissions/" + perm.getId()))
                .andExpect(status().isNoContent());

        assertSoftDeleted("permissions", perm.getId());
    }

    @Test
    @WithMockUser(authorities = "delete_permission")
    void deletePermission_withAssignedRole_returns409() throws Exception {
        Permission perm = savedPermission("perm_in_role_" + System.currentTimeMillis());
        Role role = savedRole("RoleWithPerm");
        role.getPermissions().add(perm);
        roleRepository.save(role);
        flush();

        mockMvc.perform(delete("/api/permissions/" + perm.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("roles")));
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Bag tests
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "delete_bag")
    void deleteBag_noActiveDisplacements_softDeletes() throws Exception {
        State state = savedState("State");
        Municipality muni = savedMunicipality("Muni", state);
        Parish parish = savedParish("Parish", muni);
        Branch branch = savedBranch("Branch", state, muni, parish);
        Department dept = savedDepartment("Dept", branch);
        Bag bag = savedBag("EmptyBag", "BAR003", branch, dept);
        flush();

        mockMvc.perform(delete("/api/bags/" + bag.getId()))
                .andExpect(status().isNoContent());

        assertSoftDeleted("bags", bag.getId());
    }

    @Test
    @WithMockUser(authorities = "delete_bag")
    void deleteBag_withActiveDisplacement_returns409() throws Exception {
        State state = savedState("State");
        Municipality muni = savedMunicipality("Muni", state);
        Parish parish = savedParish("Parish", muni);
        Branch branch = savedBranch("Branch", state, muni, parish);
        Department dept = savedDepartment("Dept", branch);
        Item item = savedItem("DispItem", branch, dept);
        Bag bag = savedBag("BagWithDisp", "BAR004", branch, dept);

        Displacement disp = new Displacement();
        disp.setItem(item);
        disp.setBag(bag);
        disp.setReason("Active displacement");
        disp.setBorrowerName("John Doe");
        disp.setExpectedReturnDate(java.time.OffsetDateTime.now().plusDays(3));
        displacementRepository.save(disp);
        flush();

        mockMvc.perform(delete("/api/bags/" + bag.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("displacements")));
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Displacement tests
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "delete_displacement")
    void deleteDisplacement_resolved_softDeletes() throws Exception {
        State state = savedState("State");
        Municipality muni = savedMunicipality("Muni", state);
        Parish parish = savedParish("Parish", muni);
        Branch branch = savedBranch("Branch", state, muni, parish);
        Department dept = savedDepartment("Dept", branch);
        Item item = savedItem("ResolvedItem", branch, dept);
        Bag bag = savedBag("ResolvedBag", "BAR005", branch, dept);

        Displacement disp = new Displacement();
        disp.setItem(item);
        disp.setBag(bag);
        disp.setReason("Done");
        disp.setBorrowerName("Jane");
        disp.setExpectedReturnDate(java.time.OffsetDateTime.now().plusDays(1));
        disp.setStatus(DisplacementStatus.RESOLVED);
        disp.setResolvedAt(java.time.OffsetDateTime.now());
        disp = displacementRepository.save(disp);
        flush();

        mockMvc.perform(delete("/api/displacements/" + disp.getId()))
                .andExpect(status().isNoContent());

        assertSoftDeleted("displacements", disp.getId());
    }

    @Test
    @WithMockUser(authorities = "delete_displacement")
    void deleteDisplacement_active_returns409() throws Exception {
        State state = savedState("State");
        Municipality muni = savedMunicipality("Muni", state);
        Parish parish = savedParish("Parish", muni);
        Branch branch = savedBranch("Branch", state, muni, parish);
        Department dept = savedDepartment("Dept", branch);
        Item item = savedItem("ActiveDispItem", branch, dept);
        Bag bag = savedBag("ActiveDispBag", "BAR006", branch, dept);

        Displacement disp = new Displacement();
        disp.setItem(item);
        disp.setBag(bag);
        disp.setReason("Still active");
        disp.setBorrowerName("Bob");
        disp.setExpectedReturnDate(java.time.OffsetDateTime.now().plusDays(5));
        // status defaults to ACTIVE
        disp = displacementRepository.save(disp);
        flush();

        mockMvc.perform(delete("/api/displacements/" + disp.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("active")));
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Branch with ItemRequest tests
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "delete_branch")
    void deleteBranch_withItemRequest_returns409() throws Exception {
        State state = savedState("State");
        Municipality muni = savedMunicipality("Muni", state);
        Parish parish = savedParish("Parish", muni);
        Branch branchTarget = savedBranch("BranchWithReq", state, muni, parish);
        // Requestor lives in a different branch so the target branch has no users
        Branch branchHelper = savedBranch("HelperBranch", state, muni, parish);
        User requestor = savedUser("requestor_user", branchHelper);

        ItemRequest req = new ItemRequest();
        req.setRequestType(ItemRequestType.INBOUND);
        req.setTitle("Test Request");
        req.setJustification("Test justification");
        req.setRequestedBy(requestor);
        req.setTargetBranch(branchTarget);
        itemRequestRepository.save(req);
        flush();

        mockMvc.perform(delete("/api/branches/" + branchTarget.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("item requests")));
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 404 for non-existent entities
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "delete_state")
    void deleteState_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/states/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "delete_user")
    void deleteUser_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/users/999999"))
                .andExpect(status().isNotFound());
    }
}
