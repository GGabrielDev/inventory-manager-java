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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DesktopUi {
    private final Stage stage;
    private ApiClient apiClient;
    private final ConfigManager configManager;
    
    private BorderPane mainLayout;
    private StackPane contentArea;
    private Map<String, Object> currentUser;
    private boolean isAdmin;

    public DesktopUi(Stage stage, ApiClient apiClient, ConfigManager configManager) {
        this.stage = stage;
        this.apiClient = apiClient;
        this.configManager = configManager;
    }

    public void showLogin() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(40));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #f4f7f6;");

        Label title = new Label("INVENTORY MANAGER");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#2c3e50"));

        VBox form = new VBox(10);
        form.setMaxWidth(300);
        
        TextField username = new TextField("admin");
        username.setPromptText("Username");
        username.setPrefHeight(40);
        
        PasswordField password = new PasswordField();
        password.setText("password");
        password.setPromptText("Password");
        password.setPrefHeight(40);

        Button loginBtn = new Button("Sign In");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setPrefHeight(40);
        loginBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");

        Button settingsBtn = new Button("Connection Settings");
        settingsBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #7f8c8d;");

        loginBtn.setOnAction(event -> {
            try {
                apiClient.login(username.getText(), password.getText());
                showDashboard();
            } catch (Exception exception) {
                showErrorPopup("Login Failed", "Authentication error", exception);
            }
        });

        settingsBtn.setOnAction(event -> showSettingsPopup());

        form.getChildren().addAll(new Label("Username"), username, new Label("Password"), password, loginBtn, settingsBtn);
        root.getChildren().addAll(title, form);

        stage.setScene(new Scene(root, 460, 480));
        stage.setTitle("Login - Inventory Manager");
        stage.show();
    }

    private void showDashboard() throws Exception {
        this.currentUser = apiClient.me();
        Set<String> roles = ((List<?>) currentUser.getOrDefault("roles", List.of())).stream()
                .map(Object::toString)
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

        Label brand = new Label("INV MANAGER");
        brand.setTextFill(Color.WHITE);
        brand.setFont(Font.font("System", FontWeight.BOLD, 18));
        brand.setPadding(new Insets(10, 10, 20, 10));

        sidebar.getChildren().add(brand);
        sidebar.getChildren().add(createNavButton("📊 Dashboard", this::showMainDashboardView));
        sidebar.getChildren().add(new Separator());
        sidebar.getChildren().add(createNavGroupLabel("INVENTORY"));
        sidebar.getChildren().add(createNavButton("📦 Assets (Items)", () -> showResourceView("Items", "items")));
        sidebar.getChildren().add(createNavButton("💼 Bags (Kits)", () -> showResourceView("Bags", "bags")));
        sidebar.getChildren().add(new Separator());
        sidebar.getChildren().add(createNavGroupLabel("OPERATIONS"));
        sidebar.getChildren().add(createNavButton("🔄 Transfers", () -> showResourceView("Transfers", "item-requests")));
        sidebar.getChildren().add(createNavButton("🔍 Bag Audit", this::showBagAuditScanner));
        sidebar.getChildren().add(createNavButton("📉 Displacements", () -> showResourceView("Displacements", "displacements")));
        sidebar.getChildren().add(new Separator());
        sidebar.getChildren().add(createNavGroupLabel("SYSTEM"));
        sidebar.getChildren().add(createNavButton("🏢 Branches", () -> showResourceView("Branches", "branches")));
        sidebar.getChildren().add(createNavButton("🏢 Departments", () -> showResourceView("Departments", "departments")));
        sidebar.getChildren().add(createNavButton("👥 Users", () -> showResourceView("Users", "users")));
        sidebar.getChildren().add(createNavButton("⚙️ Settings", this::showSettingsPopup));

        HBox header = new HBox(15);
        header.setPadding(new Insets(15, 20, 15, 20));
        header.setAlignment(Pos.CENTER_RIGHT);
        header.setStyle("-fx-background-color: white; -fx-border-color: #ecf0f1; -fx-border-width: 0 0 1 0;");
        
        Label userLabel = new Label("Logged in as: " + currentUser.get("username"));
        userLabel.setStyle("-fx-font-weight: bold;");
        
        Button logoutBtn = new Button("Logout");
        logoutBtn.setOnAction(e -> showLogin());
        
        header.getChildren().addAll(userLabel, logoutBtn);

        mainLayout.setLeft(sidebar);
        mainLayout.setTop(header);
        mainLayout.setCenter(contentArea);

        showMainDashboardView();

        stage.setScene(new Scene(mainLayout, 1280, 850));
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
        Label title = new Label("Overview");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        
        HBox cards = new HBox(20);
        cards.getChildren().addAll(
            createStatCard("Total Assets", "...", "#3498db"),
            createStatCard("Pending Requests", "...", "#e67e22"),
            createStatCard("Active Displacements", "...", "#e74c3c")
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
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label t = new Label(title);
        t.setFont(Font.font("System", FontWeight.BOLD, 20));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button refreshBtn = new Button("🔄 Refresh");
        Button addBtn = new Button("➕ Add New");
        addBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        
        header.getChildren().addAll(t, spacer, refreshBtn, addBtn);

        TableView<Map<String, Object>> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        refreshBtn.setOnAction(e -> loadTableData(table, resource));
        addBtn.setOnAction(e -> showCreateForm(title, resource));

        root.getChildren().addAll(header, table);
        setView(root);
        loadTableData(table, resource);
    }

    private void showCreateForm(String title, String resource) {
        switch (resource) {
            case "items" -> showItemCreateForm();
            case "bags" -> showBagCreateForm();
            case "displacements" -> showDisplacementCreateForm();
            case "branches" -> showBranchCreateForm();
            case "departments" -> showDepartmentCreateForm();
            case "users" -> showUserCreateForm();
            default -> showPlaceholder("Structured form for " + title);
        }
    }

    private void showItemCreateForm() {
        VBox root = new VBox(20);
        Label t = new Label("Add New Asset (Item)");
        t.setFont(Font.font("System", FontWeight.BOLD, 18));
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);

        TextField nameField = new TextField();
        TextField qtyField = new TextField("1");
        ComboBox<String> unitCombo = new ComboBox<>(FXCollections.observableArrayList("UND", "KG", "L", "M"));
        unitCombo.setValue("UND");
        ComboBox<IdName> branchCombo = new ComboBox<>();
        ComboBox<IdName> deptCombo = new ComboBox<>();

        grid.addRow(0, new Label("Item Name:"), nameField);
        grid.addRow(1, new Label("Initial Quantity:"), qtyField);
        grid.addRow(2, new Label("Unit:"), unitCombo);
        grid.addRow(3, new Label("Target Branch:"), branchCombo);
        grid.addRow(4, new Label("Target Department:"), deptCombo);

        Platform.runLater(() -> {
            try {
                branchCombo.setItems(fetchIdNames("branches"));
                deptCombo.setItems(fetchIdNames("departments"));
            } catch (Exception e) {}
        });

        Button saveBtn = new Button("Create Item");
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            try {
                apiClient.create("items", Map.of(
                    "name", nameField.getText(),
                    "quantity", Integer.parseInt(qtyField.getText()),
                    "unit", unitCombo.getValue(),
                    "branchId", branchCombo.getValue().id,
                    "departmentId", deptCombo.getValue().id
                ));
                showResourceView("Items", "items");
            } catch (Exception ex) { showErrorPopup("Save Error", "Item creation failed", ex); }
        });
        root.getChildren().addAll(t, grid, saveBtn);
        setView(root);
    }

    private void showBranchCreateForm() {
        VBox root = new VBox(20);
        Label t = new Label("Register New Branch Office");
        t.setFont(Font.font("System", FontWeight.BOLD, 18));
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);

        TextField nameField = new TextField();
        TextArea addrArea = new TextArea(); addrArea.setPrefRowCount(2);
        ComboBox<IdName> stateCombo = new ComboBox<>();
        ComboBox<IdName> muniCombo = new ComboBox<>();
        ComboBox<IdName> parishCombo = new ComboBox<>();

        grid.addRow(0, new Label("Branch Name:"), nameField);
        grid.addRow(1, new Label("Full Address:"), addrArea);
        grid.addRow(2, new Label("State:"), stateCombo);
        grid.addRow(3, new Label("Municipality:"), muniCombo);
        grid.addRow(4, new Label("Parish:"), parishCombo);

        Platform.runLater(() -> {
            try {
                stateCombo.setItems(fetchIdNames("states"));
                stateCombo.setOnAction(e -> {
                   try { muniCombo.setItems(fetchIdNames("municipalities")); } catch (Exception ex) {}
                });
                muniCombo.setOnAction(e -> {
                   try { parishCombo.setItems(fetchIdNames("parishes")); } catch (Exception ex) {}
                });
            } catch (Exception e) {}
        });

        Button saveBtn = new Button("Register Branch");
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            try {
                apiClient.create("branches", Map.of(
                    "name", nameField.getText(),
                    "address", addrArea.getText(),
                    "stateId", stateCombo.getValue().id,
                    "municipalityId", muniCombo.getValue().id,
                    "parishId", parishCombo.getValue().id
                ));
                showResourceView("Branches", "branches");
            } catch (Exception ex) { showErrorPopup("Save Error", "Branch registration failed", ex); }
        });
        root.getChildren().addAll(t, grid, saveBtn);
        setView(root);
    }

    private void showDepartmentCreateForm() {
        VBox root = new VBox(20);
        Label t = new Label("Create New Department");
        t.setFont(Font.font("System", FontWeight.BOLD, 18));
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);

        TextField nameField = new TextField();
        ComboBox<IdName> branchCombo = new ComboBox<>();

        grid.addRow(0, new Label("Dept Name:"), nameField);
        grid.addRow(1, new Label("Branch:"), branchCombo);

        Platform.runLater(() -> {
            try { branchCombo.setItems(fetchIdNames("branches")); } catch (Exception e) {}
        });

        Button saveBtn = new Button("Create Department");
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            try {
                apiClient.create("departments", Map.of("name", nameField.getText(), "branchId", branchCombo.getValue().id));
                showResourceView("Departments", "departments");
            } catch (Exception ex) { showErrorPopup("Save Error", "Dept creation failed", ex); }
        });
        root.getChildren().addAll(t, grid, saveBtn);
        setView(root);
    }

    private void showUserCreateForm() {
        VBox root = new VBox(20);
        Label t = new Label("Register System User");
        t.setFont(Font.font("System", FontWeight.BOLD, 18));
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);

        TextField userField = new TextField();
        PasswordField passField = new PasswordField();
        ComboBox<IdName> branchCombo = new ComboBox<>();

        grid.addRow(0, new Label("Username:"), userField);
        grid.addRow(1, new Label("Password:"), passField);
        grid.addRow(2, new Label("Primary Branch:"), branchCombo);

        Platform.runLater(() -> {
            try { branchCombo.setItems(fetchIdNames("branches")); } catch (Exception e) {}
        });

        Button saveBtn = new Button("Register User");
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            try {
                apiClient.create("users", Map.of(
                    "username", userField.getText(),
                    "password", passField.getText(),
                    "branchId", branchCombo.getValue().id,
                    "roleIds", List.of()
                ));
                showResourceView("Users", "users");
            } catch (Exception ex) { showErrorPopup("Save Error", "User registration failed", ex); }
        });
        root.getChildren().addAll(t, grid, saveBtn);
        setView(root);
    }

    private void showBagCreateForm() {
        VBox root = new VBox(20);
        Label t = new Label("Create New Bag");
        t.setFont(Font.font("System", FontWeight.BOLD, 18));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField();
        TextField barcodeField = new TextField();
        ComboBox<IdName> branchCombo = new ComboBox<>();
        ComboBox<IdName> deptCombo = new ComboBox<>();

        grid.addRow(0, new Label("Bag Name:"), nameField);
        grid.addRow(1, new Label("Barcode:"), barcodeField);
        grid.addRow(2, new Label("Branch:"), branchCombo);
        grid.addRow(3, new Label("Department:"), deptCombo);

        // Load data for combos
        Platform.runLater(() -> {
            try {
                branchCombo.setItems(fetchIdNames("branches"));
                deptCombo.setItems(fetchIdNames("departments"));
            } catch (Exception e) {
                showErrorPopup("Data Error", "Could not load branches/departments", e);
            }
        });

        Button saveBtn = new Button("Create Bag");
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            try {
                Map<String, Object> body = Map.of(
                    "name", nameField.getText(),
                    "barcode", barcodeField.getText(),
                    "branchId", branchCombo.getValue().id,
                    "assignedDepartmentId", deptCombo.getValue().id,
                    "expectedItems", List.of() // Simplified for now
                );
                apiClient.create("bags", body);
                showResourceView("Bags", "bags");
            } catch (Exception ex) {
                showErrorPopup("Save Error", "Could not create bag", ex);
            }
        });

        root.getChildren().addAll(t, grid, saveBtn);
        setView(root);
    }

    private void showDisplacementCreateForm() {
        VBox root = new VBox(20);
        Label t = new Label("New Temporary Displacement (Borrowing)");
        t.setFont(Font.font("System", FontWeight.BOLD, 18));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        ComboBox<IdName> itemCombo = new ComboBox<>();
        TextField borrowerField = new TextField();
        TextArea reasonArea = new TextArea();
        reasonArea.setPrefRowCount(3);
        DatePicker datePicker = new DatePicker(java.time.LocalDate.now().plusDays(7));

        grid.addRow(0, new Label("Item to Borrow:"), itemCombo);
        grid.addRow(1, new Label("Borrower Name:"), borrowerField);
        grid.addRow(2, new Label("Reason:"), reasonArea);
        grid.addRow(3, new Label("Expected Return:"), datePicker);

        Platform.runLater(() -> {
            try {
                itemCombo.setItems(fetchIdNames("items"));
            } catch (Exception e) {
                showErrorPopup("Data Error", "Could not load items", e);
            }
        });

        Button saveBtn = new Button("Register Displacement");
        saveBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            try {
                Map<String, Object> body = Map.of(
                    "itemId", itemCombo.getValue().id,
                    "borrowerName", borrowerField.getText(),
                    "reason", reasonArea.getText(),
                    "expectedReturnDate", datePicker.getValue().atStartOfDay().atOffset(java.time.ZoneOffset.UTC).toString()
                );
                apiClient.create("displacements", body);
                showResourceView("Displacements", "displacements");
            } catch (Exception ex) {
                showErrorPopup("Save Error", "Could not register borrowing", ex);
            }
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

    private void loadTableData(TableView<Map<String, Object>> table, String resource) {
        try {
            List<Map<String, Object>> data = apiClient.list(resource);
            table.getColumns().clear();
            if (!data.isEmpty()) {
                Map<String, Object> first = data.get(0);
                for (String key : first.keySet()) {
                    TableColumn<Map<String, Object>, String> col = new TableColumn<>(key.toUpperCase());
                    col.setCellValueFactory(cellData -> {
                        Object val = cellData.getValue().get(key);
                        if (val instanceof Map) {
                            val = ((Map<?, ?>) val).get("name");
                        }
                        return new javafx.beans.property.SimpleStringProperty(val == null ? "" : val.toString());
                    });
                    table.getColumns().add(col);
                }
                table.setItems(FXCollections.observableArrayList(data));
            }
        } catch (Exception e) {
            showErrorPopup("Fetch Error", "Could not load " + resource, e);
        }
    }

    private void setView(Node node) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(node);
    }

    private void showPlaceholder(String name) {
        VBox root = new VBox(10);
        root.setAlignment(Pos.CENTER);
        root.getChildren().addAll(new Label("🚧 " + name), new Label("This module is under development."));
        setView(root);
    }

    private void showBagAuditScanner() {
        VBox root = new VBox(15);
        Label title = new Label("Live Bag Audit Scanner");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        TextField barcodeField = new TextField();
        barcodeField.setPromptText("Scan Bag Barcode...");
        Button scanBtn = new Button("Scan");
        searchBox.getChildren().addAll(barcodeField, scanBtn);
        
        VBox resultArea = new VBox(10);
        
        scanBtn.setOnAction(e -> {
            try {
                Map<String, Object> bag = apiClient.get("bags/barcode/" + barcodeField.getText());
                resultArea.getChildren().clear();
                resultArea.getChildren().add(new Label("Bag: " + bag.get("name") + " (" + bag.get("barcode") + ")"));
                
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>) bag.get("expectedItems");
                if (items == null || items.isEmpty()) {
                    resultArea.getChildren().add(new Label("No expected items configured for this bag."));
                } else {
                    TableView<Map<String, Object>> table = new TableView<>();
                    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
                    
                    TableColumn<Map<String, Object>, String> itemCol = new TableColumn<>("EXPECTED ITEM");
                    itemCol.setCellValueFactory(cellData -> {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> itemData = (Map<String, Object>) cellData.getValue().get("item");
                        return new javafx.beans.property.SimpleStringProperty(itemData != null ? itemData.get("name").toString() : "");
                    });
                    
                    TableColumn<Map<String, Object>, String> qtyCol = new TableColumn<>("EXPECTED QTY");
                    qtyCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().get("expectedQuantity").toString()));
                    
                    table.getColumns().addAll(itemCol, qtyCol);
                    table.setItems(FXCollections.observableArrayList(items));
                    resultArea.getChildren().add(table);
                    
                    Button displaceBtn = new Button("Report Missing Item (Create Displacement)");
                    displaceBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                    displaceBtn.setOnAction(evt -> showDisplacementCreateForm());
                    resultArea.getChildren().add(displaceBtn);
                }
            } catch (Exception ex) {
                showErrorPopup("Audit Error", "Failed to fetch bag with barcode: " + barcodeField.getText(), ex);
            }
        });
        
        root.getChildren().addAll(title, searchBox, resultArea);
        setView(root);
    }

    private void showSettingsPopup() {
        Stage settingsStage = new Stage();
        settingsStage.initOwner(stage);
        settingsStage.setTitle("Connection Settings");
        TextField urlField = new TextField(apiClient.getBaseUrl());
        urlField.setPrefWidth(300);
        Button save = new Button("Save and Restart");
        save.setOnAction(e -> {
            configManager.setApiUrl(urlField.getText());
            this.apiClient = new ApiClient(urlField.getText());
            settingsStage.close();
            showLogin();
        });
        VBox layout = new VBox(10, new Label("Backend API URL:"), urlField, save);
        layout.setPadding(new Insets(20));
        settingsStage.setScene(new Scene(layout));
        settingsStage.show();
    }

    private void showErrorPopup(String title, String message, Exception ex) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(message);
            alert.getDialogPane().setContent(new VBox(10, new Label("Error: " + ex.getMessage()), new Label("Check connection or contact programmer.")));
            alert.showAndWait();
        });
    }
}
