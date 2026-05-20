package com.inventorymanager.frontend.ui;

import com.inventorymanager.frontend.api.ApiClient;
import com.inventorymanager.frontend.ui.views.*;
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

import java.time.LocalDateTime;
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
    private ViewContext viewContext;
    private FormView formView;
    
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
        
        // STABILIZATION: Initialize core layout containers immediately to prevent startup NPE in setView
        this.mainLayout = new BorderPane();
        this.contentArea = new StackPane();
        this.contentArea.setPadding(new Insets(20));
        this.contentArea.setStyle("-fx-background-color: white;");
        this.mainLayout.setCenter(contentArea);

        if (stage != null) {
            stage.setScene(new Scene(mainLayout, 1024, 768));
            stage.setTitle(bundle.getString("title.main"));
        }
        
        this.viewContext = new ViewContext(apiClient, bundle, configManager, this::setView, this::showLogin, () -> {
            try { showDashboard(); } catch (Exception ignored) {
                // If dashboard fails (e.g. session expired), return to login
                showLogin();
            }
        }, this::showSettingsPopup, (title, resource) -> {
            if (formView != null) formView.showUpsertForm(title, resource, null);
        });
        
        this.formView = new FormView(viewContext);
    }

    private void loadBundle() {
        this.bundle = ResourceBundle.getBundle("i18n/messages", new Locale(configManager.getLanguage()));
    }

    public void showLogin() {
        if (stage != null) {
            mainLayout.setLeft(null); // Hide sidebar during login
            mainLayout.setTop(null);
        }
        new LoginView(viewContext).show();
    }

    static boolean computeIsAdmin(Set<String> permissions) {
        // CONSISTENCY: Require a full mapping of admin permissions to unlock the admin navigation block.
        List<String> requiredAdminPerms = List.of(
            "get_audit_logs", "create_branch", "create_department", "create_category", 
            "create_user", "create_role", "create_permission", "create_state", 
            "create_municipality", "create_parish"
        );
        return permissions.containsAll(requiredAdminPerms);
    }

    private void showDashboard() throws Exception {
        this.currentUser = apiClient.me();
        @SuppressWarnings("unchecked")
        List<String> permissionsPayload = (List<String>) currentUser.getOrDefault("permissions", List.of());
        Set<String> permissions = permissionsPayload.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        
        this.isAdmin = computeIsAdmin(permissions);

        VBox sidebar = new VBox(5);
        sidebar.setPadding(new Insets(10));
        sidebar.setPrefWidth(220);
        sidebar.setStyle("-fx-background-color: #2c3e50;");

        Label brand = new Label(bundle.getString("title.main"));
        brand.setTextFill(Color.WHITE);
        brand.setFont(Font.font("System", FontWeight.BOLD, 14));
        brand.setPadding(new Insets(10, 10, 20, 10));

        sidebar.getChildren().add(brand);
        sidebar.getChildren().add(createNavButton(bundle.getString("nav.dashboard"), () -> new DashboardView(viewContext, globalContext).show()));
        sidebar.getChildren().add(new Separator());
        sidebar.getChildren().add(createNavGroupLabel(bundle.getString("nav.inventory")));
        sidebar.getChildren().add(createNavButton(bundle.getString("nav.assets"), () -> new ResourceView(viewContext, bundle.getString("nav.assets"), "items").show()));
        sidebar.getChildren().add(createNavButton(bundle.getString("nav.bags"), () -> new ResourceView(viewContext, bundle.getString("nav.bags"), "bags").show()));
        
        sidebar.getChildren().add(new Separator());
        sidebar.getChildren().add(createNavGroupLabel(bundle.getString("nav.operations")));
        sidebar.getChildren().add(createNavButton(bundle.getString("nav.transfers"), () -> new ResourceView(viewContext, bundle.getString("nav.transfers"), "item-requests").show()));
        sidebar.getChildren().add(createNavButton(bundle.getString("nav.audit"), this::showBagAuditScanner));
        sidebar.getChildren().add(createNavButton(bundle.getString("nav.displacements"), () -> new ResourceView(viewContext, bundle.getString("nav.displacements"), "displacements").show()));
        
        if (isAdmin) {
            sidebar.getChildren().add(new Separator());
            sidebar.getChildren().add(createNavGroupLabel(bundle.getString("nav.admin")));
            sidebar.getChildren().add(createNavButton(bundle.getString("nav.audit_logs"), () -> new AuditView(viewContext).show()));
            sidebar.getChildren().add(createNavButton(bundle.getString("nav.branches"), () -> new ResourceView(viewContext, bundle.getString("nav.branches"), "branches").show()));
            sidebar.getChildren().add(createNavButton(bundle.getString("nav.departments"), () -> new ResourceView(viewContext, bundle.getString("nav.departments"), "departments").show()));
            sidebar.getChildren().add(createNavButton(bundle.getString("nav.categories"), () -> new ResourceView(viewContext, bundle.getString("nav.categories"), "categories").show()));
            
            sidebar.getChildren().add(new Separator());
            sidebar.getChildren().add(createNavGroupLabel(bundle.containsKey("nav.identity") ? bundle.getString("nav.identity") : "IDENTITY"));
            sidebar.getChildren().add(createNavButton(bundle.getString("nav.users"), () -> new ResourceView(viewContext, bundle.getString("nav.users"), "users").show()));
            sidebar.getChildren().add(createNavButton(bundle.containsKey("nav.roles") ? bundle.getString("nav.roles") : "🛡️ Roles", () -> new ResourceView(viewContext, "Roles", "roles").show()));
            sidebar.getChildren().add(createNavButton(bundle.containsKey("nav.permissions") ? bundle.getString("nav.permissions") : "🔑 Permissions", () -> new ResourceView(viewContext, "Permissions", "permissions").show()));

            sidebar.getChildren().add(new Separator());
            sidebar.getChildren().add(createNavGroupLabel(bundle.containsKey("nav.locations") ? bundle.getString("nav.locations") : "LOCATIONS"));
            sidebar.getChildren().add(createNavButton(bundle.containsKey("nav.states") ? bundle.getString("nav.states") : "🗺️ States", () -> new ResourceView(viewContext, "States", "states").show()));
            sidebar.getChildren().add(createNavButton(bundle.containsKey("nav.municipalities") ? bundle.getString("nav.municipalities") : "🏘️ Municipalities", () -> new ResourceView(viewContext, "Municipalities", "municipalities").show()));
            sidebar.getChildren().add(createNavButton(bundle.containsKey("nav.parishes") ? bundle.getString("nav.parishes") : "📍 Parishes", () -> new ResourceView(viewContext, "Parishes", "parishes").show()));
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
                new DashboardView(viewContext, globalContext).show();
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
        
        // Show initial view
        new DashboardView(viewContext, globalContext).show();
        
        if (stage != null) {
            stage.setScene(new Scene(mainLayout, 1366, 900));
        }
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

    private void setView(Node node) {
        // DEFENSIVE: Ensure contentArea is initialized before use
        if (contentArea == null) {
            throw new IllegalStateException("UI contentArea not initialized. App lifecycle failure.");
        }
        contentArea.getChildren().clear();
        contentArea.getChildren().add(node);
        
        if (stage != null && !stage.isShowing()) {
            stage.show();
        }
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
                // 1. Identify bag
                Map<String, Object> bag = apiClient.get("bags/barcode/" + barcodeField.getText());
                Long bagId = ((Number) bag.get("id")).longValue();
                
                // 2. Perform live audit
                Map<String, Object> audit = apiClient.get("bags/" + bagId + "/audit");
                resultArea.getChildren().clear();
                resultArea.getChildren().add(new Label("Bag: " + audit.get("name") + " [" + audit.get("barcode") + "]"));
                
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>) audit.get("items");
                if (items == null || items.isEmpty()) {
                    resultArea.getChildren().add(new Label(bundle.getString("audit.scanner.empty")));
                } else {
                    TableView<Map<String, Object>> table = new TableView<>();
                    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
                    
                    TableColumn<Map<String, Object>, String> itemCol = new TableColumn<>("ITEM");
                    itemCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().get("itemName").toString()));
                    
                    TableColumn<Map<String, Object>, String> expectedCol = new TableColumn<>("EXPECTED");
                    expectedCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().get("intendedQuantity").toString()));
                    
                    TableColumn<Map<String, Object>, String> displacedCol = new TableColumn<>("BORROWED");
                    displacedCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().get("displacedQuantity").toString()));
                    
                    TableColumn<Map<String, Object>, String> remainingCol = new TableColumn<>("REMAINING");
                    remainingCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().get("remainingQuantity").toString()));
                    remainingCol.setCellFactory(tc -> new TableCell<>() {
                        @Override protected void updateItem(String val, boolean empty) {
                            super.updateItem(val, empty);
                            if (empty || val == null) setGraphic(null);
                            else {
                                Label l = new Label(val);
                                if (Integer.parseInt(val) == 0) l.setTextFill(Color.RED);
                                setGraphic(l);
                            }
                        }
                    });

                    TableColumn<Map<String, Object>, String> anomalyCol = new TableColumn<>("ANOMALIES");
                    anomalyCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().get("anomalyCount").toString()));
                    anomalyCol.setCellFactory(tc -> new TableCell<>() {
                        @Override protected void updateItem(String val, boolean empty) {
                            super.updateItem(val, empty);
                            if (empty || val == null) setGraphic(null);
                            else {
                                Label l = new Label(val);
                                if (Integer.parseInt(val) > 0) {
                                    l.setTextFill(Color.ORANGE);
                                    l.setStyle("-fx-font-weight: bold;");
                                }
                                setGraphic(l);
                            }
                        }
                    });
                    
                    TableColumn<Map<String, Object>, Void> actionCol = new TableColumn<>("ACTIONS");
                    actionCol.setCellFactory(p -> new TableCell<>() {
                        private final Button reportBtn = new Button("MISSING");
                        {
                            reportBtn.setStyle("-fx-font-size: 10px; -fx-background-color: #e74c3c; -fx-text-fill: white;");
                            reportBtn.setOnAction(evt -> {
                                Map<String, Object> row = getTableView().getItems().get(getIndex());
                                Map<String, Object> prefill = Map.of(
                                    "itemId", row.get("itemId"),
                                    "borrowerName", "AUDIT_RECOVERY",
                                    "reason", "Missing from bag " + audit.get("barcode")
                                );
                                if (formView != null) formView.showDisplacementUpsertForm(null, prefill);
                            });
                        }
                        @Override protected void updateItem(Void item, boolean empty) {
                            super.updateItem(item, empty);
                            setGraphic(empty ? null : reportBtn);
                        }
                    });
                    
                    table.getColumns().addAll(itemCol, expectedCol, displacedCol, remainingCol, anomalyCol, actionCol);
                    table.setItems(FXCollections.observableArrayList(items));
                    resultArea.getChildren().add(table);
                }
            } catch (Exception ex) { UIUtils.showErrorPopup(bundle.getString("audit.error"), "Failed to fetch audit data", ex); }
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
            try { showDashboard(); } catch (Exception ex) { UIUtils.showErrorPopup("UI Error", "Could not reload dashboard", ex); }
        });
        layout.getChildren().addAll(
            new Label(bundle.getString("settings.url")), urlField,
            new Label(bundle.getString("settings.lang")), langCombo,
            save
        );
        settingsStage.setScene(new Scene(layout));
        settingsStage.show();
    }
}
