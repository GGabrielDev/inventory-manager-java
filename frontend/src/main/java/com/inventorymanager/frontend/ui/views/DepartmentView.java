package com.inventorymanager.frontend.ui.views;

import com.inventorymanager.frontend.api.ApiClient;
import com.inventorymanager.frontend.ui.UIUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DepartmentView {
    private final ViewContext context;
    private ComboBox<UIUtils.IdName> branchSelector;
    private TableView<Map<String, Object>> table;
    private final Map<Long, String> itemCounts = new HashMap<>();

    public DepartmentView(ViewContext context) {
        this.context = context;
    }

    public void show() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(10));

        Label title = new Label(context.bundle().getString("nav.departments"));
        title.setFont(Font.font("System", FontWeight.BOLD, 20));

        HBox selectorBar = new HBox(10);
        selectorBar.setAlignment(Pos.CENTER_LEFT);
        selectorBar.setPadding(new Insets(10));
        selectorBar.setStyle("-fx-background-color: #f8f9fa; -fx-border-radius: 5;");

        branchSelector = new ComboBox<>();
        branchSelector.setPromptText("Select Branch...");
        branchSelector.setPrefWidth(250);

        Button loadBtn = new Button(context.bundle().getString("resource.apply"));
        loadBtn.setOnAction(e -> loadDepartments());

        selectorBar.getChildren().addAll(new Label(context.bundle().getString("form.branch")), branchSelector, loadBtn);

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        root.getChildren().addAll(title, selectorBar, table);
        context.viewSetter().accept(root);

        // Fetch branches asynchronously
        Platform.runLater(() -> {
            try {
                branchSelector.setItems(UIUtils.fetchIdNames(context.apiClient(), "branches"));
            } catch (Exception ex) {
                UIUtils.showErrorPopup("Fetch Error", "Could not load branches", ex);
            }
        });
    }

    private void loadDepartments() {
        UIUtils.IdName selectedBranch = branchSelector.getValue();
        if (selectedBranch == null) {
            UIUtils.showWarningPopup("Input Required", "Please select a branch first.");
            return;
        }

        try {
            List<Map<String, Object>> depts = context.apiClient().list("departments?branchId=" + selectedBranch.id);
            itemCounts.clear();
            updateTable(depts);
            fetchItemCounts(depts);
        } catch (Exception ex) {
            UIUtils.showErrorPopup("Fetch Error", "Could not load departments", ex);
        }
    }

    private void fetchItemCounts(List<Map<String, Object>> depts) {
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                for (Map<String, Object> dept : depts) {
                    try {
                        Long deptId = ((Number) dept.get("id")).longValue();
                        Map<String, Object> response = context.apiClient().get("items?departmentId=" + deptId + "&pageSize=1");
                        Object total = response.get("total");
                        itemCounts.put(deptId, total != null ? total.toString() : "0");
                        Platform.runLater(() -> table.refresh()); // Refresh UI per item found
                    } catch (Exception ignored) {}
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    private void updateTable(List<Map<String, Object>> data) {
        table.getColumns().clear();
        if (data.isEmpty()) {
            table.setPlaceholder(new Label("No departments found for this branch."));
            return;
        }

        TableColumn<Map<String, Object>, String> nameCol = new TableColumn<>("NAME");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("name").toString()));

        TableColumn<Map<String, Object>, String> countCol = new TableColumn<>("ITEM COUNT");
        countCol.setCellValueFactory(d -> {
            Long deptId = ((Number) d.getValue().get("id")).longValue();
            return new SimpleStringProperty(itemCounts.getOrDefault(deptId, "..."));
        });

        table.getColumns().addAll(nameCol, countCol);
        table.setItems(FXCollections.observableArrayList(data));
    }
}
