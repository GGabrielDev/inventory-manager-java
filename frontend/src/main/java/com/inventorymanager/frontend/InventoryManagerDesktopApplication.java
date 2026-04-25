package com.inventorymanager.frontend;

import com.inventorymanager.frontend.api.ApiClient;
import com.inventorymanager.frontend.ui.ConfigManager;
import com.inventorymanager.frontend.ui.DesktopUi;
import javafx.application.Application;
import javafx.stage.Stage;

public class InventoryManagerDesktopApplication extends Application {
    @Override
    public void start(Stage stage) {
        ConfigManager configManager = new ConfigManager();
        String apiUrl = configManager.getApiUrl();
        ApiClient apiClient = new ApiClient(apiUrl);
        DesktopUi ui = new DesktopUi(stage, apiClient, configManager);
        ui.showLogin();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
