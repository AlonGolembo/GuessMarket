package com.guessmarket.ui.controllers;

import com.guessmarket.engine.api.MarketEngine;
import com.guessmarket.ui.common.Dialogs;
import com.guessmarket.ui.common.FileLoadStatus;
import com.guessmarket.ui.common.ThemeManager;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.InputStream;

/**
 * Main window controller coordinating top-level actions (file loading, global progress)
 * and injecting the single MarketEngine instance into child tab controllers.
 */
public class MainController {

    private static final Logger LOG = LogManager.getLogger(MainController.class);

    // =========================================================================
    // FXML UI Controls
    // =========================================================================
    @FXML private BorderPane rootPane;
    @FXML private Button loadFileButton;
    @FXML private TextField filePathTextField;
    @FXML private ImageView fileStatusIcon;
    @FXML private ProgressBar fileLoadProgress;
    @FXML private HBox progressRowContainer;
//    @FXML private Button saveState;

    // =========================================================================
    // Injected Child Tab Controllers (fx:id + "Controller")
    // =========================================================================
    @FXML private EventsController eventsTabController;
    @FXML private UsersController usersTabController;

    // =========================================================================
    // Controller State & Observable Properties
    // =========================================================================
    private MarketEngine engine;
    private final StringProperty loadMessage = new SimpleStringProperty("");
    private final ObjectProperty<FileLoadStatus> loadStatus = new SimpleObjectProperty<>(FileLoadStatus.NONE);
    private final ObjectProperty<Task<Void>> currentTaskProperty = new SimpleObjectProperty<>();

    // UI Timers & Visual Assets
    private final PauseTransition loadMessageDismissTimer = new PauseTransition(Duration.seconds(2));
    private Image successImage;
    private Image errorImage;

    // =========================================================================
    // Lifecycle & Initialization
    // =========================================================================
    @FXML
    private void initialize() {
        loadIcons();
        setupBindings();
        setupListenersAndTimers();
    }

    /**
     * Loads status icon resources from classpath.
     */
    private void loadIcons() {
        InputStream successStream = getClass().getResourceAsStream("/images/check-mark.png");
        InputStream errorStream = getClass().getResourceAsStream("/images/remove.png");

        if (successStream != null) successImage = new Image(successStream);
        if (errorStream != null) errorImage = new Image(errorStream);
    }

    /**
     * Sets up UI data bindings for messages, containers, and background task progress.
     */
    private void setupBindings() {
        // Bind load message and container visibility
        progressRowContainer.visibleProperty().bind(loadMessage.isNotEmpty());
        progressRowContainer.managedProperty().bind(loadMessage.isNotEmpty());

        // Dynamically bind ProgressBar to current running task progress
        fileLoadProgress.progressProperty().bind(
                Bindings.createDoubleBinding(() -> {
                    Task<?> task = currentTaskProperty.get();
                    return task != null ? task.getProgress() : 0.0;
                }, currentTaskProperty.flatMap(Task::progressProperty))
        );
    }

    /**
     * Configures property change listeners and animation dismiss actions.
     */
    private void setupListenersAndTimers() {
        // Auto-clear notification message when timer finishes
        loadMessageDismissTimer.setOnFinished(e -> loadMessage.set(""));

        // Update status icon based on loading status outcome
        loadStatus.addListener((obs, oldStatus, newStatus) -> {
            switch (newStatus) {
                case SUCCESS -> fileStatusIcon.setImage(successImage);
                case ERROR -> fileStatusIcon.setImage(errorImage);
                case NONE -> fileStatusIcon.setImage(null);
            }
        });
    }

    // =========================================================================
    // Dependency Injection
    // =========================================================================
    /**
     * Injects the shared MarketEngine instance into this controller
     * and propagates it down to all child tab controllers.
     */
    public void setEngine(MarketEngine engine) {
        this.engine = engine;

        if (eventsTabController != null) {
            eventsTabController.setEngine(engine);
        }
        if (usersTabController != null) {
            usersTabController.setEngine(engine);
        }
    }

    // =========================================================================
    // Event Handlers & Background Tasks
    // =========================================================================
    @FXML
    private void handleClose() {
        LOG.info("User requested application exit from menu");
        Platform.exit();
    }

    @FXML void handleSaveState(){
        LOG.info("User requested application to save state");

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a Folder");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.setInitialFileName("market_snapshot");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("SER Files (*.ser)", "*.ser"),
                new FileChooser.ExtensionFilter("Text Files (*.txt)", "*.txt"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File targetFile = fileChooser.showSaveDialog(rootPane.getScene().getWindow());
        engine.saveState(targetFile.getAbsolutePath());
    }

    @FXML void handleLoadState(){
        LOG.info("User requested application to load state");

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a File");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("SER Files (*ser)", "*.ser"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File fileToLoad = fileChooser.showOpenDialog(rootPane.getScene().getWindow());
        engine.loadState(fileToLoad.getAbsolutePath());
    }

    @FXML
    private void handleAbout() {
        Dialogs.info("About Guess Market",
                "Guess Market\nA prediction-market system (LMSR & Order Book).");
    }

    @FXML
    private void handleChangeTheme(){
        Window currentWindow = rootPane.getScene().getWindow();
        ThemeManager.openThemeSelector(currentWindow);
    }

    @FXML
    private void handleLoadFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open XML Configuration");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));
        File selectedFile = fileChooser.showOpenDialog(loadFileButton.getScene().getWindow());

        if (selectedFile == null) return;

        // Ask before replacing an already-loaded market. This must happen here,
        // on the FX thread - a dialog cannot be shown from the background task.
        if (engine.isFileLoaded() && !Dialogs.confirm("Replace loaded file?",
                "A file is already loaded. Loading this one will replace it.\nContinue?")) {
            return;
        }

        loadMessageDismissTimer.stop();

        // Create and track the background task
        Task<Void> task = createLoadTask(selectedFile.getAbsolutePath());
        currentTaskProperty.set(task);

        // Execute off the JavaFX Application Thread
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Creates a background Task that loads and parses the XML file in the engine.
     */
    private Task<Void> createLoadTask(String path) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                LOG.info("User requested XML load: {}", path);
                updateProgress(0.2, 1.0);
                // Engine handles XML unmarshalling, validation, and fires onMarketDataChanged() to listeners
                engine.loadXmlFile(path);
                updateProgress(1.0, 1.0);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            LOG.info("XML load succeeded: {}", path);
            filePathTextField.setText(path);
            loadMessage.set("XML loaded successfully!");
            loadStatus.set(FileLoadStatus.SUCCESS);
            loadMessageDismissTimer.playFromStart();
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            LOG.warn("XML load failed for {}: {}", path, ex != null ? ex.getMessage() : "unknown error");
            if(loadStatus.getValue() == FileLoadStatus.NONE) {
                Dialogs.error("Failed to load XML", ex);
                loadStatus.set(FileLoadStatus.ERROR);
            }
            else{
                Dialogs.error("Failed to load XML", ex + "\nNo file was loaded");
            }
            loadMessageDismissTimer.playFromStart();
        });

        return task;
    }
}