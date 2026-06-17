package com.inventorymanager.frontend.ui.views;

import com.inventorymanager.frontend.ui.UIUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;
import java.util.Map;

public class ResourceView {
    private final ViewContext context;
    private final String title;
    private final String resource;
    private TableView<Map<String, Object>> table;

    public ResourceView(ViewContext context, String title, String resource) {
        this.context = context;
        this.title = title;
        this.resource = resource;
    }

    public void show() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(10));

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label t = new Label(title);
        t.setFont(Font.font("System", FontWeight.BOLD, 20));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button refreshBtn = new Button(context.bundle().getString("resource.refresh"));
        Button addBtn = new Button(context.bundle().getString("resource.add"));
        addBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        header.getChildren().addAll(t, spacer, refreshBtn, addBtn);

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-table-cell-border-color: #e0e0e0;");
        VBox.setVgrow(table, Priority.ALWAYS);
        
        HBox filterBar = new HBox(10);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(10));
        filterBar.setStyle("-fx-background-color: #f8f9fa; -fx-border-radius: 5;");
        
        ComboBox<UIUtils.IdName> stateFilter = new ComboBox<>();
        stateFilter.setPromptText("State...");
        ComboBox<UIUtils.IdName> muniFilter = new ComboBox<>();
        muniFilter.setPromptText("Municipality...");
        ComboBox<UIUtils.IdName> branchFilter = new ComboBox<>();
        branchFilter.setPromptText("Branch...");
        ComboBox<UIUtils.IdName> catFilter = new ComboBox<>();
        catFilter.setPromptText("Category...");

        Button applyFilter = new Button(context.bundle().getString("resource.apply"));
        Button clearFilter = new Button(context.bundle().getString("resource.clear"));
        
        filterBar.getChildren().add(new Label(context.bundle().getString("resource.filter")));
        if (List.of("items", "branches", "departments", "municipalities").contains(resource)) filterBar.getChildren().add(stateFilter);
        if (List.of("items", "branches", "departments", "parishes").contains(resource)) filterBar.getChildren().add(muniFilter);
        if (List.of("items", "departments", "users", "bags").contains(resource)) filterBar.getChildren().add(branchFilter);
        if (List.of("items").contains(resource)) filterBar.getChildren().add(catFilter);
        filterBar.getChildren().addAll(applyFilter, clearFilter);
        
        applyFilter.setOnAction(e -> loadFilteredData(stateFilter.getValue(), muniFilter.getValue(), branchFilter.getValue(), catFilter.getValue()));
        clearFilter.setOnAction(e -> {
            stateFilter.setValue(null);
            muniFilter.setValue(null);
            branchFilter.setValue(null);
            catFilter.setValue(null);
            loadFilteredData(null, null, null, null);
        });
        refreshBtn.setOnAction(e -> loadFilteredData(stateFilter.getValue(), muniFilter.getValue(), branchFilter.getValue(), catFilter.getValue()));
        addBtn.setOnAction(e -> context.formShower().accept(title, resource));

        new Thread(() -> {
            try {
                if (List.of("items", "branches", "departments", "municipalities").contains(resource)) {
                    var items = UIUtils.fetchIdNames(context.apiClient(), "states");
                    Platform.runLater(() -> stateFilter.setItems(items));
                }
                if (List.of("items", "branches", "departments", "parishes").contains(resource)) {
                    var items = UIUtils.fetchIdNames(context.apiClient(), "municipalities");
                    Platform.runLater(() -> muniFilter.setItems(items));
                }
                if (List.of("items", "departments", "users", "bags").contains(resource)) {
                    var items = UIUtils.fetchIdNames(context.apiClient(), "branches");
                    Platform.runLater(() -> branchFilter.setItems(items));
                }
                if (List.of("items").contains(resource)) {
                    var items = UIUtils.fetchIdNames(context.apiClient(), "categories");
                    Platform.runLater(() -> catFilter.setItems(items));
                }
            } catch (Exception ex) {
                Platform.runLater(() -> UIUtils.showErrorPopup("Loader Error", "Could not load relationship data for filters", ex));
            }
        }).start();

        root.getChildren().addAll(header, filterBar, table);
        context.viewSetter().accept(root);
        loadFilteredData(null, null, null, null);
    }

    private final java.util.concurrent.atomic.AtomicInteger requestId = new java.util.concurrent.atomic.AtomicInteger(0);

    private void loadFilteredData(UIUtils.IdName state, UIUtils.IdName muni, UIUtils.IdName branch, UIUtils.IdName cat) {
        int currentReqId = requestId.incrementAndGet();
        new Thread(() -> {
            try {
                String path = resource + "?page=1&pageSize=100";
                if (state != null) path += "&stateId=" + state.id;
                if (muni != null) path += "&municipalityId=" + muni.id;
                if (branch != null) path += "&branchId=" + branch.id;
                if (cat != null) path += "&categoryId=" + cat.id;
                List<Map<String, Object>> data = context.apiClient().list(path);
                Platform.runLater(() -> {
                    if (currentReqId == requestId.get()) {
                        updateTableColumns(data, () -> loadFilteredData(state, muni, branch, cat));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (currentReqId == requestId.get()) {
                        UIUtils.showErrorPopup("Fetch Error", "Could not load data", e);
                    }
                });
            }
        }).start();
    }

    private void updateTableColumns(List<Map<String, Object>> data, Runnable onRefresh) {
        table.getColumns().clear();
        if (data != null && !data.isEmpty()) {
            java.util.Set<String> allKeys = new java.util.LinkedHashSet<>();
            for (Map<String, Object> row : data) {
                allKeys.addAll(row.keySet());
            }
            
            for (String key : allKeys) {
                if (key.equals("id")) continue; // Hidden ID
                String headerText = context.bundle().containsKey("col." + key) ? context.bundle().getString("col." + key) : key.toUpperCase();
                TableColumn<Map<String, Object>, String> col = new TableColumn<>(headerText);
                col.setCellValueFactory(cellData -> {
                    Object val = UIUtils.extractDeepValue(cellData.getValue().get(key));
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
                table.getColumns().add(col);
            }
            
            String actionText = context.bundle().containsKey("table.actions") ? context.bundle().getString("table.actions") : "ACTIONS";
            TableColumn<Map<String, Object>, Void> actionCol = new TableColumn<>(actionText);
            actionCol.setPrefWidth(120);
            actionCol.setCellFactory(param -> new TableCell<>() {
                private final Button editBtn = new Button(context.bundle().getString("btn.edit"));
                private final Button deleteBtn = new Button(context.bundle().getString("btn.delete"));
                {
                    editBtn.setStyle("-fx-font-size: 10px; -fx-background-color: #3498db; -fx-text-fill: white;");
                    deleteBtn.setStyle("-fx-font-size: 10px; -fx-background-color: #e74c3c; -fx-text-fill: white;");
                    
                    editBtn.setOnAction(event -> {
                        Map<String, Object> rowData = getTableView().getItems().get(getIndex());
                        new FormView(context).showUpsertForm(title, resource, rowData);
                    });
                    
                    deleteBtn.setOnAction(event -> {
                        Map<String, Object> rowData = getTableView().getItems().get(getIndex());
                        showDeleteConfirmation(rowData, onRefresh);
                    });
                }
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        HBox box = new HBox(5, editBtn, deleteBtn);
                        box.setAlignment(Pos.CENTER);
                        setGraphic(box);
                    }
                }
            });
            table.getColumns().add(actionCol);
        }
        if (data != null) {
            table.setItems(FXCollections.observableArrayList(data));
        }
    }

    private void showDeleteConfirmation(Map<String, Object> rowData, Runnable onDeleted) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(context.bundle().getString("confirm.title"));
        alert.setHeaderText(context.bundle().getString("confirm.delete"));
        Object displayVal = UIUtils.extractDeepValue(rowData);
        alert.setContentText(title + ": " + (displayVal != null ? displayVal.toString() : "Unknown"));

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        context.apiClient().delete(resource, ((Number) rowData.get("id")).longValue());
                        Platform.runLater(onDeleted);
                    } catch (Exception ex) {
                        Platform.runLater(() -> UIUtils.showErrorPopup("Delete Error", "Could not delete record", ex));
                    }
                }).start();
            }
        });
    }
}
