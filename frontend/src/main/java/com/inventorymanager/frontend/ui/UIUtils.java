package com.inventorymanager.frontend.ui;

import com.inventorymanager.frontend.api.ApiClient;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class UIUtils {
    public static void showWarningPopup(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText(message);
            alert.showAndWait();
        });
    }

    public static class ErrorReport {
        public final String displayMessage;
        public final String backendDetails;
        public ErrorReport(String msg, String det) { this.displayMessage = msg; this.backendDetails = det; }
    }

    public static ErrorReport parseErrorReport(String originalMessage, Exception ex) {
        String displayMessage = originalMessage;
        String backendDetails = "";
        if (ex.getMessage() != null && ex.getMessage().contains("{")) {
            try {
                String msg = ex.getMessage();
                int start = msg.indexOf("{");
                String jsonStr = msg.substring(start);
                java.util.Map<String, Object> errorMap = JsonUtil.map(jsonStr);
                if (errorMap.containsKey("message") && errorMap.get("message") != null) {
                    displayMessage = String.valueOf(errorMap.get("message"));
                }
                if (errorMap.containsKey("details") && errorMap.get("details") != null) {
                    backendDetails = String.valueOf(errorMap.get("details"));
                }
                if (errorMap.containsKey("backendError") && errorMap.get("backendError") != null) {
                    backendDetails = "Backend Error: " + String.valueOf(errorMap.get("backendError"));
                }
            } catch (Exception ignored) {}
        }
        return new ErrorReport(displayMessage, backendDetails);
    }

    public static void showErrorPopup(String title, String message, Exception ex) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(message);
            
            ErrorReport report = parseErrorReport(message, ex);
            String displayMessage = report.displayMessage;
            String backendDetails = report.backendDetails;

            StringBuilder sb = new StringBuilder();
            sb.append("--- ERROR REPORT ---\n").append("Timestamp: ").append(java.time.LocalDateTime.now()).append("\n")
              .append("Title: ").append(title).append("\n").append("Message: ").append(displayMessage).append("\n")
              .append("Exception: ").append(ex.toString()).append("\n\n");
            
            if (!backendDetails.isBlank()) {
                sb.append("--- BACKEND STACK TRACE ---\n").append(backendDetails).append("\n\n");
            }
            
            sb.append("--- FRONTEND STACK TRACE ---\n");
            java.io.StringWriter sw = new java.io.StringWriter();
            ex.printStackTrace(new java.io.PrintWriter(sw));
            sb.append(sw.toString());
            
            String detailedReport = sb.toString();
            TextArea textArea = new TextArea(detailedReport);
            textArea.setEditable(false); textArea.setWrapText(true); textArea.setPrefHeight(300); textArea.setPrefWidth(600);
            Button copyBtn = new Button("📋 Copy error to clipboard");
            copyBtn.setOnAction(e -> {
                javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(detailedReport);
                clipboard.setContent(content);
                copyBtn.setText("✅ Copied!");
            });
            VBox content = new VBox(10, 
                new Label("Error details:"), 
                new Label("--- Technical Details ---"),
                textArea, 
                copyBtn, 
                new Label("--- Contact Programmer ---"),
                new Label("Please check connection or contact system administrator."));
            alert.getDialogPane().setContent(content);
            alert.showAndWait();
        });
    }

    public static Object extractDeepValue(Object val) {
        if (val == null) return "";
        if (val instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) val;
            String[] preferredKeys = {"name", "username", "title", "barcode", "requestedItemName", "borrowerName"};
            for (String key : preferredKeys) {
                if (m.containsKey(key) && m.get(key) != null) return m.get(key);
            }
            
            // Fallback: search one level deeper
            for (Object subVal : m.values()) {
                if (subVal instanceof Map) {
                    Object deep = extractDeepValue(subVal);
                    if (deep != null && !deep.toString().startsWith("{") && !deep.toString().isBlank()) {
                        return deep;
                    }
                }
            }
            
            if (m.containsKey("id")) return "#" + m.get("id");
            return m.toString();
        } else if (val instanceof List) {
            return ((List<?>) val).stream()
                    .map(UIUtils::extractDeepValue)
                    .map(Object::toString)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.joining(", "));
        }
        return val;
    }

    public static ObservableList<IdName> fetchIdNames(ApiClient apiClient, String resource) throws Exception {
        return FXCollections.observableArrayList(
            apiClient.list(resource).stream()
                .map(m -> {
                    long id = SafeExtractor.safeLong(m, "id", -1L);
                    String name = SafeExtractor.safeString(m, "name", SafeExtractor.safeString(m, "username", "Unknown"));
                    return new IdName(id, name);
                })
                .collect(Collectors.toList())
        );
    }

    public static class IdName {
        public final Long id;
        public final String name;
        public IdName(Long id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }
}
