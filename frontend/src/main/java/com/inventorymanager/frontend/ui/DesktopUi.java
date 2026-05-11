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
        brand.setFont(Font.font("System", FontWeight.BOLD, 14));
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
            sidebar.getChildren().add(createNavButton(bundle.getString("nav.categories"), () -> showResourceView(bundle.getString("nav.categories"), "categories")));
            
            sidebar.getChildren().add(new Separator());
            sidebar.getChildren().add(createNavGroupLabel(bundle.containsKey("nav.identity") ? bundle.getString("nav.identity") : "IDENTITY"));
            sidebar.getChildren().add(createNavButton(bundle.getString("nav.users"), () -> showResourceView(bundle.getString("nav.users"), "users")));
            sidebar.getChildren().add(createNavButton(bundle.containsKey("nav.roles") ? bundle.getString("nav.roles") : "🛡️ Roles", () -> showResourceView("Roles", "roles")));
            sidebar.getChildren().add(createNavButton(bundle.containsKey("nav.permissions") ? bundle.getString("nav.permissions") : "🔑 Permissions", () -> showResourceView("Permissions", "permissions")));

            sidebar.getChildren().add(new Separator());
            sidebar.getChildren().add(createNavGroupLabel(bundle.containsKey("nav.locations") ? bundle.getString("nav.locations") : "LOCATIONS"));
            sidebar.getChildren().add(createNavButton(bundle.containsKey("nav.states") ? bundle.getString("nav.states") : "🗺️ States", () -> showResourceView("States", "states")));
            sidebar.getChildren().add(createNavButton(bundle.containsKey("nav.municipalities") ? bundle.getString("nav.municipalities") : "🏘️ Municipalities", () -> showResourceView("Municipalities", "municipalities")));
            sidebar.getChildren().add(createNavButton(bundle.containsKey("nav.parishes") ? bundle.getString("nav.parishes") : "📍 Parishes", () -> showResourceView("Parishes", "parishes")));
        }
        
        sidebar.getChildren().add(new Separator());
        sidebar.getChildren().add(createNavButton(bundle.getString("nav.settings"), this::showSettingsPopup));

        ScrollPane sidebarScroll = new ScrollPane(sidebar);
        sidebarScroll.setFitToWidth(true);
        sidebarScroll.setPrefWidth(240);
        sidebarScroll.setStyle("-fx-background-color: #2c3e50; -fx-border-color: #2c3e50;");
        sidebarScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

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

        mainLayout.setLeft(sidebarScroll);
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
        Label t = new Label(title); t.setTextFill(Color.GRAY);
        Label v = new Label(value); v.setFont(Font.font("System", FontWeight.BOLD, 24));
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
        
        Button refreshBtn = new Button(bundle.getString("resource.refresh"));
        Button addBtn = new Button(bundle.getString("resource.add"));
        addBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        header.getChildren().addAll(t, spacer, refreshBtn, addBtn);

        TableView<Map<String, Object>> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
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
        
        applyFilter.setOnAction(e -> loadFilteredData(table, title, resource, stateFilter.getValue(), branchFilter.getValue()));
        clearFilter.setOnAction(e -> {
            stateFilter.setValue(null);
            branchFilter.setValue(null);
            loadFilteredData(table, title, resource, null, null);
        });
        refreshBtn.setOnAction(e -> loadFilteredData(table, title, resource, stateFilter.getValue(), branchFilter.getValue()));
        addBtn.setOnAction(e -> showUpsertForm(title, resource, null));

        Platform.runLater(() -> {
            try {
                stateFilter.setItems(fetchIdNames("states"));
                branchFilter.setItems(fetchIdNames("branches"));
            } catch (Exception ignored) {}
        });

        root.getChildren().addAll(header, filterBar, table);
        setView(root);
        loadFilteredData(table, title, resource, null, null);
    }

    private void loadFilteredData(TableView<Map<String, Object>> table, String title, String resource, IdName state, IdName branch) {
        try {
            String path = resource + "?page=1&pageSize=100";
            if (state != null) path += "&stateId=" + state.id;
            if (branch != null) path += "&branchId=" + branch.id;
            List<Map<String, Object>> data = apiClient.list(path);
            updateTableColumns(table, data, title, resource);
        } catch (Exception e) {
            showErrorPopup("Fetch Error", "Could not load data", e);
        }
    }

    private void updateTableColumns(TableView<Map<String, Object>> table, List<Map<String, Object>> data, String title, String resource) {
        if (table.getColumns().isEmpty() && data != null && !data.isEmpty()) {
            Map<String, Object> first = data.get(0);
            for (String key : first.keySet()) {
                if (key.equals("id")) continue; // Hidden ID
                String headerText = bundle.containsKey("col." + key) ? bundle.getString("col." + key) : key.toUpperCase();
                TableColumn<Map<String, Object>, String> col = new TableColumn<>(headerText);
                col.setCellValueFactory(cellData -> {
                    Object val = extractDeepValue(cellData.getValue().get(key));
                    return new javafx.beans.property.SimpleStringProperty(val == null ? "" : val.toString());
                });
                col.setCellFactory(tc -> new TableCell<>() {
                    private final javafx.scene.text.Text text = new javafx.scene.text.Text();
                    @Override protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) { setGraphic(null); }
                        else {
                            text.setText(item);
                            text.wrappingWidthProperty().bind(tc.widthProperty().subtract(10));
                            setGraphic(text);
                        }
                    }
                });
                col.setCellFactory(tc -> {
                    TableCell<Map<String, Object>, String> cell = new TableCell<>() {
                        private final javafx.scene.text.Text text = new javafx.scene.text.Text();
                        @Override
                        protected void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setGraphic(null);
                            } else {
                                text.setText(item);
                                text.wrappingWidthProperty().bind(tc.widthProperty().subtract(10));
                                setGraphic(text);
                            }
                        }
                    };
                    return cell;
                });
                table.getColumns().add(col);
            }
            if (title != null && resource != null) {
                String actionText = bundle.containsKey("table.actions") ? bundle.getString("table.actions") : "ACTIONS";
                TableColumn<Map<String, Object>, Void> actionCol = new TableColumn<>(actionText);
                actionCol.setCellFactory(param -> new TableCell<>() {
                    private final Button editBtn = new Button(bundle.containsKey("btn.edit") ? bundle.getString("btn.edit") : "Edit");
                    {
                        editBtn.setStyle("-fx-font-size: 10px;");
                        editBtn.setOnAction(event -> {
                            Map<String, Object> rowData = getTableView().getItems().get(getIndex());
                            showUpsertForm(title, resource, rowData);
                        });
                    }
                    @Override protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : editBtn);
                    }
                });
                table.getColumns().add(actionCol);
            }
        }
        if (data != null) {
            table.setItems(FXCollections.observableArrayList(data));
        } else {
            table.setItems(FXCollections.observableArrayList());
        }
    }

    private Object extractDeepValue(Object val) {
<<<<<<< HEAD
        if (val == null) return "";
        if (val instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) val;
            String[] preferredKeys = {"name", "username", "title", "barcode", "requestedItemName", "borrowerName"};
            for (String key : preferredKeys) {
                if (m.containsKey(key) && m.get(key) != null) return m.get(key);
            }
            
            // Fallback: search one level deeper if no primary key found
            for (Object subVal : m.values()) {
                if (subVal instanceof Map) {
                    Object deep = extractDeepValue(subVal);
                    if (deep != null && !deep.toString().startsWith("{") && !deep.toString().isBlank()) {
                        return deep;
                    }
                }
            }
            
            if (m.containsKey("id")) return "#" + m.get("id");
            return m.toString();
        } else if (val instanceof List) {
            return ((List<?>) val).stream()
                    .map(this::extractDeepValue)
                    .map(Object::toString)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.joining(", "));
=======
        if (val instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) val;
            if (m.containsKey("name")) return m.get("name");
            if (m.containsKey("username")) return m.get("username");
            return m.toString();
        } else if (val instanceof List) {
            return ((List<?>) val).stream().map(this::extractDeepValue).map(Object::toString).collect(Collectors.joining(", "));
>>>>>>> master
        }
        return val;
    }

    private void showGlobalAuditView() {
        VBox root = new VBox(15);
        Label title = new Label(bundle.getString("audit.title"));
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        HBox controls = new HBox(10);
        ComboBox<String> entityType = new ComboBox<>(FXCollections.observableArrayList(
            "item", "branch", "user", "bag", "item_request", "displacement", "department", "category", "role", "state", "municipality", "parish"
        ));
        entityType.setPromptText(bundle.getString("audit.type"));
        TextField entityId = new TextField();
        entityId.setPromptText(bundle.getString("audit.id") + " (Optional)");
        Button fetchBtn = new Button(bundle.getString("audit.fetch"));
        controls.getChildren().addAll(new Label(bundle.getString("audit.query")), entityType, entityId, fetchBtn);
        TableView<Map<String, Object>> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        fetchBtn.setOnAction(e -> {
            if (entityType.getValue() == null || entityId.getText().isBlank()) {
                String errMsg = bundle.containsKey("error.missing_audit_params") ? bundle.getString("error.missing_audit_params") : "Please select type and enter ID";
                showWarningPopup("Input Error", errMsg);
                return;
            }
            try {
                String path = "audit-logs/" + entityType.getValue();
                if (!entityId.getText().isBlank()) path += "/" + entityId.getText();
                Map<String, Object> response = apiClient.get(path);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
                updateTableColumns(table, data, null, null);
            } catch (Exception ex) { showErrorPopup(bundle.getString("audit.error"), "Failed to fetch logs", ex); }
        });
        root.getChildren().addAll(title, controls, table);
        setView(root);
    }

    private void showUpsertForm(String title, String resource, Map<String, Object> rowData) {
        switch (resource) {
            case "items" -> showItemUpsertForm(rowData);
            case "bags" -> showBagUpsertForm(rowData);
            case "displacements" -> showDisplacementUpsertForm(rowData);
            case "branches" -> showBranchUpsertForm(rowData);
            case "departments" -> showDepartmentUpsertForm(rowData);
            case "users" -> showUserUpsertForm(rowData);
            case "municipalities" -> showMunicipalityUpsertForm(rowData);
            case "parishes" -> showParishUpsertForm(rowData);
            case "states", "categories", "roles", "permissions" -> showNamedUpsertForm(title, resource, rowData);
            default -> showPlaceholder("Form for " + title);
        }
    }

    private void showNamedUpsertForm(String title, String resource, Map<String, Object> rowData) {
        boolean isEdit = rowData != null;
        VBox root = new VBox(20);
        Label t = new Label(isEdit ? bundle.getString("form.edit") + " " + title : bundle.getString("resource.add") + " " + title);
        t.setFont(Font.font("System", FontWeight.BOLD, 18));
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10);
        TextField nameField = new TextField(isEdit && rowData.get("name") != null ? rowData.get("name").toString() : "");
        grid.addRow(0, new Label(bundle.getString("form.name")), nameField);
        
        Button saveBtn = new Button(bundle.getString("form.save"));
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            try {
                Map<String, Object> payload = Map.of("name", nameField.getText());
                if (isEdit) {
                    apiClient.update(resource, ((Number)rowData.get("id")).longValue(), payload);
                } else {
                    apiClient.create(resource, payload);
                }
                showResourceView(title, resource);
            } catch (Exception ex) { showErrorPopup("Save Error", "Failed", ex); }
        });
        root.getChildren().addAll(t, grid, saveBtn);
        setView(root);
    }

    private void showItemUpsertForm(Map<String, Object> rowData) {
        boolean isEdit = rowData != null;
        VBox root = new VBox(20);
        Label t = new Label(isEdit ? bundle.getString("form.edit") : bundle.getString("resource.add"));
        t.setFont(Font.font("System", FontWeight.BOLD, 18));
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10);
        TextField nameField = new TextField(isEdit && rowData.get("name") != null ? rowData.get("name").toString() : "");
        TextField qtyField = new TextField(isEdit && rowData.get("quantity") != null ? rowData.get("quantity").toString() : "1");
        ComboBox<String> unitCombo = new ComboBox<>(FXCollections.observableArrayList("UND", "KG", "L", "M"));
        unitCombo.setValue(isEdit && rowData.get("unit") != null ? rowData.get("unit").toString() : "UND");
        ComboBox<IdName> branchCombo = new ComboBox<>();
        ComboBox<IdName> deptCombo = new ComboBox<>();
        grid.addRow(0, new Label(bundle.getString("form.name")), nameField);
        grid.addRow(1, new Label(bundle.getString("form.qty")), qtyField);
        grid.addRow(2, new Label(bundle.getString("form.unit")), unitCombo);
        grid.addRow(3, new Label(bundle.getString("form.branch")), branchCombo);
        grid.addRow(4, new Label(bundle.containsKey("form.dept") ? bundle.getString("form.dept") : "Dept Name:"), deptCombo);
        Platform.runLater(() -> {
            try {
                ObservableList<IdName> branches = fetchIdNames("branches");
                branchCombo.setItems(branches);
                if (isEdit && rowData.get("branch") instanceof Map) {
                    Long branchId = ((Number) ((Map<?,?>)rowData.get("branch")).get("id")).longValue();
                    branches.stream().filter(b -> b.id.equals(branchId)).findFirst().ifPresent(branchCombo::setValue);
                }
                branchCombo.setOnAction(e -> {
                   try {
                       if (branchCombo.getValue() != null) {
                           deptCombo.setItems(fetchIdNames("departments?branchId=" + branchCombo.getValue().id));
                       }
                   } catch (Exception ignored) {}
                });

                if (isEdit && rowData.get("department") instanceof Map) {
                    Long deptId = ((Number) ((Map<?,?>)rowData.get("department")).get("id")).longValue();
                    ObservableList<IdName> depts = fetchIdNames("departments?branchId=" + ((Number) ((Map<?,?>)rowData.get("branch")).get("id")).longValue());
                    deptCombo.setItems(depts);
                    depts.stream().filter(d -> d.id.equals(deptId)).findFirst().ifPresent(deptCombo::setValue);
                } else if (branchCombo.getValue() != null) {
                    deptCombo.setItems(fetchIdNames("departments?branchId=" + branchCombo.getValue().id));
                }
            } catch (Exception ignored) {}
        });
        Button saveBtn = new Button(bundle.getString("form.save"));
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            try {
                Map<String, Object> payload = Map.of(
                    "name", nameField.getText(), "quantity", Integer.parseInt(qtyField.getText()),
                    "unit", unitCombo.getValue(), "branchId", branchCombo.getValue().id,
                    "departmentId", deptCombo.getValue().id
                );
                if (isEdit) {
                    apiClient.update("items", ((Number)rowData.get("id")).longValue(), payload);
                } else {
                    apiClient.create("items", payload);
                }
                showResourceView(bundle.getString("nav.assets"), "items");
            } catch (Exception ex) { showErrorPopup("Save Error", "Failed", ex); }
        });
        root.getChildren().addAll(t, grid, saveBtn);
        setView(root);
    }

    private void showBranchUpsertForm(Map<String, Object> rowData) {
        boolean isEdit = rowData != null;
        VBox root = new VBox(20);
        Label t = new Label(isEdit ? bundle.getString("form.edit") : bundle.getString("form.register") + " Branch");
        t.setFont(Font.font("System", FontWeight.BOLD, 18));
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10);
        TextField nameField = new TextField(isEdit && rowData.get("name") != null ? rowData.get("name").toString() : "");
        TextArea addrArea = new TextArea(isEdit && rowData.get("address") != null ? rowData.get("address").toString() : ""); 
        addrArea.setPrefRowCount(2);
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
                ObservableList<IdName> states = fetchIdNames("states");
                stateCombo.setItems(states);
                if (isEdit && rowData.get("state") instanceof Map) {
                    Long id = ((Number) ((Map<?,?>)rowData.get("state")).get("id")).longValue();
                    states.stream().filter(s -> s.id.equals(id)).findFirst().ifPresent(stateCombo::setValue);
                }
                stateCombo.setOnAction(e -> {
                   try { 
                       if (stateCombo.getValue() != null) {
                           muniCombo.setItems(fetchIdNames("municipalities?stateId=" + stateCombo.getValue().id));
                       }
                   } catch (Exception ignored) {}
                });
                muniCombo.setOnAction(e -> {
                   try { 
                       if (muniCombo.getValue() != null) {
                           parishCombo.setItems(fetchIdNames("parishes?municipalityId=" + muniCombo.getValue().id));
                       }
                   } catch (Exception ignored) {}
                });
                if (isEdit) {
                    Long sId = ((Number) ((Map<?,?>)rowData.get("state")).get("id")).longValue();
                    Long mId = ((Number) ((Map<?,?>)rowData.get("municipality")).get("id")).longValue();
                    Long pId = ((Number) ((Map<?,?>)rowData.get("parish")).get("id")).longValue();
                    
                    ObservableList<IdName> munis = fetchIdNames("municipalities?stateId=" + sId);
                    muniCombo.setItems(munis);
                    munis.stream().filter(m -> m.id.equals(mId)).findFirst().ifPresent(muniCombo::setValue);
                    
                    ObservableList<IdName> parishes = fetchIdNames("parishes?municipalityId=" + mId);
                    parishCombo.setItems(parishes);
                    parishes.stream().filter(p -> p.id.equals(pId)).findFirst().ifPresent(parishCombo::setValue);
                }
            } catch (Exception ignored) {}
        });
        Button saveBtn = new Button(bundle.getString("form.save"));
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            try {
                Map<String, Object> payload = Map.of(
                    "name", nameField.getText(), "address", addrArea.getText(),
                    "stateId", stateCombo.getValue().id, "municipalityId", muniCombo.getValue().id,
                    "parishId", parishCombo.getValue().id
                );
                if (isEdit) {
                    apiClient.update("branches", ((Number)rowData.get("id")).longValue(), payload);
                } else {
                    apiClient.create("branches", payload);
                }
                showResourceView(bundle.getString("nav.branches"), "branches");
            } catch (Exception ex) { showErrorPopup("Save Error", "Failed", ex); }
        });
        root.getChildren().addAll(t, grid, saveBtn);
        setView(root);
    }

    private void showMunicipalityUpsertForm(Map<String, Object> rowData) {
        boolean isEdit = rowData != null;
        VBox root = new VBox(20);
        Label t = new Label(isEdit ? bundle.getString("form.edit") : bundle.getString("resource.add") + " Municipality");
        t.setFont(Font.font("System", FontWeight.BOLD, 18));
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10);
        TextField nameField = new TextField(isEdit && rowData.get("name") != null ? rowData.get("name").toString() : "");
        ComboBox<IdName> stateCombo = new ComboBox<>();
        grid.addRow(0, new Label(bundle.getString("form.name")), nameField);
        grid.addRow(1, new Label("State:"), stateCombo);
        Platform.runLater(() -> {
            try { 
                ObservableList<IdName> states = fetchIdNames("states");
                stateCombo.setItems(states); 
                if (isEdit && rowData.get("state") instanceof Map) {
                    Long id = ((Number) ((Map<?,?>)rowData.get("state")).get("id")).longValue();
                    states.stream().filter(s -> s.id.equals(id)).findFirst().ifPresent(stateCombo::setValue);
                }
            } catch (Exception ignored) {}
        });
        Button saveBtn = new Button(bundle.getString("form.save"));
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            try {
                if (stateCombo.getValue() == null) throw new IllegalArgumentException("State must be selected");
                Map<String, Object> payload = Map.of("name", nameField.getText(), "stateId", stateCombo.getValue().id);
                if (isEdit) {
                    apiClient.update("municipalities", ((Number)rowData.get("id")).longValue(), payload);
                } else {
                    apiClient.create("municipalities", payload);
                }
                showResourceView("Municipalities", "municipalities");
            } catch (Exception ex) { showErrorPopup("Save Error", "Failed", ex); }
        });
        root.getChildren().addAll(t, grid, saveBtn);
        setView(root);
    }

    private void showParishUpsertForm(Map<String, Object> rowData) {
        boolean isEdit = rowData != null;
        VBox root = new VBox(20);
        Label t = new Label(isEdit ? bundle.getString("form.edit") : bundle.getString("resource.add") + " Parish");
        t.setFont(Font.font("System", FontWeight.BOLD, 18));
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10);
        TextField nameField = new TextField(isEdit && rowData.get("name") != null ? rowData.get("name").toString() : "");
        ComboBox<IdName> muniCombo = new ComboBox<>();
        grid.addRow(0, new Label(bundle.getString("form.name")), nameField);
        grid.addRow(1, new Label("Municipality:"), muniCombo);
        Platform.runLater(() -> {
            try { 
                ObservableList<IdName> munis = fetchIdNames("municipalities");
                muniCombo.setItems(munis); 
                if (isEdit && rowData.get("municipality") instanceof Map) {
                    Long id = ((Number) ((Map<?,?>)rowData.get("municipality")).get("id")).longValue();
                    munis.stream().filter(m -> m.id.equals(id)).findFirst().ifPresent(muniCombo::setValue);
                }
            } catch (Exception ignored) {}
        });
        Button saveBtn = new Button(bundle.getString("form.save"));
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            try {
                if (muniCombo.getValue() == null) throw new IllegalArgumentException("Municipality must be selected");
                Map<String, Object> payload = Map.of("name", nameField.getText(), "municipalityId", muniCombo.getValue().id);
                if (isEdit) {
                    apiClient.update("parishes", ((Number)rowData.get("id")).longValue(), payload);
                } else {
                    apiClient.create("parishes", payload);
                }
                showResourceView("Parishes", "parishes");
            } catch (Exception ex) { showErrorPopup("Save Error", "Failed", ex); }
        });
        root.getChildren().addAll(t, grid, saveBtn);
        setView(root);
    }

    private void showDepartmentUpsertForm(Map<String, Object> rowData) {
        boolean isEdit = rowData != null;
        VBox root = new VBox(20);
        Label t = new Label(isEdit ? bundle.getString("form.edit") : bundle.getString("form.create") + " Department");
        t.setFont(Font.font("System", FontWeight.BOLD, 18));
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10);
        TextField nameField = new TextField(isEdit && rowData.get("name") != null ? rowData.get("name").toString() : "");
        ComboBox<IdName> branchCombo = new ComboBox<>();
        grid.addRow(0, new Label(bundle.getString("form.dept")), nameField);
        grid.addRow(1, new Label(bundle.getString("form.branch")), branchCombo);
        Platform.runLater(() -> {
            try { 
                ObservableList<IdName> branches = fetchIdNames("branches");
                branchCombo.setItems(branches); 
                if (isEdit && rowData.get("branch") instanceof Map) {
                    Long id = ((Number) ((Map<?,?>)rowData.get("branch")).get("id")).longValue();
                    branches.stream().filter(b -> b.id.equals(id)).findFirst().ifPresent(branchCombo::setValue);
                }
            } catch (Exception ignored) {}
        });
        Button saveBtn = new Button(bundle.getString("form.save"));
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            try {
                Map<String, Object> payload = Map.of("name", nameField.getText(), "branchId", branchCombo.getValue().id);
                if (isEdit) {
                    apiClient.update("departments", ((Number)rowData.get("id")).longValue(), payload);
                } else {
                    apiClient.create("departments", payload);
                }
                showResourceView(bundle.getString("nav.departments"), "departments");
            } catch (Exception ex) { showErrorPopup("Save Error", "Failed", ex); }
        });
        root.getChildren().addAll(t, grid, saveBtn);
        setView(root);
    }

    private void showUserUpsertForm(Map<String, Object> rowData) {
        boolean isEdit = rowData != null;
        VBox root = new VBox(20);
        Label t = new Label(isEdit ? bundle.getString("form.edit") : bundle.getString("form.register") + " User");
        t.setFont(Font.font("System", FontWeight.BOLD, 18));
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10);
        TextField userField = new TextField(isEdit && rowData.get("username") != null ? rowData.get("username").toString() : "");
        PasswordField passField = new PasswordField();
        if (!isEdit) passField.setPromptText("Required");
        else passField.setPromptText("Leave blank to keep same password");
        ComboBox<IdName> branchCombo = new ComboBox<>();
        
        ListView<IdName> rolesList = new ListView<>();
        rolesList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        rolesList.setPrefHeight(100);

        grid.addRow(0, new Label(bundle.getString("form.user")), userField);
        grid.addRow(1, new Label(bundle.getString("form.pass")), passField);
        grid.addRow(2, new Label(bundle.getString("form.branch")), branchCombo);
        grid.addRow(3, new Label(bundle.containsKey("form.roles") ? bundle.getString("form.roles") : "Roles:"), rolesList);

        Platform.runLater(() -> {
            try { 
                ObservableList<IdName> branches = fetchIdNames("branches");
                branchCombo.setItems(branches); 
                if (isEdit && rowData.get("branch") instanceof Map) {
                    Long id = ((Number) ((Map<?,?>)rowData.get("branch")).get("id")).longValue();
                    branches.stream().filter(b -> b.id.equals(id)).findFirst().ifPresent(branchCombo::setValue);
                }
                
                ObservableList<IdName> roles = fetchIdNames("roles");
                rolesList.setItems(roles);
                if (isEdit && rowData.get("roles") instanceof List) {
                    List<?> userRoles = (List<?>) rowData.get("roles");
                    for (Object roleObj : userRoles) {
                        if (roleObj instanceof Map) {
                            Long roleId = ((Number) ((Map<?,?>)roleObj).get("id")).longValue();
                            roles.stream().filter(r -> r.id.equals(roleId)).findFirst().ifPresent(rolesList.getSelectionModel()::select);
                        } else if (roleObj instanceof String) {
                            roles.stream().filter(r -> r.name.equalsIgnoreCase(roleObj.toString())).findFirst().ifPresent(rolesList.getSelectionModel()::select);
                        }
                    }
                }
            } catch (Exception ignored) {}
        });
        Button saveBtn = new Button(bundle.getString("form.save"));
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            try {
                List<Long> roleIds = rolesList.getSelectionModel().getSelectedItems().stream().map(r -> r.id).collect(Collectors.toList());
                Map<String, Object> payload = new HashMap<>();
                payload.put("username", userField.getText());
                if (!passField.getText().isBlank()) payload.put("password", passField.getText());
                else if (!isEdit) payload.put("password", "");
                if (branchCombo.getValue() != null) payload.put("branchId", branchCombo.getValue().id);
                payload.put("roleIds", roleIds);

                if (isEdit) {
                    apiClient.update("users", ((Number)rowData.get("id")).longValue(), payload);
                } else {
                    apiClient.create("users", payload);
                }
                showResourceView(bundle.getString("nav.users"), "users");
            } catch (Exception ex) { showErrorPopup("Save Error", "Failed", ex); }
        });
        root.getChildren().addAll(t, grid, saveBtn);
        setView(root);
    }

    private void showBagUpsertForm(Map<String, Object> rowData) {
        boolean isEdit = rowData != null;
        VBox root = new VBox(20);
        Label t = new Label(isEdit ? bundle.getString("form.edit") : bundle.getString("form.create") + " Bag");
        t.setFont(Font.font("System", FontWeight.BOLD, 18));
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10);
        TextField nameField = new TextField(isEdit && rowData.get("name") != null ? rowData.get("name").toString() : "");
        TextField barcodeField = new TextField(isEdit && rowData.get("barcode") != null ? rowData.get("barcode").toString() : "");
        ComboBox<IdName> branchCombo = new ComboBox<>();
        ComboBox<IdName> deptCombo = new ComboBox<>();
        grid.addRow(0, new Label(bundle.getString("form.name")), nameField);
        grid.addRow(1, new Label(bundle.getString("form.barcode")), barcodeField);
        grid.addRow(2, new Label(bundle.getString("form.branch")), branchCombo);
        grid.addRow(3, new Label("Dept:"), deptCombo);
        Platform.runLater(() -> {
            try {
                ObservableList<IdName> branches = fetchIdNames("branches");
                branchCombo.setItems(branches);
                if (isEdit && rowData.get("branch") instanceof Map) {
                    Long id = ((Number) ((Map<?,?>)rowData.get("branch")).get("id")).longValue();
                    branches.stream().filter(b -> b.id.equals(id)).findFirst().ifPresent(branchCombo::setValue);
                }
                branchCombo.setOnAction(e -> {
                   try {
                       if (branchCombo.getValue() != null) {
                           deptCombo.setItems(fetchIdNames("departments?branchId=" + branchCombo.getValue().id));
                       }
                   } catch (Exception ignored) {}
                });
                
                if (isEdit && rowData.get("assignedDepartment") instanceof Map) {
                    Long id = ((Number) ((Map<?,?>)rowData.get("assignedDepartment")).get("id")).longValue();
                    ObservableList<IdName> depts = fetchIdNames("departments?branchId=" + ((Number) ((Map<?,?>)rowData.get("branch")).get("id")).longValue());
                    deptCombo.setItems(depts);
                    depts.stream().filter(d -> d.id.equals(id)).findFirst().ifPresent(deptCombo::setValue);
                } else if (branchCombo.getValue() != null) {
                    deptCombo.setItems(fetchIdNames("departments?branchId=" + branchCombo.getValue().id));
                }
            } catch (Exception ignored) {}
        });
        Button saveBtn = new Button(bundle.getString("form.save"));
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            try {
                Map<String, Object> payload = Map.of(
                    "name", nameField.getText(), "barcode", barcodeField.getText(),
                    "branchId", branchCombo.getValue().id, "assignedDepartmentId", deptCombo.getValue().id,
                    "expectedItems", List.of()
                );
                if (isEdit) {
                    apiClient.update("bags", ((Number)rowData.get("id")).longValue(), payload);
                } else {
                    apiClient.create("bags", payload);
                }
                showResourceView(bundle.getString("nav.bags"), "bags");
            } catch (Exception ex) { showErrorPopup("Save Error", "Failed", ex); }
        });
        root.getChildren().addAll(t, grid, saveBtn);
        setView(root);
    }

    private void showDisplacementUpsertForm(Map<String, Object> rowData) {
        boolean isEdit = rowData != null;
        VBox root = new VBox(20);
        Label t = new Label(isEdit ? bundle.getString("form.edit") : bundle.getString("nav.displacements"));
        t.setFont(Font.font("System", FontWeight.BOLD, 18));
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10);
        ComboBox<IdName> itemCombo = new ComboBox<>();
        TextField borrowerField = new TextField(isEdit && rowData.get("borrowerName") != null ? rowData.get("borrowerName").toString() : "");
        TextArea reasonArea = new TextArea(isEdit && rowData.get("reason") != null ? rowData.get("reason").toString() : ""); 
        reasonArea.setPrefRowCount(3);
        DatePicker datePicker = new DatePicker(java.time.LocalDate.now().plusDays(7));
        grid.addRow(0, new Label("Item:"), itemCombo);
        grid.addRow(1, new Label(bundle.getString("form.borrower")), borrowerField);
        grid.addRow(2, new Label(bundle.getString("form.reason")), reasonArea);
        grid.addRow(3, new Label(bundle.getString("form.return")), datePicker);
        Platform.runLater(() -> {
            try { 
                ObservableList<IdName> items = fetchIdNames("items");
                itemCombo.setItems(items); 
                if (isEdit && rowData.get("item") instanceof Map) {
                    Long id = ((Number) ((Map<?,?>)rowData.get("item")).get("id")).longValue();
                    items.stream().filter(i -> i.id.equals(id)).findFirst().ifPresent(itemCombo::setValue);
                }
            } catch (Exception ignored) {}
        });
        Button saveBtn = new Button(bundle.getString("form.save"));
        saveBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            try {
                Map<String, Object> payload = Map.of(
                    "itemId", itemCombo.getValue().id, "borrowerName", borrowerField.getText(),
                    "reason", reasonArea.getText(),
                    "expectedReturnDate", datePicker.getValue().atStartOfDay().atOffset(java.time.ZoneOffset.UTC).toString()
                );
                if (isEdit) {
                    apiClient.update("displacements", ((Number)rowData.get("id")).longValue(), payload);
                } else {
                    apiClient.create("displacements", payload);
                }
                showResourceView(bundle.getString("nav.displacements"), "displacements");
            } catch (Exception ex) { showErrorPopup("Save Error", "Failed", ex); }
        });
        root.getChildren().addAll(t, grid, saveBtn);
        setView(root);
    }

    private ObservableList<IdName> fetchIdNames(String resource) throws Exception {
        return FXCollections.observableArrayList(
            apiClient.list(resource).stream()
                .map(m -> new IdName(((Number) m.get("id")).longValue(), m.get("name") != null ? m.get("name").toString() : m.get("username").toString()))
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
                    displaceBtn.setOnAction(evt -> showDisplacementUpsertForm(null));
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
            loadBundle();
            settingsStage.close();
            showDashboardWrapper();
        });
        layout.getChildren().addAll(
            new Label(bundle.getString("settings.url")), urlField,
            new Label(bundle.getString("settings.lang")), langCombo,
            save
        );
        settingsStage.setScene(new Scene(layout));
        settingsStage.show();
    }

    private void showDashboardWrapper() {
        try { showDashboard(); } catch (Exception ex) { showErrorPopup("UI Error", "Could not reload dashboard", ex); }
    }

    private void showWarningPopup(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText(message);
            alert.showAndWait();
        });
    }

    private void showErrorPopup(String title, String message, Exception ex) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(message);
            StringBuilder sb = new StringBuilder();
            sb.append("--- ERROR REPORT ---\n").append("Timestamp: ").append(java.time.LocalDateTime.now()).append("\n")
              .append("Title: ").append(title).append("\n").append("Message: ").append(message).append("\n")
              .append("Exception: ").append(ex.toString()).append("\n\n").append("--- STACK TRACE ---\n");
            java.io.StringWriter sw = new java.io.StringWriter();
            ex.printStackTrace(new java.io.PrintWriter(sw));
            sb.append(sw.toString());
            String detailedReport = sb.toString();
            TextArea textArea = new TextArea(detailedReport);
            textArea.setEditable(false); textArea.setWrapText(true); textArea.setPrefHeight(200); textArea.setPrefWidth(500);
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
