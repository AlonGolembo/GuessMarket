package com.guessmarket.ui.controllers;

import com.guessmarket.engine.api.MarketEngine;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import java.io.File;

public class MainController {
    @FXML private Button loadFileButton;
    @FXML private TextField filePathTextField;
    @FXML private Label fileErrorMessage;

    // Injected child controllers (fx:id + "Controller")
    @FXML private EventsController eventsTabController;
    @FXML private UsersController usersTabController;

    private MarketEngine engine;
    private final StringProperty errorMessage = new SimpleStringProperty("");

    @FXML
    private void initialize() {
        fileErrorMessage.textProperty().bind(errorMessage);
        fileErrorMessage.visibleProperty().bind(errorMessage.isNotEmpty());
        fileErrorMessage.managedProperty().bind(errorMessage.isNotEmpty());
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
            filePathTextField.setText(selectedFile.getAbsolutePath());
            // Call engine loading logic, then notify child controllers:
            // eventsTabController.loadEventsData(...);
            // usersTabController.loadUsersData(...);
            try{
                engine.loadXmlFile(selectedFile.toString());
            }catch(Exception e){
                errorMessage.set(e.getMessage() != null ? e.getMessage() : "Unknown error occurred.");
            }
        }
    }

    public void setEngine(MarketEngine engine){
        this.engine = engine;
    }
}