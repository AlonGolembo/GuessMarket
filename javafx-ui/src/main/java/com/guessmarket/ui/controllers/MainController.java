package com.guessmarket.ui.controllers;

import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.UserDTO;
import com.guessmarket.engine.api.MarketEngine;
import com.guessmarket.ui.common.FileLoadStatus;
import javafx.animation.PauseTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.InputStream;
import java.util.List;

public class MainController {
    @FXML public Label fileLoadMessage;
    @FXML private Button loadFileButton;
    @FXML private TextField filePathTextField;
    @FXML private ImageView fileStatusIcon;
    @FXML private ProgressBar fileLoadProgress;
    @FXML private HBox progressRowContainer;

    // Injected child controllers (fx:id + "Controller")
    @FXML private EventsController eventsTabController;
    @FXML private UsersController usersTabController;

    private final ObservableList<EventDTO> eventsList = FXCollections.observableArrayList();
    private final ObservableList<UserDTO> usersList = FXCollections.observableArrayList();

    // Capital "Void" here
    private final ObjectProperty<Task<Void>> currentTaskProperty = new SimpleObjectProperty<>();

    // Transitions
    private final PauseTransition loadMessageDismissTimer = new PauseTransition(Duration.seconds(2));

    // Controller properties
    private MarketEngine engine;
    private final StringProperty loadMessage = new SimpleStringProperty("");
    private final ObjectProperty<FileLoadStatus> loadStatus = new SimpleObjectProperty<>(FileLoadStatus.NONE);
    private Image successImage;
    private Image errorImage;

    @FXML
    private void initialize() {
        InputStream successStream = getClass().getResourceAsStream("/images/check-mark.png");
        InputStream errorStream = getClass().getResourceAsStream("/images/remove.png");

        if (successStream != null) successImage = new Image(successStream);
        if (errorStream != null) errorImage = new Image(errorStream);

        fileLoadMessage.textProperty().bind(loadMessage);
        progressRowContainer.visibleProperty().bind(loadMessage.isNotEmpty());
        progressRowContainer.managedProperty().bind(loadMessage.isNotEmpty());

        fileLoadProgress.progressProperty().bind(
                Bindings.createDoubleBinding(() -> {
                    Task<?> task = currentTaskProperty.get();
                    return task != null ? task.getProgress() : 0.0;
                }, currentTaskProperty.flatMap(Task::progressProperty))
        );

        loadMessageDismissTimer.setOnFinished(e -> loadMessage.set(""));

        loadStatus.addListener((obs, oldStatus, newStatus) -> {
            switch (newStatus) {
                case SUCCESS -> fileStatusIcon.setImage(successImage);
                case ERROR -> fileStatusIcon.setImage(errorImage);
                case NONE -> fileStatusIcon.setImage(null);
            }
        });

        if (eventsTabController != null) {
            eventsTabController.bindEventsList(eventsList);
        }

        if (usersTabController != null) {
            usersTabController.bindUsersList(usersList);
        }
    }

    @FXML
    private void handleLoadFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open XML Configuration");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));
        File selectedFile = fileChooser.showOpenDialog(loadFileButton.getScene().getWindow());

        if (selectedFile == null) return;

        loadMessageDismissTimer.stop();
        filePathTextField.setText(selectedFile.getAbsolutePath());

        // Create the Void task
        Task<Void> task = createLoadTask(selectedFile.getAbsolutePath());
        currentTaskProperty.set(task);

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private Task<Void> createLoadTask(String path) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateProgress(0.2, 1.0);
                engine.loadXmlFile(path); // Background XML unmarshalling & validation
                updateProgress(1.0, 1.0);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            // Populate both lists from the engine once loaded
            eventsList.setAll(engine.getAllEvents());
            usersList.setAll(engine.getAllUsers());

            loadMessage.set("XML loaded successfully!");
            loadStatus.set(FileLoadStatus.SUCCESS);
            loadMessageDismissTimer.playFromStart();
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            loadMessage.set(ex != null ? ex.getMessage() : "Failed to load XML file.");
            loadStatus.set(FileLoadStatus.ERROR);
            loadMessageDismissTimer.playFromStart();
        });

        return task;
    }

    public void setEngine(MarketEngine engine) {
        this.engine = engine;
    }
}