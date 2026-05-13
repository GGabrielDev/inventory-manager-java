package com.inventorymanager.frontend.ui.views;

import com.inventorymanager.frontend.ui.UIUtils;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;
import java.util.Map;

public class AuditView {
    private final ViewContext context;

    public AuditView(ViewContext context) {
        this.context = context;
    }

    public void show() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(10));
        
        Label title = new Label(context.bundle().getString("audit.title"));
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        
        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);
        
        ComboBox<String> entityType = new ComboBox<>(FXCollections.observableArrayList(
            "item", "branch", "user", "bag", "item_request", "displacement", "department", "category", "role", "state", "municipality", "parish"
        ));
        entityType.setPromptText(context.bundle().getString("audit.type"));
        
        TextField entityId = new TextField();
        entityId.setPromptText(context.bundle().getString("audit.id") + " (Optional)");
        
        Button fetchBtn = new Button(context.bundle().getString("audit.fetch"));
        controls.getChildren().addAll(new Label(context.bundle().getString("audit.query")), entityType, entityId, fetchBtn);
        
        TableView<Map<String, Object>> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        fetchBtn.setOnAction(e -> {
            if (entityType.getValue() == null) {
                UIUtils.showWarningPopup("Input Error", "Please select type");
                return;
            }
            try {
                String path = "audit-logs/" + entityType.getValue();
                if (entityId.getText() != null && !entityId.getText().isBlank()) path += "/" + entityId.getText();
                
                Map<String, Object> response = context.apiClient().get(path);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
                
                updateTable(table, data);
            } catch (Exception ex) { 
                UIUtils.showErrorPopup(context.bundle().getString("audit.error"), "Failed to fetch logs", ex); 
            }
        });
        
        root.getChildren().addAll(title, controls, table);
        context.viewSetter().accept(root);
    }

    private void updateTable(TableView<Map<String, Object>> table, List<Map<String, Object>> data) {
        table.getColumns().clear();
        if (data == null || data.isEmpty()) return;

        java.util.Set<String> allKeys = new java.util.LinkedHashSet<>();
        for (Map<String, Object> row : data) {
            allKeys.addAll(row.keySet());
        }

        for (String key : allKeys) {
            String headerText = context.bundle().containsKey("col." + key) ? context.bundle().getString("col." + key) : key.toUpperCase();
            TableColumn<Map<String, Object>, String> col = new TableColumn<>(headerText);
            
            if (key.equalsIgnoreCase("state")) {
                col.setCellFactory(tc -> new TableCell<>() {
                    @Override protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) { setGraphic(null); }
                        else {
                            Map<String, Object> row = getTableView().getItems().get(getIndex());
                            Object stateObj = row.get("state");
                            Label label = new Label(stateObj != null ? stateObj.toString() : "");
                            label.setWrapText(true);
                            label.setMaxWidth(400);
                            setGraphic(label);
                        }
                    }
                });
            } else {
                col.setCellValueFactory(cellData -> {
                    Object val = UIUtils.extractDeepValue(cellData.getValue().get(key));
                    return new javafx.beans.property.SimpleStringProperty(val == null ? "" : val.toString());
                });
            }
            table.getColumns().add(col);
        }
        table.setItems(FXCollections.observableArrayList(data));
    }
}
