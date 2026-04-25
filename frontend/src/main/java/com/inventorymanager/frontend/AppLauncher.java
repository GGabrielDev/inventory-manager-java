package com.inventorymanager.frontend;

/**
 * Wrapper launcher to allow the JavaFX application to run from a fat JAR.
 * This class MUST NOT extend javafx.application.Application.
 */
public class AppLauncher {
    public static void main(String[] args) {
        InventoryManagerDesktopApplication.main(args);
    }
}
