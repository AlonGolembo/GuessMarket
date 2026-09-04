package com.guessmarket.ui.common;

import javafx.scene.control.Alert;

/**
 * Modal message dialogs, shared by the controllers so error reporting looks and
 * behaves the same everywhere.
 */
public final class Dialogs {

    private Dialogs() {}

    /** Shows a blocking error dialog. */
    public static void error(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message == null || message.isBlank()
                ? "An unexpected error occurred."
                : message);
        alert.showAndWait();
    }

    /**
     * Shows a blocking error dialog for an exception, using its message (or its
     * type name when the message is absent).
     */
    public static void error(String title, Throwable cause) {
        String message = cause == null ? null : cause.getMessage();
        if (message == null && cause != null) {
            message = cause.getClass().getSimpleName();
        }
        error(title, message);
    }
}
