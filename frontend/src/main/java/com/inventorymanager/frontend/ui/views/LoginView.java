package com.inventorymanager.frontend.ui.views;

import com.inventorymanager.frontend.ui.UIUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class LoginView {
    private final ViewContext context;

    public LoginView(ViewContext context) {
        this.context = context;
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
        
        TextField username = new TextField("admin");
        username.setPromptText(context.bundle().getString("login.username"));
        username.setPrefHeight(40);
        
        PasswordField password = new PasswordField();
        password.setText("password");
        password.setPromptText(context.bundle().getString("login.password"));
        password.setPrefHeight(40);

        Button loginBtn = new Button(context.bundle().getString("login.signin"));
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setPrefHeight(40);
        loginBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");

        Button settingsBtn = new Button(context.bundle().getString("login.settings"));
        settingsBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #7f8c8d;");

        loginBtn.setOnAction(event -> {
            try {
                context.apiClient().login(username.getText(), password.getText());
                context.dashboardShower().run();
            } catch (Exception exception) {
                UIUtils.showErrorPopup(context.bundle().getString("login.status.fail"), context.bundle().getString("login.status.auth_error"), exception);
            }
        });

        settingsBtn.setOnAction(event -> context.settingsShower().run());

        form.getChildren().addAll(new Label(context.bundle().getString("login.username")), username, 
                             new Label(context.bundle().getString("login.password")), password, loginBtn, settingsBtn);
        root.getChildren().addAll(title, form);

        context.viewSetter().accept(root);
    }
}
