package com.guessmarket.ui.common;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Modal dialog for turning the app's two UI animations on or off. The checkboxes
 * start from the current {@link AnimationSettings}; the changes are written back
 * only when the user clicks <em>Save</em> (Cancel / close discards them).
 */
public final class AnimationSettingsDialog {

    private AnimationSettingsDialog() {}

    public static void show(Window owner) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Animations");
        dialog.setHeaderText("Enable or disable UI animations");
        dialog.initOwner(owner);

        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(saveType, ButtonType.CANCEL);

        CheckBox userDetailsReveal = new CheckBox("User details slide-in (Users tab)");
        userDetailsReveal.setSelected(AnimationSettings.isUserDetailsRevealEnabled());

        CheckBox panelReveal = new CheckBox("Event details slide-in (Events tab)");
        panelReveal.setSelected(AnimationSettings.isEventPanelRevealEnabled());

        Label hint = new Label("Unchecked animations are skipped; the UI updates instantly instead.");
        hint.setWrapText(true);
        hint.setStyle("-fx-font-size: 11px; -fx-text-fill: #808080;");

        VBox content = new VBox(10, userDetailsReveal, panelReveal, hint);
        content.setPadding(new Insets(10, 4, 4, 4));
        content.setPrefWidth(340);
        dialog.getDialogPane().setContent(content);

        ButtonType clicked = dialog.showAndWait().orElse(ButtonType.CANCEL);
        if (clicked == saveType) {
            AnimationSettings.setUserDetailsRevealEnabled(userDetailsReveal.isSelected());
            AnimationSettings.setEventPanelRevealEnabled(panelReveal.isSelected());
        }
    }
}
