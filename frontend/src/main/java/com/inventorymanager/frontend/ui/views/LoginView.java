package com.inventorymanager.frontend.ui.views;

import com.inventorymanager.frontend.ui.UIUtils;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class LoginView {
    private final ViewContext context;
    private TextField usernameField;
    private PasswordField passwordField;
    private Button loginBtn;
    private Label statusLabel;
    private java.util.function.Consumer<Runnable> uiThreadExecutor = Platform::runLater;

    public LoginView(ViewContext context) {
        this.context = context;
    }

    public void setUiThreadExecutor(java.util.function.Consumer<Runnable> executor) {
        this.uiThreadExecutor = executor;
    }

    public void show() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(40));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #f4f7f6;");

        Label title = new Label(context.bundle().getString("title.main"));
        title.setFont(Font.font("System", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#2c3e50"));

        VBox form = new VBox(10);
        form.setMaxWidth(300);
        
        usernameField = new TextField("admin");
        usernameField.setId("login-username");
        usernameField.setPromptText(context.bundle().getString("login.username"));
        usernameField.setPrefHeight(40);
        
        passwordField = new PasswordField();
        passwordField.setText("password");
        passwordField.setId("login-password");
        passwordField.setPromptText(context.bundle().getString("login.password"));
        passwordField.setPrefHeight(40);

        loginBtn = new Button(context.bundle().getString("login.signin"));
        loginBtn.setId("login-signin");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setPrefHeight(40);
        loginBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");

        Button settingsBtn = new Button(context.bundle().getString("login.settings"));
        settingsBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #7f8c8d;");

        loginBtn.setOnAction(event -> handleLogin());

        settingsBtn.setOnAction(event -> context.settingsShower().run());

        statusLabel = new Label();
        statusLabel.setId("login-status");
        statusLabel.setTextFill(Color.web("#e74c3c"));

        form.getChildren().addAll(new Label(context.bundle().getString("login.username")), usernameField, 
                             new Label(context.bundle().getString("login.password")), passwordField, loginBtn, settingsBtn, statusLabel);
        root.getChildren().addAll(title, form);

        context.viewSetter().accept(root);
    }

    public void handleLogin() {
        if (loginBtn != null) loginBtn.setDisable(true);
        String userText = normalizeCredential(usernameField);
        String passText = normalizeCredential(passwordField);
        performLogin(userText, passText);
    }

    public void performLogin(String username, String password) {
        if (username.isBlank() || password.isBlank()) {
            if (loginBtn != null) loginBtn.setDisable(false);
            showStatus(context.bundle().getString("login.status.invalid_input"));
            return;
        }
        
        new Thread(() -> {
            try {
                context.apiClient().login(username, password);
                uiThreadExecutor.accept(() -> context.dashboardShower().run());
            } catch (Exception exception) {
                uiThreadExecutor.accept(() -> {
                    if (loginBtn != null) loginBtn.setDisable(false);
                    showStatus(context.bundle().getString("login.status.auth_error"));
                    UIUtils.showErrorPopup(context.bundle().getString("login.status.fail"), context.bundle().getString("login.status.auth_error"), exception);
                });
            }
        }).start();
    }

    private String normalizeCredential(TextInputControl field) {
        if (field == null || field.getText() == null) {
            return "";
        }
        return field.getText().trim();
    }

    private void showStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }
}
