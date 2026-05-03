# Inventory Manager UI Style Guide (v2.0)

This document defines the visual and functional standards for the JavaFX desktop application. All future UI development must adhere to these guidelines to maintain a professional, high-density experience modeled after industry leaders like NetSuite and Zoho Inventory.

---

## 1. Color Palette

Consistency in color usage ensures a cohesive look and feel across modules.

| Role | Hex Code | Usage |
| :--- | :--- | :--- |
| **Primary Sidebar** | `#2c3e50` | Background for navigation and branding. |
| **Action Primary** | `#3498db` | Primary buttons, main dashboard icons. |
| **Success/Create** | `#27ae60` | "Add New", "Save", "Submit" actions. |
| **Warning** | `#e67e22` | "Borrow", "Request Review", "Displace". |
| **Danger/Error** | `#e74c3c` | "Delete", "Reject", "Report Missing". |
| **App Background** | `#f4f7f6` | Default background for login and dashboard frames. |
| **Content Area** | `#ffffff` | Background for data tables and forms. |
| **Text Secondary** | `#7f8c8d` | Placeholders, helper text, nav group labels. |

---

## 2. Typography

We use the system default font with strict weight and size guidelines.

*   **Main Title (Login)**: `FontWeight.BOLD`, size `24pt`.
*   **Screen Header**: `FontWeight.BOLD`, size `22pt`.
*   **Module Header**: `FontWeight.BOLD`, size `18pt`.
*   **Nav Group Label**: `FontWeight.BOLD`, size `11pt` (Uppercase).
*   **Body Text**: System default weight, size `12pt`.

---

## 3. Layout Architecture

The application uses a `BorderPane` as the main container for authenticated views.

### 3.1 Sidebar (LEFT)
*   **Width**: `220px`.
*   **Padding**: `10px`.
*   **Interaction**: Buttons must change background to `#34495e` and text to `white` on hover.
*   **Groups**: Use `Separator` and `createNavGroupLabel` to logically split Inventory, Operations, and System.

### 3.2 Header (TOP)
*   **Height**: Fixed content height with `15px` vertical padding.
*   **Alignment**: Content right-aligned (`Pos.CENTER_RIGHT`).
*   **Elements**: Organization context toggle (Admin only), logged-in username, and Logout button.

### 3.3 Content Area (CENTER)
*   **Container**: `StackPane` for dynamic view swapping.
*   **Internal Padding**: `20px` minimum.

---

## 4. Component Standards

### 4.1 Data Tables (`TableView`)
*   **Density**: High.
*   **Policy**: Always use `TableView.CONSTRAINED_RESIZE_POLICY`.
*   **Headers**: Uppercase (e.g., "ITEM NAME", "QUANTITY").
*   **Empty State**: Tables should be hidden or show a clear "No records found" label.
*   **Resolution**: Objects (like Branch or Category) should display their `.get("name")` property string.

### 4.2 Forms (`GridPane`)
*   **Alignment**: `Pos.CENTER_LEFT` or `Pos.TOP_LEFT`.
*   **Spacing**: `10px` horizontal and vertical gap.
*   **Labels**: Must appear to the left of the input field.
*   **Validation**: Required fields must be validated before the `POST/PUT` request is triggered.

### 4.3 Dialogs & Alerts
*   **Errors**: Use `Alert.AlertType.ERROR` with a consistent "Technical Details" and "Contact Programmer" section.
*   **Confirmation**: Destructive actions (like Delete) MUST prompt the user for confirmation.

---

## 5. User Experience (UX)

*   **Immediate Feedback**: All remote actions (Save/Delete) should trigger a view refresh or a confirmation toast.
*   **Graceful Degradation**: If the API is unreachable, the UI must show the Connection Settings popup rather than crashing.
*   **State Persistence**: The application must remember the user's `language` and `apiUrl` locally.
*   **Modularity**: Major screens (Dashboard, Audit) should be implemented as separate methods or classes rather than bloating the main `start` method.
