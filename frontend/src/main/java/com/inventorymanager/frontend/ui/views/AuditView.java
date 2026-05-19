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
            String type = entityType.getValue();
            String idStr = entityId.getText();
            if (type == null) {
                UIUtils.showWarningPopup("Input Error", "Please select type");
                return;
            }
            fetchBtn.setDisable(true);
            new Thread(() -> {
                try {
                    String path = "audit-logs/" + type;
                    if (idStr != null && !idStr.isBlank()) path += "/" + idStr;
                    
                    Map<String, Object> response = context.apiClient().get(path);
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
                    
                    Platform.runLater(() -> {
                        updateTable(table, data);
                        fetchBtn.setDisable(false);
                    });
                } catch (Exception ex) { 
                    Platform.runLater(() -> {
                        UIUtils.showErrorPopup(context.bundle().getString("audit.error"), "Failed to fetch logs", ex); 
                        fetchBtn.setDisable(false);
                    });
                }
            }).start();
        });
        
        root.getChildren().addAll(title, controls, table);
        context.viewSetter().accept(root);
    }

    private void updateTable(TableView<Map<String, Object>> table, List<Map<String, Object>> data) {
        table.getColumns().clear();
        if (data == null || data.isEmpty()) return;

        TableColumn<Map<String, Object>, String> opCol = new TableColumn<>("OP");
        opCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(String.valueOf(d.getValue().get("operation"))));
        opCol.setPrefWidth(80);

        TableColumn<Map<String, Object>, String> userCol = new TableColumn<>("USER");
        userCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(String.valueOf(d.getValue().get("changedByUsername"))));
        userCol.setPrefWidth(120);

        TableColumn<Map<String, Object>, String> dateCol = new TableColumn<>("DATE");
        dateCol.setCellValueFactory(d -> {
            Object val = d.getValue().get("changedAt");
            if (val != null) {
                try {
                    java.time.Instant instant = java.time.Instant.parse(val.toString());
                    java.time.LocalDateTime ldt = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
                    return new javafx.beans.property.SimpleStringProperty(ldt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                } catch (Exception e) { return new javafx.beans.property.SimpleStringProperty(val.toString()); }
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });
        dateCol.setPrefWidth(150);

        TableColumn<Map<String, Object>, Void> diffCol = new TableColumn<>("CHANGES");
        diffCol.setCellFactory(tc -> new TableCell<>() {
            private final VBox box = new VBox(2);
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); }
                else {
                    box.getChildren().clear();
                    Map<String, Object> row = getTableView().getItems().get(getIndex());
                    Map<?, ?> current = (Map<?, ?>) row.get("state");
                    Map<?, ?> previous = (Map<?, ?>) row.get("previousState");
                    
                    if (current != null) {
                        java.util.Set<Object> allKeys = new java.util.HashSet<>(current.keySet());
                        if (previous != null) allKeys.addAll(previous.keySet());
                        
                        for (Object key : allKeys) {
                            Object curVal = UIUtils.extractDeepValue(current.get(key));
                            Object preVal = previous != null ? UIUtils.extractDeepValue(previous.get(key)) : null;
                            
                            if ((preVal == null && curVal != null) || (preVal != null && curVal == null) || (preVal != null && !preVal.toString().equals(curVal.toString()))) {
                                Label l = new Label(key + ": " + (preVal != null && !preVal.toString().isBlank() ? preVal : "NONE") + " ➡️ " + (curVal != null && !curVal.toString().isBlank() ? curVal : "DELETED"));
                                if (preVal != null && curVal != null) l.setStyle("-fx-text-fill: #e67e22; -fx-font-size: 11px;");
                                else if (curVal == null) l.setStyle("-fx-text-fill: #c0392b; -fx-font-size: 11px;");
                                else l.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 11px;");
                                box.getChildren().add(l);
                            }
                        }
                    }
                    setGraphic(box);
                }
            }
        });

        table.getColumns().addAll(opCol, userCol, dateCol, diffCol);
        table.setItems(FXCollections.observableArrayList(data));
    }
}
