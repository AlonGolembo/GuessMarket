package com.guessmarket.ui.controllers;

import com.guessmarket.dto.EventDTO;
import com.guessmarket.engine.api.MarketEngine;
import com.guessmarket.ui.common.FileLoadStatus;
import javafx.animation.PauseTransition;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.RowConstraints;
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

    // Transitions
    private final PauseTransition loadMessageDismissTimer = new PauseTransition(Duration.seconds(2));

    // Set controller properties
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
//        fileLoadMessage.visibleProperty().bind(loadMessage.isNotEmpty());
//        fileLoadMessage.managedProperty().bind(loadMessage.isNotEmpty());
        progressRowContainer.visibleProperty().bind(loadMessage.isNotEmpty());
        progressRowContainer.managedProperty().bind(loadMessage.isNotEmpty());

        // Configure timer action: clear error and reset status
        loadMessageDismissTimer.setOnFinished(e -> {
            loadMessage.set("");
        });

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
    }

    @FXML
    private void handleLoadFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open XML Configuration");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("XML Files", "*.xml")
        );
        File selectedFile = fileChooser.showOpenDialog(loadFileButton.getScene().getWindow());

        if (selectedFile != null) {
            loadMessageDismissTimer.stop();
            filePathTextField.setText(selectedFile.getAbsolutePath());
            loadFileButton.setDisable(true);

            // Load XML file with engine
            Task<List<EventDTO>> loadTask = new Task<>() {
                @Override
                protected List<EventDTO> call() throws Exception {
                    engine.loadXmlFile(selectedFile.getAbsolutePath());
                    return engine.getAllEvents();
                }
            };

            // Handle Success
            loadTask.setOnSucceeded(e -> {
                List<EventDTO> loadedEvents = loadTask.getValue();
                eventsList.setAll(loadedEvents);

                loadMessage.set("XML loaded successfully!");
                loadStatus.set(FileLoadStatus.SUCCESS);
                loadMessageDismissTimer.playFromStart();
                loadFileButton.setDisable(false);
            });

            // Handle Failure
            loadTask.setOnFailed(e -> {
                Throwable ex = loadTask.getException();
                String error = (ex != null && ex.getMessage() != null) ? ex.getMessage() : "Unknown error occurred.";

                loadMessage.set(error);
                loadStatus.set(FileLoadStatus.ERROR);
                loadMessageDismissTimer.playFromStart();
                loadFileButton.setDisable(false);
            });

            Thread thread = new Thread(loadTask);
            thread.setDaemon(true);
            thread.start();

        }
    }

    public void setEngine(MarketEngine engine){
        this.engine = engine;
    }
}