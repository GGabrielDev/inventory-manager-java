package com.inventorymanager.frontend.ui;

import com.inventorymanager.frontend.api.ApiClient;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Main UI Controller for the Inventory Manager 2.0.
 * Supports English and Spanish via ResourceBundle.
 */
public class DesktopUi {
    private final Stage stage;
    private ApiClient apiClient;
    private final ConfigManager configManager;
    private ResourceBundle bundle;
    
    private BorderPane mainLayout;
    private StackPane contentArea;
    private Map<String, Object> currentUser;
    private boolean isAdmin;
    private boolean globalContext = false;

    public DesktopUi(Stage stage, ApiClient apiClient, ConfigManager configManager) {
        this.stage = stage;
        this.apiClient = apiClient;
        this.configManager = configManager;
        loadBundle();
    }

    private void loadBundle() {
        this.bundle = ResourceBundle.getBundle("i18n/messages", new Locale(configManager.getLanguage()));
    }

    public void showLogin() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(40));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #f4f7f6;");

        Label title = new Label(bundle.getString("title.main"));
        title.setFont(Font.font("System", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#2c3e50"));

        VBox form = new VBox(10);
        form.setMaxWidth(300);
        
        TextField username = new TextField("admin");
        username.setPromptText(bundle.getString("login.username"));
        username.setPrefHeight(40);
        
        PasswordField password = new PasswordField();
        password.setText("password");
        password.setPromptText(bundle.getString("login.password"));
        password.setPrefHeight(40);

        Button loginBtn = new Button(bundle.getString("login.signin"));
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setPrefHeight(40);
        loginBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");

        Button settingsBtn = new Button(bundle.getString("login.settings"));
        settingsBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #7f8c8d;");

        loginBtn.setOnAction(event -> {
            try {
                apiClient.login(username.getText(), password.getText());
                showDashboard();
            } catch (Exception exception) {
                showErrorPopup(bundle.getString("login.status.fail"), bundle.getString("login.status.auth_error"), exception);
            }
        });

        settingsBtn.setOnAction(event -> showSettingsPopup());

        form.getChildren().addAll(new Label(bundle.getString("login.username")), username, 
                             new Label(bundle.getString("login.password")), password, loginBtn, settingsBtn);
        root.getChildren().addAll(title, form);

        stage.setScene(new Scene(root, 460, 480));
        stage.setTitle(bundle.getString("login.signin") + " - Inventory Manager");
        stage.show();
    }

    private void showDashboard() throws Exception {
        this.currentUser = apiClient.me();
        @SuppressWarnings("unchecked")
        List<String> rolesPayload = (List<String>) currentUser.getOrDefault("roles", List.of());
        Set<String> roles = rolesPayload.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        this.isAdmin = roles.contains("admin");

        mainLayout = new BorderPane();
        contentArea = new StackPane();
        contentArea.setPadding(new Insets(20));
        contentArea.setStyle("-fx-background-color: white;");

        VBox sidebar = new VBox(5);
        sidebar.setPadding(new Insets(10));
        sidebar.setPrefWidth(220);
        sidebar.setStyle("-fx-background-color: #2c3e50;");

        Label brand = new Label(bundle.getString("title.main"));
        brand.setTextFill(Color.WHITE);
        brand.setFont(Font.font("System", FontWeight.BOLD, 18));
        brand.setPadding(new Insets(10, 10, 20, 10));

        sidebar.getChildren().add(brand);
        sidebar.getChildren().add(createNavButton(bundle.getString("nav.dashboard"), this::showMainDashboardView));
        sidebar.getChildren().add(new Separator());
        sidebar.getChildren().add(createNavGroupLabel(bundle.getString("nav.inventory")));
        sidebar.getChildren().add(createNavButton(bundle.getString("nav.assets"), () -> showResourceView(bundle.getString("nav.assets"), "items")));
        sidebar.getChildren().add(createNavButton(bundle.getString("nav.bags"), () -> showResourceView(bundle.getString("nav.bags"), "bags")));
        sidebar.getChildren().add(new Separator());
        sidebar.getChildren().add(createNavGroupLabel(bundle.getString("nav.operations")));
        sidebar.getChildren().add(createNavButton(bundle.getString("nav.transfers"), () -> showResourceView(bundle.getString("nav.transfers"), "item-requests")));
        sidebar.getChildren().add(createNavButton(bundle.getString("nav.audit"), this::showBagAuditScanner));
        sidebar.getChildren().add(createNavButton(bundle.getString("nav.displacements"), () -> showResourceView(bundle.getString("nav.displacements"), "displacements")));
        
        if (isAdmin) {
            sidebar.getChildren().add(new Separator());
            sidebar.getChildren().add(createNavGroupLabel(bundle.getString("nav.admin")));
            sidebar.getChildren().add(createNavButton(bundle.getString("nav.audit_logs"), this::showGlobalAuditView));
            sidebar.getChildren().add(createNavButton(bundle.getString("nav.branches"), () -> showResourceView(bundle.getString("nav.branches"), "branches")));
            sidebar.getChildren().add(createNavButton(bundle.getString("nav.departments"), () -> showResourceView(bundle.getString("nav.departments"), "departments")));
            sidebar.getChildren().add(createNavButton(bundle.getString("nav.users"), () -> showResourceView(bundle.getString("nav.users"), "users")));
        }
        
        sidebar.getChildren().add(new Separator());
        sidebar.getChildren().add(createNavButton(bundle.getString("nav.settings"), this::showSettingsPopup));

        HBox header = new HBox(15);
        header.setPadding(new Insets(15, 20, 15, 20));
        header.setAlignment(Pos.CENTER_RIGHT);
        header.setStyle("-fx-background-color: white; -fx-border-color: #ecf0f1; -fx-border-width: 0 0 1 0;");
        
        if (isAdmin) {
            ToggleButton contextToggle = new ToggleButton(bundle.getString("header.global"));
            contextToggle.setSelected(globalContext);
            contextToggle.setOnAction(e -> {
                globalContext = contextToggle.isSelected();
                contextToggle.setText(globalContext ? bundle.getString("header.global") : bundle.getString("header.branch"));
                showMainDashboardView();
            });
            header.getChildren().add(contextToggle);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label userLabel = new Label(bundle.getString("header.user") + " " + currentUser.get("username"));
        userLabel.setStyle("-fx-font-weight: bold;");
        Button logoutBtn = new Button(bundle.getString("header.logout"));
        logoutBtn.setOnAction(e -> showLogin());
        
        header.getChildren().addAll(spacer, userLabel, logoutBtn);

        mainLayout.setLeft(sidebar);
        mainLayout.setTop(header);
        mainLayout.setCenter(contentArea);

        showMainDashboardView();

        stage.setScene(new Scene(mainLayout, 1366, 900));
    }

    private Button createNavButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(10, 15, 10, 15));
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #bdc3c7; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #bdc3c7; -fx-cursor: hand;"));
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private Label createNavGroupLabel(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.web("#7f8c8d"));
        label.setFont(Font.font("System", FontWeight.BOLD, 11));
        label.setPadding(new Insets(15, 10, 5, 10));
        return label;
    }

    private void showMainDashboardView() {
        VBox root = new VBox(20);
        String contextText = globalContext ? bundle.getString("dashboard.org_view") : bundle.getString("dashboard.branch_view");
        Label title = new Label(contextText);
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        
        HBox cards = new HBox(20);
        cards.getChildren().addAll(
            createStatCard(bundle.getString("dashboard.total_assets"), "...", "#3498db"),
            createStatCard(bundle.getString("dashboard.pending_reviews"), "...", "#e67e22"),
            createStatCard(bundle.getString("dashboard.active_displacements"), "...", "#e74c3c")
        );

        root.getChildren().addAll(title, cards);
        setView(root);
    }

    private VBox createStatCard(String title, String value, String color) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(20));
        card.setPrefWidth(250);
        card.setStyle("-fx-background-color: white; -fx-border-color: " + color + "; -fx-border-width: 0 0 0 5; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");
        Label t = new Label(title);
        t.setTextFill(Color.GRAY);
        Label v = new Label(value);
        v.setFont(Font.font("System", FontWeight.BOLD, 24));
        card.getChildren().addAll(t, v);
        return card;
    }

    private void showResourceView(String title, String resource) {
        VBox root = new VBox(15);
        HBox filterBar = new HBox(10);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(10));
        filterBar.setStyle("-fx-background-color: #f8f9fa; -fx-border-radius: 5;");
        
        ComboBox<IdName> stateFilter = new ComboBox<>();
        stateFilter.setPromptText("State...");
        ComboBox<IdName> branchFilter = new ComboBox<>();
        branchFilter.setPromptText("Branch...");
        Button applyFilter = new Button(bundle.getString("resource.apply"));
        Button clearFilter = new Button(bundle.getString("resource.clear"));
        
        filterBar.getChildren().addAll(new Label(bundle.getString("resource.filter")), stateFilter, branchFilter, applyFilter, clearFilter);
        
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label t = new Label(title);
        t.setFont(Font.font("System", FontWeight.BOLD, 20));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button refreshBtn = new Button(bundle.getString("resource.refresh"));
        Button addBtn = new Button(bundle.getString("resource.add"));
        addBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        header.getChildren().addAll(t, spacer, refreshBtn, addBtn);

        TableView<Map<String, Object>> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        applyFilter.setOnAction(e -> loadFilteredData(table, resource, stateFilter.getValue(), branchFilter.getValue()));
        clearFilter.setOnAction(e -> {
            stateFilter.setValue(null);
            branchFilter.setValue(null);
            loadFilteredData(table, resource, null, null);
        });
        refreshBtn.setOnAction(e -> loadFilteredData(table, resource, stateFilter.getValue(), branchFilter.getValue()));
        addBtn.setOnAction(e -> showCreateForm(title, resource));

        Platform.runLater(() -> {
            try {
                stateFilter.setItems(fetchIdNames("states"));
                branchFilter.setItems(fetchIdNames("branches"));
            } catch (Exception ignored) {}
        });

        root.getChildren().addAll(header, filterBar, table);
        setView(root);
        loadFilteredData(table, resource, null, null);
    }

    private void loadFilteredData(TableView<Map<String, Object>> table, String resource, IdName state, IdName branch) {
        try {
            String path = resource + "?page=1&pageSize=100";
            if (state != null) path += "&stateId=" + state.id;
            if (branch != null) path += "&branchId=" + branch.id;
            List<Map<String, Object>> data = apiClient.list(path);
            updateTableColumns(table, data);
        } catch (Exception e) {
            showErrorPopup("Fetch Error", "Could not load data", e);
        }
    }

    private void updateTableColumns(TableView<Map<String, Object>> table, List<Map<String, Object>> data) {
        table.getColumns().clear();
        if (!data.isEmpty()) {
            Map<String, Object> first = data.get(0);
            for (String key : first.keySet()) {
                TableColumn<Map<String, Object>, String> col = new TableColumn<>(key.toUpperCase());
                col.setCellValueFactory(cellData -> {
                    Object val = cellData.getValue().get(key);
                    if (val instanceof Map) val = ((Map<?, ?>) val).get("name");
                    return new javafx.beans.property.SimpleStringProperty(val == null ? "" : val.toString());
                });
                table.getColumns().add(col);
            }
            table.setItems(FXCollections.observableArrayList(data));
        }
    }

    private void showGlobalAuditView() {
        VBox root = new VBox(15);
        Label title = new Label(bundle.getString("audit.title"));
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        
        HBox controls = new HBox(10);
        ComboBox<String> entityType = new ComboBox<>(FXCollections.observableArrayList(
            "item", "branch", "user", "bag", "item_request", "displacement"
        ));
        entityType.setPromptText(bundle.getString("audit.type"));
        TextField entityId = new TextField();
        entityId.setPromptText(bundle.getString("audit.id"));
        Button fetchBtn = new Button(bundle.getString("audit.fetch"));
        
        controls.getChildren().addAll(new Label(bundle.getString("audit.query")), entityType, entityId, fetchBtn);
        TableView<Map<String, Object>> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        fetchBtn.setOnAction(e -> {
            try {
                String path = "audit-logs/" + entityType.getValue() + "/" + entityId.getText();
                Map<String, Object> response = apiClient.get(path);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
                updateTableColumns(table, data);
            } catch (Exception ex) {
                showErrorPopup(bundle.getString("audit.error"), "Failed to fetch logs", ex);
            }
        });

        root.getChildren().addAll(title, controls, table);
        setView(root);
    }

    private void showCreateForm(String title, String resource) {
        switch (resource) {
            case "items" -> showItemCreateForm();
            case "bags" -> showBagCreateForm();
            case "displacements" -> showDisplacementCreateForm();
            case "branches" -> showBranchCreateForm();
            case "departments" -> showDepartmentCreateForm();
            case "users" -> showUserCreateForm();
            default -> showPlaceholder("Form for " + title);
        }
    }

    private void showItemCreateForm() {
        VBox root = new VBox(20);
        Label t = new Label(bundle.getString("resource.add"));
        t.setFont(Font.font("System", FontWeight.BOLD, 18));
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        TextField nameField = new TextField();
        TextField qtyField = new TextField("1");
        ComboBox<String> unitCombo = new ComboBox<>(FXCollections.observableArrayList("UND", "KG", "L", "M"));
        unitCombo.setValue("UND");
        ComboBox<IdName> branchCombo = new ComboBox<>();
        ComboBox<IdName> deptCombo = new ComboBox<>();
        grid.addRow(0, new Label(bundle.getString("form.name")), nameField);
        grid.addRow(1, new Label(bundle.getString("form.qty")), qtyField);
        grid.addRow(2, new Label(bundle.getString("form.unit")), unitCombo);
        grid.addRow(3, new Label(bundle.getString("form.branch")), branchCombo);
        grid.addRow(4, new Label("Dept:"), deptCombo); // Shared key would be better
        Platform.runLater(() -> {
            try {
                branchCombo.setItems(fetchIdNames("branches"));
                deptCombo.setItems(fetchIdNames("departments"));
            } catch (Exception ignored) {}
        });
        Button saveBtn = new Button(bundle.getString("form.create"));
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            try {
                apiClient.create("items", Map.of(
                    "name", nameField.getText(), "quantity", Integer.parseInt(qtyField.getText()),
                    "unit", unitCombo.getValue(), "branchId", branchCombo.getValue().id,
                    "departmentId", deptCombo.getValue().id
                ));
                showResourceView(bundle.getString("nav.assets"), "items");
            } catch (Exception ex) { showErrorPopup("Save Error", "Failed", ex); }
        });
        root.getChildren().addAll(t, grid, saveBtn);
        setView(root);
    }

    private void showBranchCreateForm() {
        VBox root = new VBox(20);
        Label t = new Label(bundle.getString("form.register") + " Branch");
        t.setFont(Font.font("System", FontWeight.BOLD, 18));
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        TextField nameField = new TextField();
        TextArea addrArea = new TextArea(); addrArea.setPrefRowCount(2);
        ComboBox<IdName> stateCombo = new ComboBox<>();
        ComboBox<IdName> muniCombo = new ComboBox<>();
        ComboBox<IdName> parishCombo = new ComboBox<>();
        grid.addRow(0, new Label(bundle.getString("form.name")), nameField);
        grid.addRow(1, new Label("Address:"), addrArea);
        grid.addRow(2, new Label("State:"), stateCombo);
        grid.addRow(3, new Label("Municipality:"), muniCombo);
        grid.addRow(4, new Label("Parish:"), parishCombo);
        Platform.runLater(() -> {
            try {
                stateCombo.setItems(fetchIdNames("states"));
                stateCombo.setOnAction(e -> {
                   try { muniCombo.setItems(fetchIdNames("municipalities")); } catch (Exception ignored) {}
                });
                muniCombo.setOnAction(e -> {
                   try { parishCombo.setItems(fetchIdNames("parishes")); } catch (Exception ignored) {}
                });
            } catch (Exception ignored) {}
        });
        Button saveBtn = new Button(bundle.getString("form.register"));
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            try {
                apiClient.create("branches", Map.of(
                    "name", nameField.getText(), "address", addrArea.getText(),
                    "stateId", stateCombo.getValue().id, "municipalityId", muniCombo.getValue().id,
                    "parishId", parishCombo.getValue().id
                ));
                showResourceView(bundle.getString("nav.branches"), "branches");
            } catch (Exception ex) { showErrorPopup("Save Error", "Failed", ex); }
        });
        root.getChildren().addAll(t, grid, saveBtn);
        setView(root);
    }

    private void showDepartmentCreateForm() {
        VBox root = new VBox(20);
        Label t = new Label(bundle.getString("form.create") + " Department");
        t.setFont(Font.font("System", FontWeight.BOLD, 18));
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        TextField nameField = new TextField();
        ComboBox<IdName> branchCombo = new ComboBox<>();
        grid.addRow(0, new Label(bundle.getString("form.dept")), nameField);
        grid.addRow(1, new Label(bundle.getString("form.branch")), branchCombo);
        Platform.runLater(() -> {
            try { branchCombo.setItems(fetchIdNames("branches")); } catch (Exception ignored) {}
        });
        Button saveBtn = new Button(bundle.getString("form.create"));
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            try {
                apiClient.create("departments", Map.of("name", nameField.getText(), "branchId", branchCombo.getValue().id));
                showResourceView(bundle.getString("nav.departments"), "departments");
            } catch (Exception ex) { showErrorPopup("Save Error", "Failed", ex); }
        });
        root.getChildren().addAll(t, grid, saveBtn);
        setView(root);
    }

    private void showUserCreateForm() {
        VBox root = new VBox(20);
        Label t = new Label(bundle.getString("form.register") + " User");
        t.setFont(Font.font("System", FontWeight.BOLD, 18));
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        TextField userField = new TextField();
        PasswordField passField = new PasswordField();
        ComboBox<IdName> branchCombo = new ComboBox<>();
        grid.addRow(0, new Label(bundle.getString("form.user")), userField);
        grid.addRow(1, new Label(bundle.getString("form.pass")), passField);
        grid.addRow(2, new Label(bundle.getString("form.branch")), branchCombo);
        Platform.runLater(() -> {
            try { branchCombo.setItems(fetchIdNames("branches")); } catch (Exception ignored) {}
        });
        Button saveBtn = new Button(bundle.getString("form.register"));
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            try {
                apiClient.create("users", Map.of(
                    "username", userField.getText(), "password", passField.getText(),
                    "branchId", branchCombo.getValue().id, "roleIds", List.of()
                ));
                showResourceView(bundle.getString("nav.users"), "users");
            } catch (Exception ex) { showErrorPopup("Save Error", "Failed", ex); }
        });
        root.getChildren().addAll(t, grid, saveBtn);
        setView(root);
    }

    private void showBagCreateForm() {
        VBox root = new VBox(20);
        Label t = new Label(bundle.getString("form.create") + " Bag");
        t.setFont(Font.font("System", FontWeight.BOLD, 18));
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        TextField nameField = new TextField();
        TextField barcodeField = new TextField();
        ComboBox<IdName> branchCombo = new ComboBox<>();
        ComboBox<IdName> deptCombo = new ComboBox<>();
        grid.addRow(0, new Label(bundle.getString("form.name")), nameField);
        grid.addRow(1, new Label(bundle.getString("form.barcode")), barcodeField);
        grid.addRow(2, new Label(bundle.getString("form.branch")), branchCombo);
        grid.addRow(3, new Label("Dept:"), deptCombo);
        Platform.runLater(() -> {
            try {
                branchCombo.setItems(fetchIdNames("branches"));
                deptCombo.setItems(fetchIdNames("departments"));
            } catch (Exception ignored) {}
        });
        Button saveBtn = new Button(bundle.getString("form.create"));
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            try {
                apiClient.create("bags", Map.of(
                    "name", nameField.getText(), "barcode", barcodeField.getText(),
                    "branchId", branchCombo.getValue().id, "assignedDepartmentId", deptCombo.getValue().id,
                    "expectedItems", List.of()
                ));
                showResourceView(bundle.getString("nav.bags"), "bags");
            } catch (Exception ex) { showErrorPopup("Save Error", "Failed", ex); }
        });
        root.getChildren().addAll(t, grid, saveBtn);
        setView(root);
    }

    private void showDisplacementCreateForm() {
        VBox root = new VBox(20);
        Label t = new Label(bundle.getString("nav.displacements"));
        t.setFont(Font.font("System", FontWeight.BOLD, 18));
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        ComboBox<IdName> itemCombo = new ComboBox<>();
        TextField borrowerField = new TextField();
        TextArea reasonArea = new TextArea(); reasonArea.setPrefRowCount(3);
        DatePicker datePicker = new DatePicker(java.time.LocalDate.now().plusDays(7));
        grid.addRow(0, new Label("Item:"), itemCombo);
        grid.addRow(1, new Label(bundle.getString("form.borrower")), borrowerField);
        grid.addRow(2, new Label(bundle.getString("form.reason")), reasonArea);
        grid.addRow(3, new Label(bundle.getString("form.return")), datePicker);
        Platform.runLater(() -> {
            try { itemCombo.setItems(fetchIdNames("items")); } catch (Exception ignored) {}
        });
        Button saveBtn = new Button(bundle.getString("form.register"));
        saveBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            try {
                apiClient.create("displacements", Map.of(
                    "itemId", itemCombo.getValue().id, "borrowerName", borrowerField.getText(),
                    "reason", reasonArea.getText(),
                    "expectedReturnDate", datePicker.getValue().atStartOfDay().atOffset(java.time.ZoneOffset.UTC).toString()
                ));
                showResourceView(bundle.getString("nav.displacements"), "displacements");
            } catch (Exception ex) { showErrorPopup("Save Error", "Failed", ex); }
        });
        root.getChildren().addAll(t, grid, saveBtn);
        setView(root);
    }

    private ObservableList<IdName> fetchIdNames(String resource) throws Exception {
        return FXCollections.observableArrayList(
            apiClient.list(resource).stream()
                .map(m -> new IdName(((Number) m.get("id")).longValue(), m.get("name").toString()))
                .collect(Collectors.toList())
        );
    }

    private static class IdName {
        final Long id;
        final String name;
        IdName(Long id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }

    private void setView(Node node) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(node);
    }

    private void showPlaceholder(String name) {
        VBox root = new VBox(10);
        root.setAlignment(Pos.CENTER);
        root.getChildren().addAll(new Label("🚧 " + name), new Label("Development in progress."));
        setView(root);
    }

    private void showBagAuditScanner() {
        VBox root = new VBox(15);
        Label title = new Label(bundle.getString("audit.scanner.title"));
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        TextField barcodeField = new TextField();
        barcodeField.setPromptText(bundle.getString("audit.scanner.scan_prompt"));
        Button scanBtn = new Button(bundle.getString("audit.scanner.scan"));
        searchBox.getChildren().addAll(barcodeField, scanBtn);
        VBox resultArea = new VBox(10);
        scanBtn.setOnAction(e -> {
            try {
                Map<String, Object> bag = apiClient.get("bags/barcode/" + barcodeField.getText());
                resultArea.getChildren().clear();
                resultArea.getChildren().add(new Label("Bag: " + bag.get("name")));
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>) bag.get("expectedItems");
                if (items == null || items.isEmpty()) {
                    resultArea.getChildren().add(new Label(bundle.getString("audit.scanner.empty")));
                } else {
                    TableView<Map<String, Object>> table = new TableView<>();
                    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
                    TableColumn<Map<String, Object>, String> itemCol = new TableColumn<>(bundle.getString("audit.scanner.expected"));
                    itemCol.setCellValueFactory(cellData -> {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> itemData = (Map<String, Object>) cellData.getValue().get("item");
                        return new javafx.beans.property.SimpleStringProperty(itemData != null ? itemData.get("name").toString() : "");
                    });
                    table.getColumns().add(itemCol);
                    table.setItems(FXCollections.observableArrayList(items));
                    resultArea.getChildren().add(table);
                    Button displaceBtn = new Button(bundle.getString("audit.scanner.missing"));
                    displaceBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                    displaceBtn.setOnAction(evt -> showDisplacementCreateForm());
                    resultArea.getChildren().add(displaceBtn);
                }
            } catch (Exception ex) { showErrorPopup(bundle.getString("audit.error"), "Failed to fetch bag", ex); }
        });
        root.getChildren().addAll(title, searchBox, resultArea);
        setView(root);
    }

    private void showSettingsPopup() {
        Stage settingsStage = new Stage();
        settingsStage.initOwner(stage);
        settingsStage.setTitle(bundle.getString("settings.title"));
        
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        
        TextField urlField = new TextField(apiClient.getBaseUrl());
        
        ComboBox<String> langCombo = new ComboBox<>(FXCollections.observableArrayList("en", "es"));
        langCombo.setValue(configManager.getLanguage());
        
        Button save = new Button(bundle.getString("settings.save"));
        save.setMaxWidth(Double.MAX_VALUE);
        
        save.setOnAction(e -> {
            configManager.setApiUrl(urlField.getText());
            configManager.setLanguage(langCombo.getValue());
            this.apiClient = new ApiClient(urlField.getText());
            loadBundle(); // Reload bundle
            settingsStage.close();
            showLogin(); // Refresh UI
        });
        
        layout.getChildren().addAll(
            new Label(bundle.getString("settings.url")), urlField,
            new Label(bundle.getString("settings.lang")), langCombo,
            save
        );
        
        settingsStage.setScene(new Scene(layout));
        settingsStage.show();
    }

    private void showErrorPopup(String title, String message, Exception ex) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(message);
            
            // Format detailed error report
            StringBuilder sb = new StringBuilder();
            sb.append("--- ERROR REPORT ---\n");
            sb.append("Timestamp: ").append(java.time.LocalDateTime.now()).append("\n");
            sb.append("Title: ").append(title).append("\n");
            sb.append("Message: ").append(message).append("\n");
            sb.append("Exception: ").append(ex.toString()).append("\n\n");
            sb.append("--- STACK TRACE ---\n");
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            ex.printStackTrace(pw);
            sb.append(sw.toString());
            
            String detailedReport = sb.toString();

            TextArea textArea = new TextArea(detailedReport);
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setPrefHeight(200);
            textArea.setPrefWidth(500);

            Button copyBtn = new Button("📋 Copy error to clipboard");
            copyBtn.setOnAction(e -> {
                javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(detailedReport);
                clipboard.setContent(content);
                copyBtn.setText("✅ Copied!");
            });

            VBox content = new VBox(10, new Label("Error details:"), textArea, copyBtn, new Label("Please check connection or contact programmer."));
            alert.getDialogPane().setContent(content);
            alert.showAndWait();
        });
    }
}
