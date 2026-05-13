package com.inventorymanager.frontend.ui.views;

import com.inventorymanager.frontend.ui.UIUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FormView {
    private final ViewContext context;

    public FormView(ViewContext context) {
        this.context = context;
    }

    public void showUpsertForm(String title, String resource, Map<String, Object> rowData) {
        switch (resource) {
            case "items" -> showItemUpsertForm(rowData);
            case "bags" -> showBagUpsertForm(rowData);
            case "displacements" -> showDisplacementUpsertForm(rowData, null);
            case "branches" -> showBranchUpsertForm(rowData);
            case "departments" -> showDepartmentUpsertForm(rowData);
            case "users" -> showUserUpsertForm(rowData);
            case "municipalities" -> showMunicipalityUpsertForm(rowData);
            case "parishes" -> showParishUpsertForm(rowData);
            case "states", "categories", "roles", "permissions" -> showNamedUpsertForm(title, resource, rowData);
            default -> {
                VBox root = new VBox(10);
                root.getChildren().addAll(new Label("🚧 Form for " + title), new Label("Development in progress."));
                context.viewSetter().accept(root);
            }
        }
    }

    private void showNamedUpsertForm(String title, String resource, Map<String, Object> rowData) {
        boolean isEdit = rowData != null;
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        Label t = new Label(isEdit ? context.bundle().getString("form.edit") + " " + title : context.bundle().getString("resource.add") + " " + title);
        t.setFont(Font.font("System", FontWeight.BOLD, 18));
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10);
        TextField nameField = new TextField(isEdit && rowData.get("name") != null ? rowData.get("name").toString() : "");
        grid.addRow(0, new Label(context.bundle().getString("form.name")), nameField);
        
        Button saveBtn = new Button(context.bundle().getString("form.save"));
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            try {
                Map<String, Object> payload = Map.of("name", nameField.getText());
                if (isEdit) {
                    context.apiClient().update(resource, ((Number)rowData.get("id")).longValue(), payload);
                } else {
                    context.apiClient().create(resource, payload);
                }
                new ResourceView(context, title, resource).show();
            } catch (Exception ex) { UIUtils.showErrorPopup("Save Error", "Failed", ex); }
        });
        root.getChildren().addAll(t, grid, saveBtn);
        context.viewSetter().accept(root);
    }

    private void showItemUpsertForm(Map<String, Object> rowData) {
        boolean isEdit = rowData != null;
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        Label t = new Label(isEdit ? context.bundle().getString("form.edit") : context.bundle().getString("resource.add"));
        t.setFont(Font.font("System", FontWeight.BOLD, 18));
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10);
        
        TextField nameField = new TextField(isEdit && rowData.get("name") != null ? rowData.get("name").toString() : "");
        TextField qtyField = new TextField(isEdit && rowData.get("quantity") != null ? rowData.get("quantity").toString() : "1");
        ComboBox<String> unitCombo = new ComboBox<>(FXCollections.observableArrayList("UND", "KG", "L", "M"));
        unitCombo.setValue(isEdit && rowData.get("unit") != null ? rowData.get("unit").toString() : "UND");
        
        ComboBox<UIUtils.IdName> branchCombo = new ComboBox<>();
        ComboBox<UIUtils.IdName> deptCombo = new ComboBox<>();
        
        grid.addRow(0, new Label(context.bundle().getString("form.name")), nameField);
        grid.addRow(1, new Label(context.bundle().getString("form.qty")), qtyField);
        grid.addRow(2, new Label(context.bundle().getString("form.unit")), unitCombo);
        grid.addRow(3, new Label(context.bundle().getString("form.branch")), branchCombo);
        grid.addRow(4, new Label(context.bundle().getString("form.dept")), deptCombo);

        Platform.runLater(() -> {
            try {
                ObservableList<UIUtils.IdName> branches = UIUtils.fetchIdNames(context.apiClient(), "branches");
                branchCombo.setItems(branches);
                if (isEdit && rowData.get("branch") instanceof Map) {
                    Long branchId = ((Number) ((Map<?,?>)rowData.get("branch")).get("id")).longValue();
                    branches.stream().filter(b -> b.id.equals(branchId)).findFirst().ifPresent(branchCombo::setValue);
                }
                branchCombo.setOnAction(e -> {
                   try {
                       if (branchCombo.getValue() != null) {
                           deptCombo.setItems(UIUtils.fetchIdNames(context.apiClient(), "departments?branchId=" + branchCombo.getValue().id));
                       }
                   } catch (Exception ignored) {}
                });

                if (isEdit && rowData.get("department") instanceof Map) {
                    Long deptId = ((Number) ((Map<?,?>)rowData.get("department")).get("id")).longValue();
                    ObservableList<UIUtils.IdName> depts = UIUtils.fetchIdNames(context.apiClient(), "departments?branchId=" + ((Number) ((Map<?,?>)rowData.get("branch")).get("id")).longValue());
                    deptCombo.setItems(depts);
                    depts.stream().filter(d -> d.id.equals(deptId)).findFirst().ifPresent(deptCombo::setValue);
                } else if (branchCombo.getValue() != null) {
                    deptCombo.setItems(UIUtils.fetchIdNames(context.apiClient(), "departments?branchId=" + branchCombo.getValue().id));
                }
            } catch (Exception ignored) {}
        });

        Button saveBtn = new Button(context.bundle().getString("form.save"));
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            try {
                Map<String, Object> payload = Map.of(
                    "name", nameField.getText(), 
                    "quantity", Integer.parseInt(qtyField.getText()),
                    "unit", unitCombo.getValue(), 
                    "branchId", branchCombo.getValue().id,
                    "departmentId", deptCombo.getValue().id
                );
                if (isEdit) {
                    context.apiClient().update("items", ((Number)rowData.get("id")).longValue(), payload);
                } else {
                    context.apiClient().create("items", payload);
                }
                new ResourceView(context, context.bundle().getString("nav.assets"), "items").show();
            } catch (Exception ex) { UIUtils.showErrorPopup("Save Error", "Failed", ex); }
        });
        root.getChildren().addAll(t, grid, saveBtn);
        context.viewSetter().accept(root);
    }

    private void showBranchUpsertForm(Map<String, Object> rowData) {
        boolean isEdit = rowData != null;
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        Label t = new Label(isEdit ? context.bundle().getString("form.edit") : context.bundle().getString("form.register") + " Branch");
        t.setFont(Font.font("System", FontWeight.BOLD, 18));
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10);
        
        TextField nameField = new TextField(isEdit && rowData.get("name") != null ? rowData.get("name").toString() : "");
        TextArea addrArea = new TextArea(isEdit && rowData.get("address") != null ? rowData.get("address").toString() : ""); 
        addrArea.setPrefRowCount(2);
        
        ComboBox<UIUtils.IdName> stateCombo = new ComboBox<>();
        ComboBox<UIUtils.IdName> muniCombo = new ComboBox<>();
        ComboBox<UIUtils.IdName> parishCombo = new ComboBox<>();
        
        grid.addRow(0, new Label(context.bundle().getString("form.name")), nameField);
        grid.addRow(1, new Label("Address:"), addrArea);
        grid.addRow(2, new Label("State:"), stateCombo);
        grid.addRow(3, new Label("Municipality:"), muniCombo);
        grid.addRow(4, new Label("Parish:"), parishCombo);

        Platform.runLater(() -> {
            try {
                ObservableList<UIUtils.IdName> states = UIUtils.fetchIdNames(context.apiClient(), "states");
                stateCombo.setItems(states);
                if (isEdit && rowData.get("state") instanceof Map) {
                    Long id = ((Number) ((Map<?,?>)rowData.get("state")).get("id")).longValue();
                    states.stream().filter(s -> s.id.equals(id)).findFirst().ifPresent(stateCombo::setValue);
                }
                stateCombo.setOnAction(e -> {
                   try { 
                       if (stateCombo.getValue() != null) {
                           muniCombo.setItems(UIUtils.fetchIdNames(context.apiClient(), "municipalities?stateId=" + stateCombo.getValue().id));
                       }
                   } catch (Exception ignored) {}
                });
                muniCombo.setOnAction(e -> {
                   try { 
                       if (muniCombo.getValue() != null) {
                           parishCombo.setItems(UIUtils.fetchIdNames(context.apiClient(), "parishes?municipalityId=" + muniCombo.getValue().id));
                       }
                   } catch (Exception ignored) {}
                });
                if (isEdit) {
                    Long sId = ((Number) ((Map<?,?>)rowData.get("state")).get("id")).longValue();
                    Long mId = ((Number) ((Map<?,?>)rowData.get("municipality")).get("id")).longValue();
                    
                    ObservableList<UIUtils.IdName> munis = UIUtils.fetchIdNames(context.apiClient(), "municipalities?stateId=" + sId);
                    muniCombo.setItems(munis);
                    munis.stream().filter(m -> m.id.equals(mId)).findFirst().ifPresent(muniCombo::setValue);
                    
                    ObservableList<UIUtils.IdName> parishes = UIUtils.fetchIdNames(context.apiClient(), "parishes?municipalityId=" + mId);
                    parishCombo.setItems(parishes);
                    if (rowData.get("parish") instanceof Map) {
                        Long pId = ((Number) ((Map<?,?>)rowData.get("parish")).get("id")).longValue();
                        parishes.stream().filter(p -> p.id.equals(pId)).findFirst().ifPresent(parishCombo::setValue);
                    }
                }
            } catch (Exception ignored) {}
        });

        Button saveBtn = new Button(context.bundle().getString("form.save"));
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            try {
                Map<String, Object> payload = Map.of(
                    "name", nameField.getText(), 
                    "address", addrArea.getText(),
                    "stateId", stateCombo.getValue().id, 
                    "municipalityId", muniCombo.getValue().id,
                    "parishId", parishCombo.getValue().id
                );
                if (isEdit) {
                    context.apiClient().update("branches", ((Number)rowData.get("id")).longValue(), payload);
                } else {
                    context.apiClient().create("branches", payload);
                }
                new ResourceView(context, context.bundle().getString("nav.branches"), "branches").show();
            } catch (Exception ex) { UIUtils.showErrorPopup("Save Error", "Failed", ex); }
        });
        root.getChildren().addAll(t, grid, saveBtn);
        context.viewSetter().accept(root);
    }

    private void showDepartmentUpsertForm(Map<String, Object> rowData) {
        boolean isEdit = rowData != null;
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        Label t = new Label(isEdit ? context.bundle().getString("form.edit") : context.bundle().getString("form.create") + " Department");
        t.setFont(Font.font("System", FontWeight.BOLD, 18));
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10);
        
        TextField nameField = new TextField(isEdit && rowData.get("name") != null ? rowData.get("name").toString() : "");
        ComboBox<UIUtils.IdName> branchCombo = new ComboBox<>();
        
        grid.addRow(0, new Label(context.bundle().getString("form.dept")), nameField);
        grid.addRow(1, new Label(context.bundle().getString("form.branch")), branchCombo);

        Platform.runLater(() -> {
            try { 
                ObservableList<UIUtils.IdName> branches = UIUtils.fetchIdNames(context.apiClient(), "branches");
                branchCombo.setItems(branches); 
                if (isEdit && rowData.get("branch") instanceof Map) {
                    Long id = ((Number) ((Map<?,?>)rowData.get("branch")).get("id")).longValue();
                    branches.stream().filter(b -> b.id.equals(id)).findFirst().ifPresent(branchCombo::setValue);
                }
            } catch (Exception ignored) {}
        });

        Button saveBtn = new Button(context.bundle().getString("form.save"));
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            try {
                Map<String, Object> payload = Map.of("name", nameField.getText(), "branchId", branchCombo.getValue().id);
                if (isEdit) {
                    context.apiClient().update("departments", ((Number)rowData.get("id")).longValue(), payload);
                } else {
                    context.apiClient().create("departments", payload);
                }
                new DepartmentView(context).show();
            } catch (Exception ex) { UIUtils.showErrorPopup("Save Error", "Failed", ex); }
        });
        root.getChildren().addAll(t, grid, saveBtn);
        context.viewSetter().accept(root);
    }

    private void showUserUpsertForm(Map<String, Object> rowData) {
        boolean isEdit = rowData != null;
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        Label t = new Label(isEdit ? context.bundle().getString("form.edit") : context.bundle().getString("form.register") + " User");
        t.setFont(Font.font("System", FontWeight.BOLD, 18));
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10);
        
        TextField userField = new TextField(isEdit && rowData.get("username") != null ? rowData.get("username").toString() : "");
        PasswordField passField = new PasswordField();
        if (!isEdit) passField.setPromptText("Required");
        else passField.setPromptText("Leave blank to keep same password");
        
        ComboBox<UIUtils.IdName> branchCombo = new ComboBox<>();
        ListView<UIUtils.IdName> rolesList = new ListView<>();
        rolesList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        rolesList.setPrefHeight(100);

        grid.addRow(0, new Label(context.bundle().getString("form.user")), userField);
        grid.addRow(1, new Label(context.bundle().getString("form.pass")), passField);
        grid.addRow(2, new Label(context.bundle().getString("form.branch")), branchCombo);
        grid.addRow(3, new Label(context.bundle().getString("form.roles")), rolesList);

        Platform.runLater(() -> {
            try { 
                ObservableList<UIUtils.IdName> branches = UIUtils.fetchIdNames(context.apiClient(), "branches");
                branchCombo.setItems(branches); 
                if (isEdit && rowData.get("branch") instanceof Map) {
                    Long id = ((Number) ((Map<?,?>)rowData.get("branch")).get("id")).longValue();
                    branches.stream().filter(b -> b.id.equals(id)).findFirst().ifPresent(branchCombo::setValue);
                }
                
                ObservableList<UIUtils.IdName> roles = UIUtils.fetchIdNames(context.apiClient(), "roles");
                rolesList.setItems(roles);
                if (isEdit && rowData.get("roles") instanceof List) {
                    List<?> userRoles = (List<?>) rowData.get("roles");
                    for (Object roleObj : userRoles) {
                        if (roleObj instanceof Map) {
                            Long roleId = ((Number) ((Map<?,?>)roleObj).get("id")).longValue();
                            roles.stream().filter(r -> r.id.equals(roleId)).findFirst().ifPresent(rolesList.getSelectionModel()::select);
                        }
                    }
                }
            } catch (Exception ignored) {}
        });

        Button saveBtn = new Button(context.bundle().getString("form.save"));
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            try {
                List<Long> roleIds = rolesList.getSelectionModel().getSelectedItems().stream().map(r -> r.id).collect(Collectors.toList());
                Map<String, Object> payload = new HashMap<>();
                payload.put("username", userField.getText());
                if (!passField.getText().isBlank()) payload.put("password", passField.getText());
                if (branchCombo.getValue() != null) payload.put("branchId", branchCombo.getValue().id);
                payload.put("roleIds", roleIds);

                if (isEdit) {
                    context.apiClient().update("users", ((Number)rowData.get("id")).longValue(), payload);
                } else {
                    context.apiClient().create("users", payload);
                }
                new ResourceView(context, context.bundle().getString("nav.users"), "users").show();
            } catch (Exception ex) { UIUtils.showErrorPopup("Save Error", "Failed", ex); }
        });
        root.getChildren().addAll(t, grid, saveBtn);
        context.viewSetter().accept(root);
    }

    private void showMunicipalityUpsertForm(Map<String, Object> rowData) {
        boolean isEdit = rowData != null;
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        Label t = new Label(isEdit ? context.bundle().getString("form.edit") : context.bundle().getString("resource.add") + " Municipality");
        t.setFont(Font.font("System", FontWeight.BOLD, 18));
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10);
        
        TextField nameField = new TextField(isEdit && rowData.get("name") != null ? rowData.get("name").toString() : "");
        ComboBox<UIUtils.IdName> stateCombo = new ComboBox<>();
        
        grid.addRow(0, new Label(context.bundle().getString("form.name")), nameField);
        grid.addRow(1, new Label("State:"), stateCombo);

        Platform.runLater(() -> {
            try { 
                ObservableList<UIUtils.IdName> states = UIUtils.fetchIdNames(context.apiClient(), "states");
                stateCombo.setItems(states); 
                if (isEdit && rowData.get("state") instanceof Map) {
                    Long id = ((Number) ((Map<?,?>)rowData.get("state")).get("id")).longValue();
                    states.stream().filter(s -> s.id.equals(id)).findFirst().ifPresent(stateCombo::setValue);
                }
            } catch (Exception ignored) {}
        });

        Button saveBtn = new Button(context.bundle().getString("form.save"));
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            try {
                Map<String, Object> payload = Map.of("name", nameField.getText(), "stateId", stateCombo.getValue().id);
                if (isEdit) {
                    context.apiClient().update("municipalities", ((Number)rowData.get("id")).longValue(), payload);
                } else {
                    context.apiClient().create("municipalities", payload);
                }
                new ResourceView(context, "Municipalities", "municipalities").show();
            } catch (Exception ex) { UIUtils.showErrorPopup("Save Error", "Failed", ex); }
        });
        root.getChildren().addAll(t, grid, saveBtn);
        context.viewSetter().accept(root);
    }

    private void showParishUpsertForm(Map<String, Object> rowData) {
        boolean isEdit = rowData != null;
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        Label t = new Label(isEdit ? context.bundle().getString("form.edit") : context.bundle().getString("resource.add") + " Parish");
        t.setFont(Font.font("System", FontWeight.BOLD, 18));
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10);
        
        TextField nameField = new TextField(isEdit && rowData.get("name") != null ? rowData.get("name").toString() : "");
        ComboBox<UIUtils.IdName> muniCombo = new ComboBox<>();
        
        grid.addRow(0, new Label(context.bundle().getString("form.name")), nameField);
        grid.addRow(1, new Label("Municipality:"), muniCombo);

        Platform.runLater(() -> {
            try { 
                ObservableList<UIUtils.IdName> munis = UIUtils.fetchIdNames(context.apiClient(), "municipalities");
                muniCombo.setItems(munis); 
                if (isEdit && rowData.get("municipality") instanceof Map) {
                    Long id = ((Number) ((Map<?,?>)rowData.get("municipality")).get("id")).longValue();
                    munis.stream().filter(m -> m.id.equals(id)).findFirst().ifPresent(muniCombo::setValue);
                }
            } catch (Exception ignored) {}
        });

        Button saveBtn = new Button(context.bundle().getString("form.save"));
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            try {
                Map<String, Object> payload = Map.of("name", nameField.getText(), "municipalityId", muniCombo.getValue().id);
                if (isEdit) {
                    context.apiClient().update("parishes", ((Number)rowData.get("id")).longValue(), payload);
                } else {
                    context.apiClient().create("parishes", payload);
                }
                new ResourceView(context, "Parishes", "parishes").show();
            } catch (Exception ex) { UIUtils.showErrorPopup("Save Error", "Failed", ex); }
        });
        root.getChildren().addAll(t, grid, saveBtn);
        context.viewSetter().accept(root);
    }

    private void showBagUpsertForm(Map<String, Object> rowData) {
        boolean isEdit = rowData != null;
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        Label t = new Label(isEdit ? context.bundle().getString("form.edit") : context.bundle().getString("form.create") + " Bag");
        t.setFont(Font.font("System", FontWeight.BOLD, 18));
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10);
        
        TextField nameField = new TextField(isEdit && rowData.get("name") != null ? rowData.get("name").toString() : "");
        TextField barcodeField = new TextField(isEdit && rowData.get("barcode") != null ? rowData.get("barcode").toString() : "");
        ComboBox<UIUtils.IdName> branchCombo = new ComboBox<>();
        ComboBox<UIUtils.IdName> deptCombo = new ComboBox<>();
        
        grid.addRow(0, new Label(context.bundle().getString("form.name")), nameField);
        grid.addRow(1, new Label(context.bundle().getString("form.barcode")), barcodeField);
        grid.addRow(2, new Label(context.bundle().getString("form.branch")), branchCombo);
        grid.addRow(3, new Label("Dept:"), deptCombo);

        Platform.runLater(() -> {
            try {
                ObservableList<UIUtils.IdName> branches = UIUtils.fetchIdNames(context.apiClient(), "branches");
                branchCombo.setItems(branches);
                if (isEdit && rowData.get("branch") instanceof Map) {
                    Long id = ((Number) ((Map<?,?>)rowData.get("branch")).get("id")).longValue();
                    branches.stream().filter(b -> b.id.equals(id)).findFirst().ifPresent(branchCombo::setValue);
                }
                branchCombo.setOnAction(e -> {
                   try {
                       if (branchCombo.getValue() != null) {
                           deptCombo.setItems(UIUtils.fetchIdNames(context.apiClient(), "departments?branchId=" + branchCombo.getValue().id));
                       }
                   } catch (Exception ignored) {}
                });
                
                if (isEdit && rowData.get("assignedDepartment") instanceof Map) {
                    Long id = ((Number) ((Map<?,?>)rowData.get("assignedDepartment")).get("id")).longValue();
                    ObservableList<UIUtils.IdName> depts = UIUtils.fetchIdNames(context.apiClient(), "departments?branchId=" + ((Number) ((Map<?,?>)rowData.get("branch")).get("id")).longValue());
                    deptCombo.setItems(depts);
                    depts.stream().filter(d -> d.id.equals(id)).findFirst().ifPresent(deptCombo::setValue);
                } else if (branchCombo.getValue() != null) {
                    deptCombo.setItems(UIUtils.fetchIdNames(context.apiClient(), "departments?branchId=" + branchCombo.getValue().id));
                }
            } catch (Exception ignored) {}
        });

        Button saveBtn = new Button(context.bundle().getString("form.save"));
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            try {
                Map<String, Object> payload = Map.of(
                    "name", nameField.getText(), 
                    "barcode", barcodeField.getText(),
                    "branchId", branchCombo.getValue().id, 
                    "assignedDepartmentId", deptCombo.getValue().id,
                    "expectedItems", List.of()
                );
                if (isEdit) {
                    context.apiClient().update("bags", ((Number)rowData.get("id")).longValue(), payload);
                } else {
                    context.apiClient().create("bags", payload);
                }
                new ResourceView(context, context.bundle().getString("nav.bags"), "bags").show();
            } catch (Exception ex) { UIUtils.showErrorPopup("Save Error", "Failed", ex); }
        });
        root.getChildren().addAll(t, grid, saveBtn);
        context.viewSetter().accept(root);
    }

    public void showDisplacementUpsertForm(Map<String, Object> rowData, Map<String, Object> prefill) {
        boolean isEdit = rowData != null;
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        Label t = new Label(isEdit ? context.bundle().getString("form.edit") : context.bundle().getString("nav.displacements"));
        t.setFont(Font.font("System", FontWeight.BOLD, 18));
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10);
        ComboBox<UIUtils.IdName> itemCombo = new ComboBox<>();
        
        String initialBorrower = isEdit && rowData.get("borrowerName") != null ? rowData.get("borrowerName").toString() : "";
        if (!isEdit && prefill != null && prefill.containsKey("borrowerName")) initialBorrower = prefill.get("borrowerName").toString();
        TextField borrowerField = new TextField(initialBorrower);
        
        String initialReason = isEdit && rowData.get("reason") != null ? rowData.get("reason").toString() : "";
        if (!isEdit && prefill != null && prefill.containsKey("reason")) initialReason = prefill.get("reason").toString();
        TextArea reasonArea = new TextArea(initialReason); 
        reasonArea.setPrefRowCount(3);
        
        DatePicker datePicker = new DatePicker(java.time.LocalDate.now().plusDays(7));
        grid.addRow(0, new Label("Item:"), itemCombo);
        grid.addRow(1, new Label(context.bundle().getString("form.borrower")), borrowerField);
        grid.addRow(2, new Label(context.bundle().getString("form.reason")), reasonArea);
        grid.addRow(3, new Label(context.bundle().getString("form.return")), datePicker);

        Platform.runLater(() -> {
            try { 
                ObservableList<UIUtils.IdName> items = UIUtils.fetchIdNames(context.apiClient(), "items");
                itemCombo.setItems(items); 
                if (isEdit && rowData.get("item") instanceof Map) {
                    Long id = ((Number) ((Map<?,?>)rowData.get("item")).get("id")).longValue();
                    items.stream().filter(i -> i.id.equals(id)).findFirst().ifPresent(itemCombo::setValue);
                } else if (!isEdit && prefill != null && prefill.containsKey("itemId")) {
                    Long id = ((Number) prefill.get("itemId")).longValue();
                    items.stream().filter(i -> i.id.equals(id)).findFirst().ifPresent(itemCombo::setValue);
                }
            } catch (Exception ignored) {}
        });
        
        Button saveBtn = new Button(context.bundle().getString("form.save"));
        saveBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            try {
                Map<String, Object> payload = Map.of(
                    "itemId", itemCombo.getValue().id, 
                    "borrowerName", borrowerField.getText(),
                    "reason", reasonArea.getText(),
                    "expectedReturnDate", datePicker.getValue().atStartOfDay().atOffset(java.time.ZoneOffset.UTC).toString()
                );
                if (isEdit) {
                    context.apiClient().update("displacements", ((Number)rowData.get("id")).longValue(), payload);
                } else {
                    context.apiClient().create("displacements", payload);
                }
                new ResourceView(context, context.bundle().getString("nav.displacements"), "displacements").show();
            } catch (Exception ex) { UIUtils.showErrorPopup("Save Error", "Failed", ex); }
        });
        root.getChildren().addAll(t, grid, saveBtn);
        context.viewSetter().accept(root);
    }
}
