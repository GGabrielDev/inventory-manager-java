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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class FormView {
    private final ViewContext context;
    private final AtomicInteger comboRequestId = new AtomicInteger(0);

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
            saveBtn.setDisable(true);
            String name = nameField.getText(); if (name.isBlank()) { UIUtils.showWarningPopup("Validation Error", "Name is required"); saveBtn.setDisable(false); return; }
            new Thread(() -> {
                try {
                    Map<String, Object> payload = Map.of("name", name);
                    if (isEdit) {
                        context.apiClient().update(resource, ((Number)rowData.get("id")).longValue(), payload);
                    } else {
                        context.apiClient().create(resource, payload);
                    }
                    Platform.runLater(() -> new ResourceView(context, title, resource).show());
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        saveBtn.setDisable(false);
                        UIUtils.showErrorPopup("Save Error", "Failed", ex);
                    });
                }
            }).start();
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

        int initialReqId = comboRequestId.incrementAndGet();
        new Thread(() -> {
            try {
                ObservableList<UIUtils.IdName> branches = UIUtils.fetchIdNames(context.apiClient(), "branches");
                Platform.runLater(() -> {
                    if (initialReqId == comboRequestId.get()) {
                        branchCombo.setItems(branches);
                        if (isEdit && rowData.get("branch") instanceof Map) {
                            Long branchId = ((Number) ((Map<?,?>)rowData.get("branch")).get("id")).longValue();
                            branches.stream().filter(b -> b.id.equals(branchId)).findFirst().ifPresent(branchCombo::setValue);
                        }
                    }
                });

                branchCombo.setOnAction(e -> {
                    UIUtils.IdName selectedBranch = branchCombo.getValue();
                    if (selectedBranch != null) {
                        int reqId = comboRequestId.incrementAndGet();
                        new Thread(() -> {
                            try {
                                var depts = UIUtils.fetchIdNames(context.apiClient(), "departments?branchId=" + selectedBranch.id);
                                Platform.runLater(() -> {
                                    if (reqId == comboRequestId.get()) {
                                        deptCombo.setItems(depts);
                                    }
                                });
                            } catch (Exception ex) { Platform.runLater(() -> UIUtils.showErrorPopup("Loader Error", "Failed to load relationship data", ex)); }
                        }).start();
                    }
                });

                if (isEdit && rowData.get("department") instanceof Map) {
                    Long branchId = ((Number) ((Map<?,?>)rowData.get("branch")).get("id")).longValue();
                    Long deptId = ((Number) ((Map<?,?>)rowData.get("department")).get("id")).longValue();
                    ObservableList<UIUtils.IdName> depts = UIUtils.fetchIdNames(context.apiClient(), "departments?branchId=" + branchId);
                    Platform.runLater(() -> {
                        if (initialReqId == comboRequestId.get()) {
                            deptCombo.setItems(depts);
                            depts.stream().filter(d -> d.id.equals(deptId)).findFirst().ifPresent(deptCombo::setValue);
                        }
                    });
                }
            } catch (Exception ex) { Platform.runLater(() -> UIUtils.showErrorPopup("Loader Error", "Failed to load relationship data", ex)); }
        }).start();

        Button saveBtn = new Button(context.bundle().getString("form.save"));
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            saveBtn.setDisable(true);
            String name = nameField.getText(); if (name.isBlank()) { UIUtils.showWarningPopup("Validation Error", "Name is required"); saveBtn.setDisable(false); return; }
            String qty = qtyField.getText();
            String unit = unitCombo.getValue();
            UIUtils.IdName branch = branchCombo.getValue(); if (branch == null) { UIUtils.showWarningPopup("Validation Error", "Branch is required"); saveBtn.setDisable(false); return; }
            UIUtils.IdName dept = deptCombo.getValue(); if (dept == null) { UIUtils.showWarningPopup("Validation Error", "Department is required"); saveBtn.setDisable(false); return; }
            
            new Thread(() -> {
                try {
                    Map<String, Object> payload = Map.of(
                        "name", name, 
                        "quantity", Integer.parseInt(qty),
                        "unit", unit, 
                        "branchId", branch.id,
                        "departmentId", dept.id
                    );
                    if (isEdit) {
                        context.apiClient().update("items", ((Number)rowData.get("id")).longValue(), payload);
                    } else {
                        context.apiClient().create("items", payload);
                    }
                    Platform.runLater(() -> new ResourceView(context, context.bundle().getString("nav.assets"), "items").show());
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        saveBtn.setDisable(false);
                        UIUtils.showErrorPopup("Save Error", "Failed", ex);
                    });
                }
            }).start();
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

        int initialReqId = comboRequestId.incrementAndGet();
        new Thread(() -> {
            try {
                ObservableList<UIUtils.IdName> states = UIUtils.fetchIdNames(context.apiClient(), "states");
                Platform.runLater(() -> {
                    if (initialReqId == comboRequestId.get()) {
                        stateCombo.setItems(states);
                        if (isEdit && rowData.get("state") instanceof Map) {
                            Long id = ((Number) ((Map<?,?>)rowData.get("state")).get("id")).longValue();
                            states.stream().filter(s -> s.id.equals(id)).findFirst().ifPresent(stateCombo::setValue);
                        }
                    }
                });

                stateCombo.setOnAction(e -> {
                   UIUtils.IdName selected = stateCombo.getValue();
                   if (selected != null) {
                       int reqId = comboRequestId.incrementAndGet();
                       new Thread(() -> {
                           try {
                               var munis = UIUtils.fetchIdNames(context.apiClient(), "municipalities?stateId=" + selected.id);
                               Platform.runLater(() -> {
                                   if (reqId == comboRequestId.get()) {
                                       muniCombo.setItems(munis);
                                   }
                               });
                           } catch (Exception ex) { Platform.runLater(() -> UIUtils.showErrorPopup("Loader Error", "Failed to load relationship data", ex)); }
                       }).start();
                   }
                });

                muniCombo.setOnAction(e -> {
                   UIUtils.IdName selected = muniCombo.getValue();
                   if (selected != null) {
                       int reqId = comboRequestId.incrementAndGet();
                       new Thread(() -> {
                           try {
                               var parishes = UIUtils.fetchIdNames(context.apiClient(), "parishes?municipalityId=" + selected.id);
                               Platform.runLater(() -> {
                                   if (reqId == comboRequestId.get()) {
                                       parishCombo.setItems(parishes);
                                   }
                               });
                           } catch (Exception ex) { Platform.runLater(() -> UIUtils.showErrorPopup("Loader Error", "Failed to load relationship data", ex)); }
                       }).start();
                   }
                });

                if (isEdit) {
                    Long sId = ((Number) ((Map<?,?>)rowData.get("state")).get("id")).longValue();
                    Long mId = ((Number) ((Map<?,?>)rowData.get("municipality")).get("id")).longValue();
                    
                    ObservableList<UIUtils.IdName> munis = UIUtils.fetchIdNames(context.apiClient(), "municipalities?stateId=" + sId);
                    ObservableList<UIUtils.IdName> parishes = UIUtils.fetchIdNames(context.apiClient(), "parishes?municipalityId=" + mId);
                    
                    Platform.runLater(() -> {
                        if (initialReqId == comboRequestId.get()) {
                            muniCombo.setItems(munis);
                            munis.stream().filter(m -> m.id.equals(mId)).findFirst().ifPresent(muniCombo::setValue);
                            
                            parishCombo.setItems(parishes);
                            if (rowData.get("parish") instanceof Map) {
                                Long pId = ((Number) ((Map<?,?>)rowData.get("parish")).get("id")).longValue();
                                parishes.stream().filter(p -> p.id.equals(pId)).findFirst().ifPresent(parishCombo::setValue);
                            }
                        }
                    });
                }
            } catch (Exception ex) { Platform.runLater(() -> UIUtils.showErrorPopup("Loader Error", "Failed to load relationship data", ex)); }
        }).start();

        Button saveBtn = new Button(context.bundle().getString("form.save"));
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            saveBtn.setDisable(true);
            String name = nameField.getText(); if (name.isBlank()) { UIUtils.showWarningPopup("Validation Error", "Name is required"); saveBtn.setDisable(false); return; }
            String addr = addrArea.getText();
            UIUtils.IdName s = stateCombo.getValue();
            UIUtils.IdName m = muniCombo.getValue();
            UIUtils.IdName p = parishCombo.getValue();
            new Thread(() -> {
                try {
                    Map<String, Object> payload = Map.of(
                        "name", name, 
                        "address", addr,
                        "stateId", s.id, 
                        "municipalityId", m.id,
                        "parishId", p.id
                    );
                    if (isEdit) {
                        context.apiClient().update("branches", ((Number)rowData.get("id")).longValue(), payload);
                    } else {
                        context.apiClient().create("branches", payload);
                    }
                    Platform.runLater(() -> new ResourceView(context, context.bundle().getString("nav.branches"), "branches").show());
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        saveBtn.setDisable(false);
                        UIUtils.showErrorPopup("Save Error", "Failed", ex);
                    });
                }
            }).start();
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

        int initialReqId = comboRequestId.incrementAndGet();
        new Thread(() -> {
            try { 
                ObservableList<UIUtils.IdName> branches = UIUtils.fetchIdNames(context.apiClient(), "branches");
                Platform.runLater(() -> {
                    if (initialReqId == comboRequestId.get()) {
                        branchCombo.setItems(branches); 
                        if (isEdit && rowData.get("branch") instanceof Map) {
                            Long id = ((Number) ((Map<?,?>)rowData.get("branch")).get("id")).longValue();
                            branches.stream().filter(b -> b.id.equals(id)).findFirst().ifPresent(branchCombo::setValue);
                        }
                    }
                });
            } catch (Exception ex) { Platform.runLater(() -> UIUtils.showErrorPopup("Loader Error", "Failed to load relationship data", ex)); }
        }).start();

        Button saveBtn = new Button(context.bundle().getString("form.save"));
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            saveBtn.setDisable(true);
            String name = nameField.getText(); if (name.isBlank()) { UIUtils.showWarningPopup("Validation Error", "Name is required"); saveBtn.setDisable(false); return; }
            UIUtils.IdName branch = branchCombo.getValue(); if (branch == null) { UIUtils.showWarningPopup("Validation Error", "Branch is required"); saveBtn.setDisable(false); return; }
            new Thread(() -> {
                try {
                    Map<String, Object> payload = Map.of("name", name, "branchId", branch.id);
                    if (isEdit) {
                        context.apiClient().update("departments", ((Number)rowData.get("id")).longValue(), payload);
                    } else {
                        context.apiClient().create("departments", payload);
                    }
                    Platform.runLater(() -> new DepartmentView(context).show());
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        saveBtn.setDisable(false);
                        UIUtils.showErrorPopup("Save Error", "Failed", ex);
                    });
                }
            }).start();
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

        int initialReqId = comboRequestId.incrementAndGet();
        new Thread(() -> {
            try { 
                ObservableList<UIUtils.IdName> branches = UIUtils.fetchIdNames(context.apiClient(), "branches");
                ObservableList<UIUtils.IdName> roles = UIUtils.fetchIdNames(context.apiClient(), "roles");
                
                Platform.runLater(() -> {
                    if (initialReqId == comboRequestId.get()) {
                        branchCombo.setItems(branches); 
                        if (isEdit && rowData.get("branch") instanceof Map) {
                            Long id = ((Number) ((Map<?,?>)rowData.get("branch")).get("id")).longValue();
                            branches.stream().filter(b -> b.id.equals(id)).findFirst().ifPresent(branchCombo::setValue);
                        }
                        
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
                    }
                });
            } catch (Exception ex) { Platform.runLater(() -> UIUtils.showErrorPopup("Loader Error", "Failed to load relationship data", ex)); }
        }).start();

        Button saveBtn = new Button(context.bundle().getString("form.save"));
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            saveBtn.setDisable(true);
            String username = userField.getText(); if (username.isBlank()) { UIUtils.showWarningPopup("Validation Error", "Username is required"); saveBtn.setDisable(false); return; }
            String password = passField.getText();
            UIUtils.IdName branch = branchCombo.getValue(); if (branch == null) { UIUtils.showWarningPopup("Validation Error", "Branch is required"); saveBtn.setDisable(false); return; }
            List<Long> roleIds = rolesList.getSelectionModel().getSelectedItems().stream().map(r -> r.id).collect(Collectors.toList());

            new Thread(() -> {
                try {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("username", username);
                    if (!password.isBlank()) payload.put("password", password);
                    if (branch != null) payload.put("branchId", branch.id);
                    payload.put("roleIds", roleIds);

                    if (isEdit) {
                        context.apiClient().update("users", ((Number)rowData.get("id")).longValue(), payload);
                    } else {
                        context.apiClient().create("users", payload);
                    }
                    Platform.runLater(() -> new ResourceView(context, context.bundle().getString("nav.users"), "users").show());
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        saveBtn.setDisable(false);
                        UIUtils.showErrorPopup("Save Error", "Failed", ex);
                    });
                }
            }).start();
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

        int initialReqId = comboRequestId.incrementAndGet();
        new Thread(() -> {
            try { 
                ObservableList<UIUtils.IdName> states = UIUtils.fetchIdNames(context.apiClient(), "states");
                Platform.runLater(() -> {
                    if (initialReqId == comboRequestId.get()) {
                        stateCombo.setItems(states); 
                        if (isEdit && rowData.get("state") instanceof Map) {
                            Long id = ((Number) ((Map<?,?>)rowData.get("state")).get("id")).longValue();
                            states.stream().filter(s -> s.id.equals(id)).findFirst().ifPresent(stateCombo::setValue);
                        }
                    }
                });
            } catch (Exception ex) { Platform.runLater(() -> UIUtils.showErrorPopup("Loader Error", "Failed to load relationship data", ex)); }
        }).start();

        Button saveBtn = new Button(context.bundle().getString("form.save"));
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            saveBtn.setDisable(true);
            String name = nameField.getText(); if (name.isBlank()) { UIUtils.showWarningPopup("Validation Error", "Name is required"); saveBtn.setDisable(false); return; }
            UIUtils.IdName state = stateCombo.getValue(); if (state == null) { UIUtils.showWarningPopup("Validation Error", "State is required"); saveBtn.setDisable(false); return; }
            new Thread(() -> {
                try {
                    Map<String, Object> payload = Map.of("name", name, "stateId", state.id);
                    if (isEdit) {
                        context.apiClient().update("municipalities", ((Number)rowData.get("id")).longValue(), payload);
                    } else {
                        context.apiClient().create("municipalities", payload);
                    }
                    Platform.runLater(() -> new ResourceView(context, "Municipalities", "municipalities").show());
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        saveBtn.setDisable(false);
                        UIUtils.showErrorPopup("Save Error", "Failed", ex);
                    });
                }
            }).start();
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
        ComboBox<UIUtils.IdName> stateCombo = new ComboBox<>();
        ComboBox<UIUtils.IdName> muniCombo = new ComboBox<>();
        
        grid.addRow(0, new Label(context.bundle().getString("form.name")), nameField);
        grid.addRow(1, new Label("State:"), stateCombo);
        grid.addRow(2, new Label("Municipality:"), muniCombo);

        stateCombo.setOnAction(e -> {
            UIUtils.IdName selected = stateCombo.getValue();
            if (selected != null) {
                int reqId = comboRequestId.incrementAndGet();
                new Thread(() -> {
                    try {
                        var munis = UIUtils.fetchIdNames(context.apiClient(), "municipalities?stateId=" + selected.id);
                        Platform.runLater(() -> {
                            if (reqId == comboRequestId.get()) {
                                muniCombo.setItems(munis);
                            }
                        });
                    } catch (Exception ex) { Platform.runLater(() -> UIUtils.showErrorPopup("Loader Error", "Failed to load relationship data", ex)); }
                }).start();
            }
        });

        int initialReqId = comboRequestId.incrementAndGet();
        new Thread(() -> {
            try { 
                ObservableList<UIUtils.IdName> states = UIUtils.fetchIdNames(context.apiClient(), "states");
                Platform.runLater(() -> {
                    if (initialReqId == comboRequestId.get()) {
                        stateCombo.setItems(states);
                    }
                });

                if (isEdit && rowData.get("municipality") instanceof Map) {
                    Map<?,?> muniMap = (Map<?,?>) rowData.get("municipality");
                    if (muniMap.get("state") instanceof Map) {
                        Long stateId = ((Number) ((Map<?,?>)muniMap.get("state")).get("id")).longValue();
                        Platform.runLater(() -> {
                            if (initialReqId == comboRequestId.get()) {
                                states.stream().filter(s -> s.id.equals(stateId)).findFirst().ifPresent(s -> {
                                    stateCombo.setValue(s);
                                    int reqId = comboRequestId.incrementAndGet();
                                    new Thread(() -> {
                                        try {
                                            ObservableList<UIUtils.IdName> munis = UIUtils.fetchIdNames(context.apiClient(), "municipalities?stateId=" + s.id);
                                            Object mId = muniMap.get("id");
                                            Platform.runLater(() -> {
                                                if (mId != null && reqId == comboRequestId.get()) {
                                                    Long muniId = ((Number) mId).longValue();
                                                    muniCombo.setItems(munis);
                                                    munis.stream().filter(m -> m.id.equals(muniId)).findFirst().ifPresent(muniCombo::setValue);
                                                }
                                            });
                                        } catch (Exception ex) { Platform.runLater(() -> UIUtils.showErrorPopup("Loader Error", "Failed to load relationship data", ex)); }
                                    }).start();
                                });
                            }
                        });
                    }
                }
            } catch (Exception ex) { Platform.runLater(() -> UIUtils.showErrorPopup("Loader Error", "Failed to load relationship data", ex)); }
        }).start();

        Button saveBtn = new Button(context.bundle().getString("form.save"));
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            saveBtn.setDisable(true);
            UIUtils.IdName selectedMuni = muniCombo.getValue();
            String name = nameField.getText(); if (name.isBlank()) { UIUtils.showWarningPopup("Validation Error", "Name is required"); saveBtn.setDisable(false); return; }
            new Thread(() -> {
                try {
                    if (selectedMuni == null) throw new Exception("Please select a municipality");
                    Map<String, Object> payload = Map.of("name", name, "municipalityId", selectedMuni.id);
                    if (isEdit) {
                        context.apiClient().update("parishes", ((Number)rowData.get("id")).longValue(), payload);
                    } else {
                        context.apiClient().create("parishes", payload);
                    }
                    Platform.runLater(() -> new ResourceView(context, "Parishes", "parishes").show());
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        saveBtn.setDisable(false);
                        UIUtils.showErrorPopup("Save Error", "Failed", ex);
                    });
                }
            }).start();
        });
        root.getChildren().addAll(t, grid, saveBtn);
        context.viewSetter().accept(root);
    }

    static Map<String, Object> constructBagPayload(boolean isEdit, String name, String barcode, UIUtils.IdName branch, UIUtils.IdName dept) {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("name", name);
        payload.put("barcode", barcode);
        payload.put("branchId", branch.id);
        payload.put("assignedDepartmentId", dept.id);
        
        if (!isEdit) {
            payload.put("expectedItems", java.util.List.of());
        }
        return payload;
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

        int initialReqId = comboRequestId.incrementAndGet();
        new Thread(() -> {
            try {
                ObservableList<UIUtils.IdName> branches = UIUtils.fetchIdNames(context.apiClient(), "branches");
                Platform.runLater(() -> {
                    if (initialReqId == comboRequestId.get()) {
                        branchCombo.setItems(branches);
                        if (isEdit && rowData.get("branch") instanceof Map) {
                            Long id = ((Number) ((Map<?,?>)rowData.get("branch")).get("id")).longValue();
                            branches.stream().filter(b -> b.id.equals(id)).findFirst().ifPresent(branchCombo::setValue);
                        }
                    }
                });

                branchCombo.setOnAction(e -> {
                   UIUtils.IdName selected = branchCombo.getValue();
                   if (selected != null) {
                       int reqId = comboRequestId.incrementAndGet();
                       new Thread(() -> {
                           try {
                               var depts = UIUtils.fetchIdNames(context.apiClient(), "departments?branchId=" + selected.id);
                               Platform.runLater(() -> {
                                   if (reqId == comboRequestId.get()) {
                                       deptCombo.setItems(depts);
                                   }
                               });
                           } catch (Exception ex) { Platform.runLater(() -> UIUtils.showErrorPopup("Loader Error", "Failed to load relationship data", ex)); }
                       }).start();
                   }
                });
                
                if (isEdit && rowData.get("assignedDepartment") instanceof Map) {
                    Long bId = ((Number) ((Map<?,?>)rowData.get("branch")).get("id")).longValue();
                    Long dId = ((Number) ((Map<?,?>)rowData.get("assignedDepartment")).get("id")).longValue();
                    ObservableList<UIUtils.IdName> depts = UIUtils.fetchIdNames(context.apiClient(), "departments?branchId=" + bId);
                    Platform.runLater(() -> {
                        if (initialReqId == comboRequestId.get()) {
                            deptCombo.setItems(depts);
                            depts.stream().filter(d -> d.id.equals(dId)).findFirst().ifPresent(deptCombo::setValue);
                        }
                    });
                }
            } catch (Exception ex) { Platform.runLater(() -> UIUtils.showErrorPopup("Loader Error", "Failed to load relationship data", ex)); }
        }).start();

        Button saveBtn = new Button(context.bundle().getString("form.save"));
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            saveBtn.setDisable(true);
            String name = nameField.getText(); if (name.isBlank()) { UIUtils.showWarningPopup("Validation Error", "Name is required"); saveBtn.setDisable(false); return; }
            String barcode = barcodeField.getText(); if (barcode.isBlank()) { UIUtils.showWarningPopup("Validation Error", "Barcode is required"); saveBtn.setDisable(false); return; }
            UIUtils.IdName branch = branchCombo.getValue(); if (branch == null) { UIUtils.showWarningPopup("Validation Error", "Branch is required"); saveBtn.setDisable(false); return; }
            UIUtils.IdName dept = deptCombo.getValue(); if (dept == null) { UIUtils.showWarningPopup("Validation Error", "Department is required"); saveBtn.setDisable(false); return; }
            new Thread(() -> {
                try {
                    Map<String, Object> payload = constructBagPayload(isEdit, name, barcode, branch, dept);

                    if (isEdit) {
                        context.apiClient().update("bags", ((Number)rowData.get("id")).longValue(), payload);
                    } else {
                        context.apiClient().create("bags", payload);
                    }
                    Platform.runLater(() -> new ResourceView(context, context.bundle().getString("nav.bags"), "bags").show());
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        saveBtn.setDisable(false);
                        UIUtils.showErrorPopup("Save Error", "Failed", ex);
                    });
                }
            }).start();
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

        int initialReqId = comboRequestId.incrementAndGet();
        new Thread(() -> {
            try { 
                ObservableList<UIUtils.IdName> items = UIUtils.fetchIdNames(context.apiClient(), "items");
                Platform.runLater(() -> {
                    if (initialReqId == comboRequestId.get()) {
                        itemCombo.setItems(items); 
                        if (isEdit && rowData.get("item") instanceof Map) {
                            Long id = ((Number) ((Map<?,?>)rowData.get("item")).get("id")).longValue();
                            items.stream().filter(i -> i.id.equals(id)).findFirst().ifPresent(itemCombo::setValue);
                        } else if (!isEdit && prefill != null && prefill.containsKey("itemId")) {
                            Long id = ((Number) prefill.get("itemId")).longValue();
                            items.stream().filter(i -> i.id.equals(id)).findFirst().ifPresent(itemCombo::setValue);
                        }
                    }
                });
            } catch (Exception ex) { Platform.runLater(() -> UIUtils.showErrorPopup("Loader Error", "Failed to load relationship data", ex)); }
        }).start();
        
        Button saveBtn = new Button(context.bundle().getString("form.save"));
        saveBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            saveBtn.setDisable(true);
            UIUtils.IdName item = itemCombo.getValue(); if (item == null) { UIUtils.showWarningPopup("Validation Error", "Item is required"); saveBtn.setDisable(false); return; }
            String borrower = borrowerField.getText(); if (borrower.isBlank()) { UIUtils.showWarningPopup("Validation Error", "Borrower is required"); saveBtn.setDisable(false); return; }
            String reason = reasonArea.getText(); if (reason.isBlank()) { UIUtils.showWarningPopup("Validation Error", "Reason is required"); saveBtn.setDisable(false); return; }
            java.time.LocalDate returnDate = datePicker.getValue();

            new Thread(() -> {
                try {
                    Map<String, Object> payload = Map.of(
                        "itemId", item.id, 
                        "borrowerName", borrower,
                        "reason", reason,
                        "expectedReturnDate", returnDate.atStartOfDay().atOffset(java.time.ZoneOffset.UTC).toString()
                    );
                    if (isEdit) {
                        context.apiClient().update("displacements", ((Number)rowData.get("id")).longValue(), payload);
                    } else {
                        context.apiClient().create("displacements", payload);
                    }
                    Platform.runLater(() -> new ResourceView(context, context.bundle().getString("nav.displacements"), "displacements").show());
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        saveBtn.setDisable(false);
                        UIUtils.showErrorPopup("Save Error", "Failed", ex);
                    });
                }
            }).start();
        });
        root.getChildren().addAll(t, grid, saveBtn);
        context.viewSetter().accept(root);
    }
}
