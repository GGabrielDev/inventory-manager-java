package com.inventorymanager.frontend;

import com.inventorymanager.frontend.api.ApiClient;
import com.inventorymanager.frontend.ui.DesktopUi;
import javafx.application.Application;
import javafx.stage.Stage;

public class InventoryManagerDesktopApplication extends Application {
    @Override
    public void start(Stage stage) {
        String apiUrl = System.getenv().getOrDefault("INVENTORY_API_URL", "http://localhost:4000/api");
        ApiClient apiClient = new ApiClient(apiUrl);
        DesktopUi ui = new DesktopUi(stage, apiClient);
        ui.showLogin();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
