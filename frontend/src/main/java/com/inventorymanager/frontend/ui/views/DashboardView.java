package com.inventorymanager.frontend.ui.views;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class DashboardView {
    private final ViewContext context;
    private final boolean globalContext;

    public DashboardView(ViewContext context, boolean globalContext) {
        this.context = context;
        this.globalContext = globalContext;
    }

    public void show() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        
        String contextText = globalContext ? context.bundle().getString("dashboard.org_view") : context.bundle().getString("dashboard.branch_view");
        Label title = new Label(contextText);
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        
        HBox cards = new HBox(20);
        cards.getChildren().addAll(
            createStatCard(context.bundle().getString("dashboard.total_assets"), "...", "#3498db"),
            createStatCard(context.bundle().getString("dashboard.pending_reviews"), "...", "#e67e22"),
            createStatCard(context.bundle().getString("dashboard.active_displacements"), "...", "#e74c3c")
        );
        
        root.getChildren().addAll(title, cards);
        context.viewSetter().accept(root);
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
}
