package com.inventorymanager.frontend.ui;

import com.inventorymanager.backend.InventoryManagerBackendApplication;
import com.inventorymanager.frontend.api.ApiClient;
import com.inventorymanager.frontend.ui.views.FormView;
import com.inventorymanager.frontend.ui.views.ViewContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.testfx.framework.junit5.ApplicationTest;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Base class for frontend feature tests that combine a real Spring Boot backend
 * (H2 in-memory) with TestFX UI interactions.
 * <p>
 * Each test:
 * <ul>
 *   <li>Starts a real backend on a random port with an H2 in-memory database</li>
 *   <li>Launches the JavaFX UI (DesktopUi) via TestFX</li>
 *   <li>Logs in as admin (seeded by AdminSeedRunner)</li>
 *   <li>Provides helpers to create entities through the FormView and verify via the ApiClient</li>
 * </ul>
 */
@SpringBootTest(
        classes = InventoryManagerBackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:feature-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
                "spring.flyway.enabled=false"
        }
)
@ActiveProfiles("test")
public abstract class FeatureTestBase extends ApplicationTest {

    protected static final int SEARCH_PAGE_SIZE = 100;
    protected static final int MAX_SEARCH_PAGES = 20;

    @LocalServerPort
    protected int port;

    protected Stage stage;
    protected DesktopUi desktopUi;
    protected ApiClient apiClient;
    protected ViewContext viewContext;

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

    /**
     * Initialises the JavaFX stage, desktop UI, and logs in as admin.
     */
    protected void initDesktopUiAndLogin() throws Exception {
        String baseUrl = backendApiBaseUrl();
        apiClient = new ApiClient(baseUrl);

        interact(() -> {
            stage = new Stage();
            desktopUi = new DesktopUi(stage, apiClient, new TestConfigManager(baseUrl));
            desktopUi.showLogin();
        });

        loginAsAdmin();
        viewContext = readPrivateField(desktopUi, "viewContext", ViewContext.class);
    }

    protected void loginAsAdmin() {
        waitUntil(() -> exists("#login-signin"), Duration.ofSeconds(10), "Login view did not render");
        clickOn("#login-username");
        eraseText(32);
        write("admin");
        clickOn("#login-password");
        eraseText(32);
        write("password");
        clickOn("#login-signin");
        waitUntil(() -> findButtonContaining("States").isPresent(), Duration.ofSeconds(15),
                "Dashboard did not load admin navigation");
    }

    // ─── Form helpers ────────────────────────────────────────────────────

    /** Opens a named upsert form (State, Category, Role, Permission) and saves. */
    protected void createNamedThroughForm(String title, String resource, String name) {
        interact(() -> new FormView(viewContext).showUpsertForm(title, resource, null));
        waitUntil(() -> visibleTextFieldCount() > 0, Duration.ofSeconds(10), "Form did not render for " + resource);
        setFirstTextField(name);
        clickButtonContaining("Save");
    }

    /** Edits a named entity through the upsert form. */
    protected void editNamedThroughForm(String title, String resource, Map<String, Object> rowData, String newName) {
        interact(() -> new FormView(viewContext).showUpsertForm(title, resource, rowData));
        waitUntil(() -> visibleTextFieldCount() > 0, Duration.ofSeconds(10), "Edit form did not open for " + resource);
        setFirstTextField(newName);
        clickButtonContaining("Save");
    }

    /** Creates a State through the UI. */
    protected void createStateThroughForm(String name) {
        createNamedThroughForm("States", "states", name);
    }

    /** Creates a Municipality through the UI with the given parent state name. */
    protected void createMunicipalityThroughForm(String name, String stateName) {
        interact(() -> new FormView(viewContext).showUpsertForm("Municipalities", "municipalities", null));
        waitUntil(() -> visibleTextFieldCount() > 0, Duration.ofSeconds(10), "Municipality form did not render");
        setFirstTextField(name);
        waitUntil(() -> visibleComboCount() >= 1, Duration.ofSeconds(10), "State combo did not render");
        selectComboValue(0, stateName);
        clickButtonContaining("Save");
    }

    /** Creates a Parish through the UI with parent state and municipality. */
    protected void createParishThroughForm(String name, String stateName, String municipalityName) {
        interact(() -> new FormView(viewContext).showUpsertForm("Parishes", "parishes", null));
        waitUntil(() -> visibleTextFieldCount() > 0, Duration.ofSeconds(10), "Parish form did not render");
        setFirstTextField(name);
        waitUntil(() -> visibleComboCount() >= 2, Duration.ofSeconds(10), "Parish combos did not render");
        selectComboValue(0, stateName);
        waitUntil(() -> comboHasValue(1, municipalityName), Duration.ofSeconds(10), "Municipality combo did not load");
        selectComboValue(1, municipalityName);
        clickButtonContaining("Save");
    }

    /** Creates a Branch through the UI with address and location hierarchy. */
    protected void createBranchThroughForm(String name, String address, String stateName,
                                            String municipalityName, String parishName) {
        interact(() -> new FormView(viewContext).showUpsertForm("Branches", "branches", null));
        waitUntil(() -> visibleTextFieldCount() >= 1, Duration.ofSeconds(10), "Branch form did not render");
        waitUntil(() -> visibleTextAreaCount() >= 1, Duration.ofSeconds(10), "Branch address field did not render");
        waitUntil(() -> visibleComboCount() >= 3, Duration.ofSeconds(10), "Branch combos did not render");
        setFirstTextField(name);
        setFirstTextArea(address);
        selectComboValue(0, stateName);
        waitUntil(() -> comboHasValue(1, municipalityName), Duration.ofSeconds(10), "Municipality combo did not load");
        selectComboValue(1, municipalityName);
        waitUntil(() -> comboHasValue(2, parishName), Duration.ofSeconds(10), "Parish combo did not load");
        selectComboValue(2, parishName);
        clickButtonContaining("Save");
    }

    /** Creates a Department through the UI linked to the given branch. */
    protected void createDepartmentThroughForm(String name, String branchName) {
        interact(() -> new FormView(viewContext).showUpsertForm("Departments", "departments", null));
        waitUntil(() -> visibleTextFieldCount() > 0, Duration.ofSeconds(10), "Department form did not render");
        setFirstTextField(name);
        waitUntil(() -> visibleComboCount() >= 1, Duration.ofSeconds(10), "Branch combo did not render");
        selectComboValue(0, branchName);
        clickButtonContaining("Save");
    }

    // ─── API verification helpers ────────────────────────────────────────

    /** Waits for an entity with the given "name" field to appear in list results. */
    protected Map<String, Object> waitForEntityByName(String resource, String name, Duration timeout) {
        waitUntil(() -> findEntityByName(resource, name).isPresent(), timeout,
                "Expected entity not found: " + resource + " -> " + name);
        return findEntityByName(resource, name).orElseThrow();
    }

    /** Waits for an entity with the given "id" field to appear in list results. */
    protected Map<String, Object> waitForEntityById(String resource, long id, Duration timeout) {
        waitUntil(() -> findEntityById(resource, id).isPresent(), timeout,
                "Expected entity not found: " + resource + " -> id=" + id);
        return findEntityById(resource, id).orElseThrow();
    }

    /** Waits for an entity to no longer appear in list results (e.g. after soft-delete). */
    protected void waitForEntityAbsent(String resource, String name, Duration timeout) {
        waitUntil(() -> findEntityByName(resource, name).isEmpty(), timeout,
                "Entity still present after expected deletion: " + resource + " -> " + name);
    }

    protected Optional<Map<String, Object>> findEntityByName(String resource, String name) {
        try {
            for (int page = 1; page <= MAX_SEARCH_PAGES; page++) {
                List<Map<String, Object>> rows = apiClient.list(resource + "?page=" + page + "&pageSize=" + SEARCH_PAGE_SIZE);
                Optional<Map<String, Object>> match = rows.stream()
                        .filter(row -> name.equals(String.valueOf(row.get("name"))))
                        .findFirst();
                if (match.isPresent()) return match;
                if (rows.size() < SEARCH_PAGE_SIZE) break;
            }
            return Optional.empty();
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    protected Optional<Map<String, Object>> findEntityById(String resource, long id) {
        try {
            for (int page = 1; page <= MAX_SEARCH_PAGES; page++) {
                List<Map<String, Object>> rows = apiClient.list(resource + "?page=" + page + "&pageSize=" + SEARCH_PAGE_SIZE);
                Optional<Map<String, Object>> match = rows.stream()
                        .filter(row -> {
                            Object rowId = row.get("id");
                            return rowId != null && String.valueOf(rowId).equals(String.valueOf(id));
                        })
                        .findFirst();
                if (match.isPresent()) return match;
                if (rows.size() < SEARCH_PAGE_SIZE) break;
            }
            return Optional.empty();
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    /** Counts the total number of entities returned by list (all pages). */
    protected long countEntities(String resource) {
        long count = 0;
        try {
            for (int page = 1; page <= MAX_SEARCH_PAGES; page++) {
                List<Map<String, Object>> rows = apiClient.list(resource + "?page=" + page + "&pageSize=" + SEARCH_PAGE_SIZE);
                count += rows.size();
                if (rows.size() < SEARCH_PAGE_SIZE) break;
            }
        } catch (Exception ignored) {
        }
        return count;
    }

    // ─── TestFX UI helpers ──────────────────────────────────────────────

    protected void setFirstTextField(String value) {
        waitUntil(() -> visibleTextFieldCount() > 0, Duration.ofSeconds(10), "No visible text field found");
        interact(() -> {
            List<TextField> fields = lookup(".text-field").queryAllAs(TextField.class).stream()
                    .filter(TextField::isVisible).toList();
            TextField field = fields.get(0);
            field.clear();
            field.setText(value);
        });
    }

    protected int visibleTextFieldCount() {
        return interactRead(() -> (int) lookup(".text-field").queryAllAs(TextField.class).stream()
                .filter(TextField::isVisible).count());
    }

    protected int visibleTextAreaCount() {
        return interactRead(() -> (int) lookup(".text-area").queryAll().stream()
                .filter(n -> n.isVisible()).count());
    }

    protected void setFirstTextArea(String value) {
        waitUntil(() -> visibleTextAreaCount() > 0, Duration.ofSeconds(10), "No visible text area found");
        javafx.scene.control.TextArea area = interactRead(() ->
                lookup(".text-area").queryAll().stream()
                        .filter(javafx.scene.Node::isVisible)
                        .map(n -> (javafx.scene.control.TextArea) n)
                        .findFirst().orElseThrow());
        interact(() -> {
            area.clear();
            area.setText(value);
        });
    }

    protected int visibleComboCount() {
        return interactRead(() -> (int) lookup(".combo-box").queryAllAs(ComboBox.class).stream()
                .filter(ComboBox::isVisible).count());
    }

    protected void selectComboValue(int comboIndex, String text) {
        waitUntil(() -> visibleComboCount() > comboIndex, Duration.ofSeconds(10),
                "Expected combo index not visible: " + comboIndex);
        interact(() -> {
            List<ComboBox> combos = lookup(".combo-box").queryAllAs(ComboBox.class).stream()
                    .filter(ComboBox::isVisible).toList();
            combos.get(comboIndex).show();
        });
        clickOn(text);
    }

    protected boolean comboHasValue(int comboIndex, String text) {
        return interactRead(() -> {
            List<ComboBox> combos = lookup(".combo-box").queryAllAs(ComboBox.class).stream()
                    .filter(ComboBox::isVisible).toList();
            if (combos.size() <= comboIndex) return false;
            return combos.get(comboIndex).getItems().stream()
                    .anyMatch(v -> String.valueOf(v).contains(text));
        });
    }

    protected void clickButtonContaining(String fragment) {
        waitUntil(() -> findButtonContaining(fragment).isPresent(), Duration.ofSeconds(10),
                "Missing button containing: " + fragment);
        Button btn = findButtonContaining(fragment).orElseThrow();
        interact(btn::requestFocus);
        clickOn(btn);
    }

    protected Optional<Button> findButtonContaining(String fragment) {
        String normalized = fragment.toLowerCase();
        return lookup(".button").queryAllAs(Button.class).stream()
                .filter(Button::isVisible)
                .filter(b -> b.getText() != null && b.getText().toLowerCase().contains(normalized))
                .findFirst();
    }

    protected boolean exists(String query) {
        return !lookup(query).queryAll().isEmpty();
    }

    protected String backendApiBaseUrl() {
        return "http://localhost:" + port + "/api";
    }

    /**
     * Polls until a predicate is true or the timeout expires.
     */
    protected void waitUntil(Callable<Boolean> predicate, Duration timeout, String failureMessage) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            try {
                if (predicate.call()) return;
                Thread.sleep(100);
            } catch (Exception ignored) {
            }
        }
        throw new AssertionError(failureMessage);
    }

    @SuppressWarnings("unchecked")
    protected <T> T readPrivateField(Object target, String fieldName, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(target);
    }

    protected <T> T interactRead(Callable<T> callable) {
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
