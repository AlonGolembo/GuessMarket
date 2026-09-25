package com.guessmarket.client.controllers;

import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.NewEventDTO;
import com.guessmarket.engine.api.MarketEngine;
import com.guessmarket.engine.exception.MarketException;
import com.guessmarket.client.common.Dialogs;
import com.guessmarket.client.common.FileLoadStatus;
import com.guessmarket.client.common.NewEventDialog;
import com.guessmarket.client.common.ThemeManager;
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
import java.util.Optional;

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
    /** This client's single identity for its whole run - set once at startup, before login. */
    private String currentUserName;
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
     * Injects the shared MarketEngine instance and the identity this run of the
     * client logged in as (login already happened, before the window was built -
     * see {@code GuessMarketApp}), and propagates both to the child tab controllers.
     */
    public void setContext(MarketEngine engine, String currentUserName) {
        this.engine = engine;
        this.currentUserName = currentUserName;

        if (eventsTabController != null) {
            eventsTabController.setContext(engine, currentUserName);
        }
        if (usersTabController != null) {
            usersTabController.setContext(engine, currentUserName);
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

    @FXML
    private void handleAddEvent() {
        if (engine == null) {
            return;
        }

        Window owner = rootPane.getScene().getWindow();
        Optional<NewEventDTO> spec = NewEventDialog.show(owner, currentUserName);
        if (spec.isEmpty()) {
            return;   // Cancel / closed
        }

        try {
            EventDTO created = engine.createEvent(spec.get());
            LOG.info("Event '{}' created", created.name());
            Dialogs.info("Event created",
                    "Event '" + created.name() + "' was created successfully.");
        } catch (MarketException ex) {
            LOG.warn("Event creation rejected: {}", ex.getMessage());
            Dialogs.error("Could not create the event", ex);
        }
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
     * Creates a background Task that uploads the XML file to the server. Unlike
     * the single-process build, this is a real network round trip - no artificial
     * delay is added to make the progress bar move, per the exercise spec.
     */
    private Task<Void> createLoadTask(String path) {
        String uploaderName = currentUserName;
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                LOG.info("User '{}' requested XML upload: {}", uploaderName, path);
                updateProgress(0.2, 1.0);
                // Engine handles the upload, validation, and fires onMarketDataChanged() to listeners
                engine.loadXmlFile(path, uploaderName);
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