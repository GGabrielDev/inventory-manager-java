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

public class DesktopUi {
    private final Stage stage;
    private final ApiClient apiClient;

    public DesktopUi(Stage stage, ApiClient apiClient) {
        this.stage = stage;
        this.apiClient = apiClient;
    }

    public void showLogin() {
        TextField username = new TextField("admin");
        PasswordField password = new PasswordField();
        password.setText("admin");
        Label status = new Label();
        Button login = new Button("Login");

        login.setOnAction(event -> {
            try {
                apiClient.login(username.getText(), password.getText());
                showDashboard();
            } catch (Exception exception) {
                status.setText(exception.getMessage());
            }
        });

        VBox root = new VBox(12,
                new Label("Inventory Manager - JavaFX"),
                new Label("Username"), username,
                new Label("Password"), password,
                login,
                status
        );
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER_LEFT);
        stage.setScene(new Scene(root, 460, 320));
        stage.setTitle("Inventory Manager Desktop");
        stage.show();
    }

    private void showDashboard() throws Exception {
        TabPane tabs = new TabPane();
        tabs.getTabs().add(createResourceTab("Users", "users", "{\n  \"username\": \"new-user\",\n  \"password\": \"password\",\n  \"roleIds\": []\n}"));
        tabs.getTabs().add(createResourceTab("Roles", "roles", "{\n  \"name\": \"operator\",\n  \"description\": \"Operator role\",\n  \"permissionIds\": []\n}"));
        tabs.getTabs().add(createResourceTab("Permissions", "permissions", "{\n  \"name\": \"get_item\",\n  \"description\": \"Allows item reads\"\n}"));
        tabs.getTabs().add(createResourceTab("Departments", "departments", "{\n  \"name\": \"Main Department\"\n}"));
        tabs.getTabs().add(createResourceTab("Categories", "categories", "{\n  \"name\": \"Default Category\"\n}"));
        tabs.getTabs().add(createResourceTab("Items", "items", "{\n  \"name\": \"Sample Item\",\n  \"quantity\": 1,\n  \"unit\": \"UND\",\n  \"observations\": null,\n  \"characteristicsJson\": \"{}\",\n  \"categoryId\": null,\n  \"departmentId\": 1\n}"));
        tabs.getTabs().add(createResourceTab("States", "states", "{\n  \"name\": \"Sample State\"\n}"));
        tabs.getTabs().add(createResourceTab("Municipalities", "municipalities", "{\n  \"name\": \"Sample Municipality\",\n  \"stateId\": 1\n}"));
        tabs.getTabs().add(createResourceTab("Parishes", "parishes", "{\n  \"name\": \"Sample Parish\",\n  \"municipalityId\": 1\n}"));

        Label me = new Label(apiClient.me().toString());
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
