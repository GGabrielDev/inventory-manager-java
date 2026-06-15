package com.inventorymanager.frontend.ui;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.inventorymanager.backend.InventoryManagerBackendApplication;
import com.inventorymanager.frontend.api.ApiClient;
import com.inventorymanager.frontend.ui.views.FormView;
import com.inventorymanager.frontend.ui.views.ResourceView;
import com.inventorymanager.frontend.ui.views.ViewContext;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
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
                "spring.datasource.url=jdbc:h2:mem:frontend-ui-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
                "spring.flyway.enabled=false",
                "spring.jpa.open-in-view=true"
        }
)
@ActiveProfiles("test")
class FrontendLocationPipelineInteractionTest extends ApplicationTest {
    private static final int SEARCH_PAGE_SIZE = 100;
    private static final int MAX_SEARCH_PAGES = 20;
    private static final int DIALOG_DISMISS_ATTEMPTS = 3;

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
    void locationWorkflowUsesFrontendInteractionsAgainstRealBackend() throws Exception {
        String baseUrl = backendApiBaseUrl();
        apiClient = new ApiClient(baseUrl);

        interact(() -> {
            stage = new Stage();
            desktopUi = new DesktopUi(stage, apiClient, new TestConfigManager(baseUrl));
            desktopUi.showLogin();
        });

        waitUntil(() -> exists("#login-signin"), Duration.ofSeconds(10), "Login view did not render");
        clickOn("#login-username");
        eraseText(32);
        write("admin");
        clickOn("#login-password");
        eraseText(32);
        write("password");
        clickOn("#login-signin");

        waitUntil(() -> findButtonContaining("States").isPresent(), Duration.ofSeconds(15), "Dashboard did not load admin navigation");
        viewContext = readPrivateField(desktopUi, "viewContext", ViewContext.class);
        assertNotNull(viewContext);

        String stateOriginal = "E2E State Original";
        String stateEdited = "E2E State Edited";
        String municipalityOriginal = "E2E Municipality Original";
        String municipalityEdited = "E2E Municipality Edited";
        String parishOriginal = "E2E Parish Original";
        String parishEdited = "E2E Parish Edited";

        createStateThroughFrontend(stateOriginal);
        Map<String, Object> createdState = waitForEntityByName("states", stateOriginal, Duration.ofSeconds(10));
        editNamedEntityThroughFrontendForm("States", "states", createdState, stateEdited);
        Map<String, Object> editedState = waitForEntityByName("states", stateEdited, Duration.ofSeconds(10));

        createMunicipalityThroughFrontend(municipalityOriginal, stateEdited);
        Map<String, Object> createdMunicipality = waitForEntityByName("municipalities", municipalityOriginal, Duration.ofSeconds(10));
        editMunicipalityThroughFrontendForm(createdMunicipality, municipalityEdited, stateEdited);
        waitForEntityByName("municipalities", municipalityEdited, Duration.ofSeconds(10));

        createParishThroughFrontend(parishOriginal, stateEdited, municipalityEdited);
        Map<String, Object> createdParish = waitForEntityByName("parishes", parishOriginal, Duration.ofSeconds(10));
        editParishThroughFrontendForm(createdParish, parishEdited, stateEdited, municipalityEdited);
        waitForEntityByName("parishes", parishEdited, Duration.ofSeconds(10));

        attemptDeleteStateThroughResourceViewAndConfirm(editedState);
        dismissDialogsIfOpen();

        Map<String, Object> stateAfterDeleteAttempt = waitForEntityByName("states", stateEdited, Duration.ofSeconds(10));
        assertNotNull(stateAfterDeleteAttempt, "State should remain because municipality/parish references prevent deletion");
    }

    private void createStateThroughFrontend(String stateName) {
        interact(() -> new FormView(viewContext).showUpsertForm("States", "states", null));
        waitUntil(() -> visibleTextFieldCount() > 0, Duration.ofSeconds(10), "State form did not render");
        setFirstTextField(stateName);
        clickButtonContaining("Save");
    }

    private void createMunicipalityThroughFrontend(String municipalityName, String stateName) {
        interact(() -> new FormView(viewContext).showUpsertForm("Municipalities", "municipalities", null));
        waitUntil(() -> visibleTextFieldCount() > 0, Duration.ofSeconds(10), "Municipality form did not render");
        setFirstTextField(municipalityName);
        waitUntil(() -> visibleComboCount() >= 1, Duration.ofSeconds(10), "Municipality state combo did not render");
        selectComboValue(0, stateName);
        clickButtonContaining("Save");
    }

    private void createParishThroughFrontend(String parishName, String stateName, String municipalityName) {
        interact(() -> new FormView(viewContext).showUpsertForm("Parishes", "parishes", null));
        waitUntil(() -> visibleTextFieldCount() > 0, Duration.ofSeconds(10), "Parish form did not render");
        setFirstTextField(parishName);
        waitUntil(() -> visibleComboCount() >= 2, Duration.ofSeconds(10), "Parish combos did not render");
        selectComboValue(0, stateName);
        waitUntil(() -> comboHasValue(1, municipalityName), Duration.ofSeconds(10), "Municipality combo did not load dependent values");
        selectComboValue(1, municipalityName);
        clickButtonContaining("Save");
    }

    private void editNamedEntityThroughFrontendForm(String title, String resource, Map<String, Object> rowData, String newName) {
        interact(() -> new FormView(viewContext).showUpsertForm(title, resource, rowData));
        waitUntil(() -> visibleTextFieldCount() > 0, Duration.ofSeconds(10), "Edit form did not open");
        setFirstTextField(newName);
        clickButtonContaining("Save");
    }

    private void editMunicipalityThroughFrontendForm(Map<String, Object> rowData, String newName, String stateName) {
        interact(() -> new FormView(viewContext).showUpsertForm("Municipalities", "municipalities", rowData));
        waitUntil(() -> visibleTextFieldCount() > 0, Duration.ofSeconds(10), "Municipality edit form did not open");
        setFirstTextField(newName);
        waitUntil(() -> visibleComboCount() >= 1, Duration.ofSeconds(10), "Municipality edit combo did not render");
        selectComboValue(0, stateName);
        clickButtonContaining("Save");
    }

    private void editParishThroughFrontendForm(Map<String, Object> rowData, String newName, String stateName, String municipalityName) {
        interact(() -> new FormView(viewContext).showUpsertForm("Parishes", "parishes", rowData));
        waitUntil(() -> visibleTextFieldCount() > 0, Duration.ofSeconds(10), "Parish edit form did not open");
        setFirstTextField(newName);
        waitUntil(() -> visibleComboCount() >= 2, Duration.ofSeconds(10), "Parish edit combos did not render");
        selectComboValue(0, stateName);
        waitUntil(() -> comboHasValue(1, municipalityName), Duration.ofSeconds(10), "Municipality combo did not load for parish edit");
        selectComboValue(1, municipalityName);
        clickButtonContaining("Save");
    }

    private void attemptDeleteStateThroughResourceViewAndConfirm(Map<String, Object> stateRow) throws Exception {
        ResourceView resourceView = new ResourceView(viewContext, "States", "states");
        interact(resourceView::show);
        Method deleteMethod = ResourceView.class.getDeclaredMethod("showDeleteConfirmation", Map.class, Runnable.class);
        deleteMethod.setAccessible(true);
        Runnable noRefreshNeeded = () -> {
            // In this test flow we assert delete failure through backend state retention,
            // so no post-delete table refresh callback is required.
        };
        Platform.runLater(() -> {
            try {
                deleteMethod.invoke(resourceView, stateRow, noRefreshNeeded);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        waitUntil(() -> exists(".dialog-pane"), Duration.ofSeconds(10), "Delete confirmation did not appear");
        clickOn("OK");
    }

    private void setFirstTextField(String value) {
        waitUntil(() -> visibleTextFieldCount() > 0, Duration.ofSeconds(10), "No visible text field found");
        interact(() -> {
            List<TextField> fields = lookup(".text-field").queryAllAs(TextField.class).stream().filter(TextField::isVisible).toList();
            TextField field = fields.get(0);
            field.clear();
            field.setText(value);
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

    private int visibleComboCount() {
        return interactRead(() -> lookup(".combo-box").queryAllAs(ComboBox.class).stream().filter(ComboBox::isVisible).toList().size());
    }

    private Map<String, Object> waitForEntityByName(String resource, String name, Duration timeout) throws Exception {
        waitUntil(() -> findEntityByName(resource, name).isPresent(), timeout, "Expected entity not found: " + resource + " -> " + name);
        return findEntityByName(resource, name).orElseThrow();
    }

    private Optional<Map<String, Object>> findEntityByName(String resource, String name) {
        try {
            for (int page = 1; page <= MAX_SEARCH_PAGES; page++) {
                List<Map<String, Object>> rows = apiClient.list(resource + "?page=" + page + "&pageSize=" + SEARCH_PAGE_SIZE);
                Optional<Map<String, Object>> match = rows.stream()
                        .filter(row -> name.equals(String.valueOf(row.get("name"))))
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

    private String backendApiBaseUrl() {
        return "http://localhost:" + port + "/api";
    }

    private void dismissDialogsIfOpen() {
        for (int i = 0; i < DIALOG_DISMISS_ATTEMPTS; i++) {
            if (exists(".dialog-pane")) {
                clickOn("OK");
            }
        }
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
