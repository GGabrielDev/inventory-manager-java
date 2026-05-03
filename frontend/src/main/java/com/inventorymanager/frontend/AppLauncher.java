package com.inventorymanager.frontend;

/**
 * Wrapper launcher to allow the JavaFX application to run from a fat JAR.
 * This class MUST NOT extend javafx.application.Application.
 */
public class AppLauncher {
    public static void main(String[] args) {
        for (String arg : args) {
            if ("--verbose".equalsIgnoreCase(arg)) {
                System.setProperty("app.verbose", "true");
                System.out.println(">>> Verbose logging enabled");
            }
        }
        InventoryManagerDesktopApplication.main(args);
    }
}
