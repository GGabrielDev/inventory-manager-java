package com.inventorymanager.frontend.ui.views;

import com.inventorymanager.frontend.api.ApiClient;
import com.inventorymanager.frontend.ui.ConfigManager;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.scene.Node;

public record ViewContext(
    ApiClient apiClient,
    ResourceBundle bundle,
    ConfigManager configManager,
    Consumer<Node> viewSetter,
    Runnable loginShower,
    Runnable dashboardShower,
    Runnable settingsShower,
    java.util.function.BiConsumer<String, String> formShower
) {}
