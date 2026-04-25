package com.inventorymanager.frontend.ui;

import com.inventorymanager.frontend.api.ApiClient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DesktopUi {
    private final Stage stage;
    private ApiClient apiClient;
    private final ConfigManager configManager;

    public DesktopUi(Stage stage, ApiClient apiClient, ConfigManager configManager) {
        this.stage = stage;
        this.apiClient = apiClient;
        this.configManager = configManager;
    }

    public void showLogin() {
        TextField username = new TextField("admin");
        PasswordField password = new PasswordField();
        password.setText("admin");
        Label status = new Label();
        Button login = new Button("Login");
        Button settings = new Button("Connection Settings");

        login.setOnAction(event -> {
            try {
                apiClient.login(username.getText(), password.getText());
                showDashboard();
            } catch (Exception exception) {
                showErrorPopup("Login Failed", 
                    "Could not log in to the server at " + apiClient.getBaseUrl(), 
                    exception);
            }
        });

        settings.setOnAction(event -> showSettingsPopup());

        VBox root = new VBox(12,
                new Label("Inventory Manager - JavaFX"),
                new Label("Connected to: " + apiClient.getBaseUrl()),
                new Label("Username"), username,
                new Label("Password"), password,
                new HBox(8, login, settings),
                status
        );
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER_LEFT);
        stage.setScene(new Scene(root, 460, 360));
        stage.setTitle("Inventory Manager Desktop");
        stage.show();
    }

    private void showSettingsPopup() {
        Stage settingsStage = new Stage();
        settingsStage.initOwner(stage);
        settingsStage.setTitle("Connection Settings");

        TextField urlField = new TextField(apiClient.getBaseUrl());
        urlField.setPrefWidth(300);
        Button save = new Button("Save and Restart");

        save.setOnAction(e -> {
            String newUrl = urlField.getText();
            configManager.setApiUrl(newUrl);
            this.apiClient = new ApiClient(newUrl);
            settingsStage.close();
            showLogin(); // Refresh login screen
        });

        VBox layout = new VBox(10, 
            new Label("Backend API URL:"), 
            urlField, 
            new Label("Note: Changes will apply immediately."),
            save
        );
        layout.setPadding(new Insets(20));
        settingsStage.setScene(new Scene(layout));
        settingsStage.show();
    }

    private void showErrorPopup(String title, String message, Exception ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(message);
        
        VBox content = new VBox(10);
        content.getChildren().add(new Label("Error details: " + ex.toString()));
        content.getChildren().add(new Separator());
        content.getChildren().add(new Label("Please check your connection or contact the programmer."));
        
        Button changeAddress = new Button("Change Connection Address");
        changeAddress.setOnAction(e -> {
            alert.close();
            showSettingsPopup();
        });
        
        content.getChildren().add(changeAddress);
        
        alert.getDialogPane().setContent(content);
        alert.showAndWait();
    }

    private void showDashboard() throws Exception {
        Map<String, Object> mePayload = apiClient.me();
        Set<String> roles = ((List<?>) mePayload.getOrDefault("roles", List.of())).stream()
                .map(Object::toString)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        boolean isAdmin = roles.contains("admin");

        TabPane tabs = new TabPane();
        tabs.getTabs().add(createResourceTab("Users", "users", "{\n  \"username\": \"new-user\",\n  \"password\": \"password\",\n  \"roleIds\": []\n}"));
        tabs.getTabs().add(createResourceTab("Roles", "roles", "{\n  \"name\": \"operator\",\n  \"description\": \"Operator role\",\n  \"permissionIds\": []\n}"));
        tabs.getTabs().add(createResourceTab("Permissions", "permissions", "{\n  \"name\": \"get_item\",\n  \"description\": \"Allows item reads\"\n}"));
        tabs.getTabs().add(createResourceTab("Departments", "departments", "{\n  \"name\": \"Main Department\"\n}"));
        tabs.getTabs().add(createResourceTab("Categories", "categories", "{\n  \"name\": \"Default Category\"\n}"));
        if (isAdmin) {
            tabs.getTabs().add(createResourceTab("Items", "items", "{\n  \"name\": \"Sample Item\",\n  \"quantity\": 1,\n  \"unit\": \"UND\",\n  \"observations\": null,\n  \"characteristicsJson\": \"{}\",\n  \"categoryId\": null,\n  \"departmentId\": 1\n}"));
        }
        tabs.getTabs().add(createItemRequestTab(isAdmin));
        tabs.getTabs().add(createResourceTab("States", "states", "{\n  \"name\": \"Sample State\"\n}"));
        tabs.getTabs().add(createResourceTab("Municipalities", "municipalities", "{\n  \"name\": \"Sample Municipality\",\n  \"stateId\": 1\n}"));
        tabs.getTabs().add(createResourceTab("Parishes", "parishes", "{\n  \"name\": \"Sample Parish\",\n  \"municipalityId\": 1\n}"));

        Label me = new Label(mePayload.toString());
        BorderPane root = new BorderPane();
        root.setTop(new VBox(8, new Label("Authenticated user"), me));
        root.setCenter(tabs);
        root.setPadding(new Insets(12));
        stage.setScene(new Scene(root, 1200, 780));
    }

    private Tab createResourceTab(String title, String resource, String templateJson) {
        TextArea payloadArea = new TextArea(templateJson);
        payloadArea.setPrefRowCount(12);
        TextArea outputArea = new TextArea();
        outputArea.setPrefRowCount(20);
        TextField idField = new TextField();
        idField.setPromptText("id");

        Button listBtn = new Button("List");
        Button createBtn = new Button("Create");
        Button updateBtn = new Button("Update");
        Button deleteBtn = new Button("Delete");
        Button changelogBtn = new Button("Changelog");

        listBtn.setOnAction(e -> execute(outputArea, () -> apiClient.list(resource).toString()));
        createBtn.setOnAction(e -> execute(outputArea, () -> apiClient.create(resource, JsonUtil.map(payloadArea.getText())).toString()));
        updateBtn.setOnAction(e -> execute(outputArea, () -> apiClient.update(resource, Long.parseLong(idField.getText()), JsonUtil.map(payloadArea.getText())).toString()));
        deleteBtn.setOnAction(e -> execute(outputArea, () -> {
            apiClient.delete(resource, Long.parseLong(idField.getText()));
            return "Deleted " + resource + " #" + idField.getText();
        }));
        changelogBtn.setOnAction(e -> execute(outputArea, () -> {
            String entity = switch (resource) {
                case "users" -> "user";
                case "roles" -> "role";
                case "permissions" -> "permission";
                case "departments" -> "department";
                case "categories" -> "category";
                case "items" -> "item";
                case "states" -> "state";
                case "municipalities" -> "municipality";
                case "parishes" -> "parish";
                default -> resource;
            };
            List<Map<String, Object>> changes = apiClient.list("changelogs/" + entity + "/" + Long.parseLong(idField.getText()));
            return changes.toString();
        }));

        HBox actions = new HBox(8, idField, listBtn, createBtn, updateBtn, deleteBtn, changelogBtn);
        VBox body = new VBox(8, new Label("Payload JSON"), payloadArea, actions, new Label("Output"), outputArea);
        body.setPadding(new Insets(10));
        Tab tab = new Tab(title, body);
        tab.setClosable(false);
        return tab;
    }

    private Tab createItemRequestTab(boolean isAdmin) {
        String requestTemplate = """
                {
                  "requestType": "INBOUND",
                  "title": "New incoming stock",
                  "justification": "Supplier shipment arrived",
                  "entries": [
                    {
                      "itemId": null,
                      "requestedItemName": "New Item Name",
                      "requestedQuantity": 10,
                      "requestedUnit": "UND",
                      "requestedCategoryId": 1,
                      "sourceDepartmentId": null,
                      "targetDepartmentId": 1,
                      "observations": "Batch 2026-Q2",
                      "characteristicsJson": "{}"
                    }
                  ]
                }
                """;

        TextArea payloadArea = new TextArea(requestTemplate);
        payloadArea.setPrefRowCount(14);
        TextArea outputArea = new TextArea();
        outputArea.setPrefRowCount(20);
        TextField idField = new TextField();
        idField.setPromptText("request id");

        Button listBtn = new Button("List");
        Button createBtn = new Button("Create");
        Button updateBtn = new Button("Update");
        Button submitBtn = new Button("Submit");
        Button reviewBtn = new Button("Review");
        Button executeBtn = new Button("Execute");

        listBtn.setOnAction(e -> execute(outputArea, () -> apiClient.list("item-requests").toString()));
        createBtn.setOnAction(e -> execute(outputArea, () -> apiClient.create("item-requests", JsonUtil.map(payloadArea.getText())).toString()));
        updateBtn.setOnAction(e -> execute(outputArea, () -> apiClient.update("item-requests", Long.parseLong(idField.getText()), JsonUtil.map(payloadArea.getText())).toString()));
        submitBtn.setOnAction(e -> execute(outputArea, () -> apiClient.create("item-requests/" + Long.parseLong(idField.getText()) + "/submit", Map.of()).toString()));
        reviewBtn.setOnAction(e -> execute(outputArea, () -> apiClient.create(
                "item-requests/" + Long.parseLong(idField.getText()) + "/review",
                Map.of("decision", "approve", "comment", "Reviewed from desktop")
        ).toString()));
        executeBtn.setOnAction(e -> execute(outputArea, () -> apiClient.create(
                "item-requests/" + Long.parseLong(idField.getText()) + "/execute",
                Map.of()
        ).toString()));

        reviewBtn.setDisable(!isAdmin);
        executeBtn.setDisable(!isAdmin);

        HBox actions = new HBox(8, idField, listBtn, createBtn, updateBtn, submitBtn, reviewBtn, executeBtn);
        VBox body = new VBox(
                8,
                new Label("Item request payload (operators submit; admins review/execute)"),
                payloadArea,
                actions,
                new Label("Output"),
                outputArea
        );
        body.setPadding(new Insets(10));
        Tab tab = new Tab("Item Requests", body);
        tab.setClosable(false);
        return tab;
    }

    private void execute(TextArea output, ThrowingSupplier action) {
        try {
            output.setText(action.get());
        } catch (Exception exception) {
            output.setText(exception.getMessage());
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier {
        String get() throws Exception;
    }
}
