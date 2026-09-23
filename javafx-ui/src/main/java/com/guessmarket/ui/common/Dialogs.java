package com.guessmarket.ui.common;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;

import java.util.Optional;

/**
 * Modal message dialogs, shared by the controllers so error reporting looks and
 * behaves the same everywhere.
 */
public final class Dialogs {

    private Dialogs() {}

    /** Prompts for a line of text; empty if the user cancelled. */
    public static Optional<String> prompt(String title, String header, String promptText) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(promptText);
        return dialog.showAndWait();
    }

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

    /** Shows a blocking informational dialog. */
    public static void info(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /** Shows a blocking OK/Cancel dialog; returns {@code true} if the user chose OK. */
    public static boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
        return alert.getResult() == ButtonType.OK;
    }
}
