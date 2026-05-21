package com.inventorymanager.frontend.ui;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.inventorymanager.backend.InventoryManagerBackendApplication;
import com.inventorymanager.frontend.api.ApiClient;
import com.inventorymanager.frontend.ui.views.FormView;
import com.inventorymanager.frontend.ui.views.ViewContext;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.testfx.framework.junit5.ApplicationTest;

@SpringBootTest(
        classes = InventoryManagerBackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:frontend-ui-inventory-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
                "spring.flyway.enabled=false"
        }
)
@ActiveProfiles("test")
class FrontendInventoryWorkflowInteractionTest extends ApplicationTest {
    private static final int SEARCH_PAGE_SIZE = 100;
    private static final int MAX_SEARCH_PAGES = 20;

    @LocalServerPort
    private int port;

    private Stage stage;
    private DesktopUi desktopUi;
    private ApiClient apiClient;
    private ViewContext viewContext;

    static class TestConfigManager extends ConfigManager {
        private final String apiUrl;

        TestConfigManager(String apiUrl) {
            this.apiUrl = apiUrl;
        }

        @Override
        public String getLanguage() {
            return "en";
        }

        @Override
        public String getApiUrl() {
            return apiUrl;
        }
    }

    @Test
    void inventoryWorkflowUsesFrontendInteractionsAgainstRealBackend() throws Exception {
        String baseUrl = backendApiBaseUrl();
        apiClient = new ApiClient(baseUrl);

        interact(() -> {
            stage = new Stage();
            desktopUi = new DesktopUi(stage, apiClient, new TestConfigManager(baseUrl));
            desktopUi.showLogin();
        });

        loginAsAdmin();
        viewContext = readPrivateField(desktopUi, "viewContext", ViewContext.class);
        assertNotNull(viewContext);

        String stateName = "WF State";
        String municipalityName = "WF Municipality";
        String parishName = "WF Parish";

        apiClient.create("states", Map.of("name", stateName));
        Map<String, Object> createdState = waitForEntityByField("states", "name", stateName, Duration.ofSeconds(10));
        apiClient.create("municipalities", Map.of(
                "name", municipalityName,
                "stateId", numericId(createdState, "id")
        ));
        Map<String, Object> createdMunicipality = waitForEntityByField("municipalities", "name", municipalityName, Duration.ofSeconds(10));
        apiClient.create("parishes", Map.of(
                "name", parishName,
                "municipalityId", numericId(createdMunicipality, "id")
        ));

        String categoryOriginal = "WF Category Original";
        String categoryEdited = "WF Category Edited";
        createNamedEntityThroughFrontend("Categories", "categories", categoryOriginal);
        Map<String, Object> createdCategory = waitForEntityByField("categories", "name", categoryOriginal, Duration.ofSeconds(10));
        editNamedEntityThroughFrontend("Categories", "categories", createdCategory, categoryEdited);
        waitForEntityByField("categories", "name", categoryEdited, Duration.ofSeconds(10));

        String branchOriginal = "WF Branch Original";
        String branchEdited = "WF Branch Edited";
        createBranchThroughFrontend(branchOriginal, "WF Address Original", stateName, municipalityName, parishName);
        Map<String, Object> createdBranch = waitForEntityByField("branches", "name", branchOriginal, Duration.ofSeconds(10));
        editBranchThroughFrontend(createdBranch, branchEdited, "WF Address Edited", stateName, municipalityName, parishName);
        waitForEntityByField("branches", "name", branchEdited, Duration.ofSeconds(10));

        String departmentOriginal = "WF Department Original";
        String departmentEdited = "WF Department Edited";
        createDepartmentThroughFrontend(departmentOriginal, branchEdited);
        Map<String, Object> createdDepartment = waitForEntityByField("departments", "name", departmentOriginal, Duration.ofSeconds(10));
        editDepartmentThroughFrontend(createdDepartment, departmentEdited, branchEdited);
        waitForEntityByField("departments", "name", departmentEdited, Duration.ofSeconds(10));

        String itemOriginal = "WF Item Original";
        String itemEdited = "WF Item Edited";
        createItemThroughFrontend(itemOriginal, "5", "UND", branchEdited, departmentEdited);
        Map<String, Object> createdItem = waitForEntityByField("items", "name", itemOriginal, Duration.ofSeconds(10));
        editItemThroughFrontend(createdItem, itemEdited, "11", "KG", branchEdited, departmentEdited);
        waitForEntityByField("items", "name", itemEdited, Duration.ofSeconds(10));

        String bagOriginal = "WF Bag Original";
        String bagEdited = "WF Bag Edited";
        createBagThroughFrontend(bagOriginal, "WF-BAR-001", branchEdited, departmentEdited);
        Map<String, Object> createdBag = waitForEntityByField("bags", "name", bagOriginal, Duration.ofSeconds(10));
        editBagThroughFrontend(createdBag, bagEdited, "WF-BAR-002", branchEdited, departmentEdited);
        waitForEntityByField("bags", "name", bagEdited, Duration.ofSeconds(10));

        String borrowerOriginal = "WF Borrower Original";
        String borrowerEdited = "WF Borrower Edited";
        createDisplacementThroughFrontend(itemEdited, borrowerOriginal, "WF reason original");
        Map<String, Object> createdDisplacement = waitForEntityByField("displacements", "borrowerName", borrowerOriginal, Duration.ofSeconds(10));
        editDisplacementThroughFrontend(createdDisplacement, itemEdited, borrowerEdited, "WF reason edited");
        Map<String, Object> editedDisplacement = waitForEntityByField("displacements", "borrowerName", borrowerEdited, Duration.ofSeconds(10));

        assertNotNull(editedDisplacement);
        assertTrue(String.valueOf(editedDisplacement.getOrDefault("borrowerName", "")).contains("WF Borrower Edited"));
    }

    private void loginAsAdmin() {
        waitUntil(() -> exists("#login-signin"), Duration.ofSeconds(10), "Login view did not render");
        clickOn("#login-username");
        eraseText(32);
        write("admin");
        clickOn("#login-password");
        eraseText(32);
        write("password");
        clickOn("#login-signin");
        waitUntil(() -> findButtonContaining("States").isPresent(), Duration.ofSeconds(15), "Dashboard did not load admin navigation");
    }

    private void createNamedEntityThroughFrontend(String title, String resource, String name) {
        interact(() -> new FormView(viewContext).showUpsertForm(title, resource, null));
        waitUntil(() -> visibleTextFieldCount() > 0, Duration.ofSeconds(10), "Named form did not render");
        setTextField(0, name);
        clickButtonContaining("Save");
    }

    private void editNamedEntityThroughFrontend(String title, String resource, Map<String, Object> rowData, String newName) {
        interact(() -> new FormView(viewContext).showUpsertForm(title, resource, rowData));
        waitUntil(() -> visibleTextFieldCount() > 0, Duration.ofSeconds(10), "Named edit form did not render");
        setTextField(0, newName);
        clickButtonContaining("Save");
    }

    private void createBranchThroughFrontend(String name, String address, String stateName, String municipalityName, String parishName) {
        interact(() -> new FormView(viewContext).showUpsertForm("Branches", "branches", null));
        waitUntil(() -> visibleTextFieldCount() >= 1, Duration.ofSeconds(10), "Branch form did not render");
        waitUntil(() -> visibleTextAreaCount() >= 1, Duration.ofSeconds(10), "Branch address did not render");
        waitUntil(() -> visibleComboCount() >= 3, Duration.ofSeconds(10), "Branch combos did not render");
        setTextField(0, name);
        setTextArea(0, address);
        selectComboValue(0, stateName);
        waitUntil(() -> comboHasValue(1, municipalityName), Duration.ofSeconds(10), "Municipality combo did not load");
        selectComboValue(1, municipalityName);
        waitUntil(() -> comboHasValue(2, parishName), Duration.ofSeconds(10), "Parish combo did not load");
        selectComboValue(2, parishName);
        clickButtonContaining("Save");
    }

    private void editBranchThroughFrontend(Map<String, Object> rowData, String name, String address, String stateName, String municipalityName, String parishName) {
        interact(() -> new FormView(viewContext).showUpsertForm("Branches", "branches", rowData));
        waitUntil(() -> visibleTextFieldCount() >= 1, Duration.ofSeconds(10), "Branch edit form did not render");
        waitUntil(() -> visibleTextAreaCount() >= 1, Duration.ofSeconds(10), "Branch edit address did not render");
        waitUntil(() -> visibleComboCount() >= 3, Duration.ofSeconds(10), "Branch edit combos did not render");
        setTextField(0, name);
        setTextArea(0, address);
        selectComboValue(0, stateName);
        waitUntil(() -> comboHasValue(1, municipalityName), Duration.ofSeconds(10), "Branch municipality combo did not load");
        selectComboValue(1, municipalityName);
        waitUntil(() -> comboHasValue(2, parishName), Duration.ofSeconds(10), "Branch parish combo did not load");
        selectComboValue(2, parishName);
        clickButtonContaining("Save");
    }

    private void createDepartmentThroughFrontend(String name, String branchName) {
        interact(() -> new FormView(viewContext).showUpsertForm("Departments", "departments", null));
        waitUntil(() -> visibleTextFieldCount() >= 1, Duration.ofSeconds(10), "Department form did not render");
        waitUntil(() -> visibleComboCount() >= 1, Duration.ofSeconds(10), "Department branch combo did not render");
        setTextField(0, name);
        selectComboValue(0, branchName);
        clickButtonContaining("Save");
    }

    private void editDepartmentThroughFrontend(Map<String, Object> rowData, String name, String branchName) {
        interact(() -> new FormView(viewContext).showUpsertForm("Departments", "departments", rowData));
        waitUntil(() -> visibleTextFieldCount() >= 1, Duration.ofSeconds(10), "Department edit form did not render");
        waitUntil(() -> visibleComboCount() >= 1, Duration.ofSeconds(10), "Department edit branch combo did not render");
        setTextField(0, name);
        selectComboValue(0, branchName);
        clickButtonContaining("Save");
    }

    private void createItemThroughFrontend(String name, String quantity, String unit, String branchName, String departmentName) {
        interact(() -> new FormView(viewContext).showUpsertForm("Assets", "items", null));
        waitUntil(() -> visibleTextFieldCount() >= 2, Duration.ofSeconds(10), "Item form did not render");
        waitUntil(() -> visibleComboCount() >= 3, Duration.ofSeconds(10), "Item combos did not render");
        setTextField(0, name);
        setTextField(1, quantity);
        selectComboValue(0, unit);
        selectComboValue(1, branchName);
        waitUntil(() -> comboHasValue(2, departmentName), Duration.ofSeconds(10), "Item department combo did not load");
        selectComboValue(2, departmentName);
        clickButtonContaining("Save");
    }

    private void editItemThroughFrontend(Map<String, Object> rowData, String name, String quantity, String unit, String branchName, String departmentName) {
        interact(() -> new FormView(viewContext).showUpsertForm("Assets", "items", rowData));
        waitUntil(() -> visibleTextFieldCount() >= 2, Duration.ofSeconds(10), "Item edit form did not render");
        waitUntil(() -> visibleComboCount() >= 3, Duration.ofSeconds(10), "Item edit combos did not render");
        setTextField(0, name);
        setTextField(1, quantity);
        selectComboValue(0, unit);
        selectComboValue(1, branchName);
        waitUntil(() -> comboHasValue(2, departmentName), Duration.ofSeconds(10), "Item edit department combo did not load");
        selectComboValue(2, departmentName);
        clickButtonContaining("Save");
    }

    private void createBagThroughFrontend(String name, String barcode, String branchName, String departmentName) {
        interact(() -> new FormView(viewContext).showUpsertForm("Bags", "bags", null));
        waitUntil(() -> visibleTextFieldCount() >= 2, Duration.ofSeconds(10), "Bag form did not render");
        waitUntil(() -> visibleComboCount() >= 2, Duration.ofSeconds(10), "Bag combos did not render");
        setTextField(0, name);
        setTextField(1, barcode);
        selectComboValue(0, branchName);
        waitUntil(() -> comboHasValue(1, departmentName), Duration.ofSeconds(10), "Bag department combo did not load");
        selectComboValue(1, departmentName);
        clickButtonContaining("Save");
    }

    private void editBagThroughFrontend(Map<String, Object> rowData, String name, String barcode, String branchName, String departmentName) {
        interact(() -> new FormView(viewContext).showUpsertForm("Bags", "bags", rowData));
        waitUntil(() -> visibleTextFieldCount() >= 2, Duration.ofSeconds(10), "Bag edit form did not render");
        waitUntil(() -> visibleComboCount() >= 2, Duration.ofSeconds(10), "Bag edit combos did not render");
        setTextField(0, name);
        setTextField(1, barcode);
        selectComboValue(0, branchName);
        waitUntil(() -> comboHasValue(1, departmentName), Duration.ofSeconds(10), "Bag edit department combo did not load");
        selectComboValue(1, departmentName);
        clickButtonContaining("Save");
    }

    private void createDisplacementThroughFrontend(String itemName, String borrower, String reason) {
        interact(() -> new FormView(viewContext).showUpsertForm("Displacements", "displacements", null));
        waitUntil(() -> visibleComboCount() >= 1, Duration.ofSeconds(10), "Displacement item combo did not render");
        waitUntil(() -> comboHasValue(0, itemName), Duration.ofSeconds(10), "Displacement item options did not load");
        waitUntil(() -> visibleTextFieldCount() >= 1, Duration.ofSeconds(10), "Displacement borrower did not render");
        waitUntil(() -> visibleTextAreaCount() >= 1, Duration.ofSeconds(10), "Displacement reason did not render");
        selectComboValue(0, itemName);
        setTextField(0, borrower);
        setTextArea(0, reason);
        clickButtonContaining("Save");
    }

    private void editDisplacementThroughFrontend(Map<String, Object> rowData, String itemName, String borrower, String reason) {
        interact(() -> new FormView(viewContext).showUpsertForm("Displacements", "displacements", rowData));
        waitUntil(() -> visibleComboCount() >= 1, Duration.ofSeconds(10), "Displacement edit combo did not render");
        waitUntil(() -> comboHasValue(0, itemName), Duration.ofSeconds(10), "Displacement edit item options did not load");
        waitUntil(() -> visibleTextFieldCount() >= 1, Duration.ofSeconds(10), "Displacement edit borrower did not render");
        waitUntil(() -> visibleTextAreaCount() >= 1, Duration.ofSeconds(10), "Displacement edit reason did not render");
        selectComboValue(0, itemName);
        setTextField(0, borrower);
        setTextArea(0, reason);
        clickButtonContaining("Save");
    }

    private void setTextField(int index, String value) {
        waitUntil(() -> visibleTextFieldCount() > index, Duration.ofSeconds(10), "Expected text field index not visible: " + index);
        interact(() -> {
            List<TextField> fields = lookup(".text-field").queryAllAs(TextField.class).stream().filter(TextField::isVisible).toList();
            TextField field = fields.get(index);
            field.clear();
            field.setText(value);
        });
    }

    private void setTextArea(int index, String value) {
        waitUntil(() -> visibleTextAreaCount() > index, Duration.ofSeconds(10), "Expected text area index not visible: " + index);
        interact(() -> {
            List<TextArea> areas = lookup(".text-area").queryAllAs(TextArea.class).stream().filter(TextArea::isVisible).toList();
            TextArea area = areas.get(index);
            area.clear();
            area.setText(value);
        });
    }

    private void selectComboValue(int comboIndex, String text) {
        waitUntil(() -> visibleComboCount() > comboIndex, Duration.ofSeconds(10), "Expected combo index not visible: " + comboIndex);
        interact(() -> {
            List<ComboBox> combos = lookup(".combo-box").queryAllAs(ComboBox.class).stream().filter(ComboBox::isVisible).toList();
            ComboBox combo = combos.get(comboIndex);
            combo.show();
        });
        clickOn(text);
    }

    private boolean comboHasValue(int comboIndex, String text) {
        return interactRead(() -> {
            List<ComboBox> combos = lookup(".combo-box").queryAllAs(ComboBox.class).stream().filter(ComboBox::isVisible).toList();
            if (combos.size() <= comboIndex) return false;
            return combos.get(comboIndex).getItems().stream().anyMatch(v -> String.valueOf(v).contains(text));
        });
    }

    private int visibleTextFieldCount() {
        return interactRead(() -> lookup(".text-field").queryAllAs(TextField.class).stream().filter(TextField::isVisible).toList().size());
    }

    private int visibleTextAreaCount() {
        return interactRead(() -> lookup(".text-area").queryAllAs(TextArea.class).stream().filter(TextArea::isVisible).toList().size());
    }

    private int visibleComboCount() {
        return interactRead(() -> lookup(".combo-box").queryAllAs(ComboBox.class).stream().filter(ComboBox::isVisible).toList().size());
    }

    private Map<String, Object> waitForEntityByField(String resource, String field, String value, Duration timeout) {
        waitUntil(() -> findEntityByField(resource, field, value).isPresent(), timeout, "Expected entity not found: " + resource + " -> " + field + "=" + value);
        return findEntityByField(resource, field, value).orElseThrow();
    }

    private Optional<Map<String, Object>> findEntityByField(String resource, String field, String value) {
        try {
            for (int page = 1; page <= MAX_SEARCH_PAGES; page++) {
                List<Map<String, Object>> rows = apiClient.list(resource + "?page=" + page + "&pageSize=" + SEARCH_PAGE_SIZE);
                Optional<Map<String, Object>> match = rows.stream()
                        .filter(row -> value.equals(String.valueOf(row.get(field))))
                        .findFirst();
                if (match.isPresent()) {
                    return match;
                }
                if (rows.size() < SEARCH_PAGE_SIZE) {
                    return Optional.empty();
                }
            }
            return Optional.empty();
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private void clickButtonContaining(String fragment) {
        waitUntil(() -> findButtonContaining(fragment).isPresent(), Duration.ofSeconds(10), "Missing button containing: " + fragment);
        interact(() -> findButtonContaining(fragment).orElseThrow().requestFocus());
        clickOn(findButtonContaining(fragment).orElseThrow());
    }

    private Optional<Button> findButtonContaining(String fragment) {
        String normalized = fragment.toLowerCase();
        return lookup(".button").queryAllAs(Button.class).stream()
                .filter(Button::isVisible)
                .filter(b -> b.getText() != null && b.getText().toLowerCase().contains(normalized))
                .findFirst();
    }

    private boolean exists(String query) {
        return !lookup(query).queryAll().isEmpty();
    }

    private Long numericId(Map<String, Object> row, String key) {
        return ((Number) row.get(key)).longValue();
    }

    private String backendApiBaseUrl() {
        return "http://localhost:" + port + "/api";
    }

    private void waitUntil(Callable<Boolean> predicate, Duration timeout, String failureMessage) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            try {
                if (predicate.call()) return;
                Thread.sleep(100);
            } catch (Exception ignored) {
                // keep polling
            }
        }
        throw new AssertionError(failureMessage);
    }

    @SuppressWarnings("unchecked")
    private <T> T readPrivateField(Object target, String fieldName, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(target);
    }

    private <T> T interactRead(Callable<T> callable) {
        final Object[] holder = new Object[1];
        interact(() -> {
            try {
                holder[0] = callable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        @SuppressWarnings("unchecked")
        T cast = (T) holder[0];
        return cast;
    }

    @AfterEach
    void closeStage() {
        if (stage != null) {
            interact(stage::close);
        }
    }
}
