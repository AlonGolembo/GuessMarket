package com.guessmarket.ui.common;

import javafx.scene.Scene;
import javafx.scene.control.ChoiceDialog;
import javafx.stage.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;

/**
 * Bonus 1 - swappable skins. Each skin is a stylesheet that changes the whole
 * window's background, the look of the buttons, and the font family + size of
 * every label. The app starts on the plain default look (no stylesheet); the
 * user opens this selector from <em>System &rarr; Theme</em>.
 */
public class ThemeManager {

    private static final Logger LOG = LogManager.getLogger(ThemeManager.class);

    private static final String DARK_THEME = resource("/com/guessmarket/ui/styles/dark.css");
    private static final String LIGHT_THEME = resource("/com/guessmarket/ui/styles/light.css");
    private static final String SOLARIZED_THEME = resource("/com/guessmarket/ui/styles/solarized.css");
    private static final String NORD_THEME = resource("/com/guessmarket/ui/styles/nord.css");
    private static final String SEPIA_THEME = resource("/com/guessmarket/ui/styles/sepia.css");

    private static final String DEFAULT_LABEL = "Default Theme";
    private static final String LIGHT_LABEL = "Light Theme";
    private static final String DARK_LABEL = "Dark Theme";
    private static final String SOLARIZED_LABEL = "Solarized Theme";
    private static final String NORD_LABEL = "Nord Theme";
    private static final String SEPIA_LABEL = "Sepia Theme";

    /** Resolves a classpath stylesheet to the URL string a Scene expects. */
    private static String resource(String path) {
        var url = ThemeManager.class.getResource(path);
        if (url == null) {
            throw new IllegalStateException("Missing stylesheet on classpath: " + path);
        }
        return url.toExternalForm();
    }

    /** Shows a theme selector dialog. */
    public static void openThemeSelector(Window ownerWindow) {
        List<String> themes = List.of(
                DEFAULT_LABEL, LIGHT_LABEL, DARK_LABEL, SOLARIZED_LABEL, NORD_LABEL, SEPIA_LABEL);

        ChoiceDialog<String> dialog = new ChoiceDialog<>(currentTheme(ownerWindow.getScene()), themes);
        dialog.initOwner(ownerWindow);
        dialog.setTitle("Theme Selection");
        dialog.setHeaderText("Choose App Theme");
        dialog.setGraphic(null);
        dialog.setContentText("Select Theme:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(selectedTheme -> changeTheme(ownerWindow.getScene(), selectedTheme));
    }

    private static String currentTheme(Scene scene) {
        if (scene.getStylesheets().isEmpty()) {
            return DEFAULT_LABEL;
        }
        String css = scene.getStylesheets().get(0);
        if (css.equals(DARK_THEME)) return DARK_LABEL;
        if (css.equals(SOLARIZED_THEME)) return SOLARIZED_LABEL;
        if (css.equals(NORD_THEME)) return NORD_LABEL;
        if (css.equals(SEPIA_THEME)) return SEPIA_LABEL;
        return LIGHT_LABEL;
    }

    /** Removes any skin - back to the plain default look. */
    public static void setDefaultTheme(Scene scene) {
        scene.getStylesheets().clear();
    }

    public static void setDarkTheme(Scene scene) {
        scene.getStylesheets().setAll(DARK_THEME);
    }

    public static void setLightTheme(Scene scene) {
        scene.getStylesheets().setAll(LIGHT_THEME);
    }

    public static void setSolarizedTheme(Scene scene) {
        scene.getStylesheets().setAll(SOLARIZED_THEME);
    }

    public static void setNordTheme(Scene scene) {
        scene.getStylesheets().setAll(NORD_THEME);
    }

    public static void setSepiaTheme(Scene scene) {
        scene.getStylesheets().setAll(SEPIA_THEME);
    }

    public static void changeTheme(Scene scene, String theme) {
        switch (theme) {
            case DEFAULT_LABEL -> setDefaultTheme(scene);
            case DARK_LABEL -> setDarkTheme(scene);
            case LIGHT_LABEL -> setLightTheme(scene);
            case SOLARIZED_LABEL -> setSolarizedTheme(scene);
            case NORD_LABEL -> setNordTheme(scene);
            case SEPIA_LABEL -> setSepiaTheme(scene);
            default -> {
                LOG.warn("Ignoring request to apply unknown theme: {}", theme);
                return;
            }
        }
        LOG.info("Theme changed to {}", theme);
    }
}
