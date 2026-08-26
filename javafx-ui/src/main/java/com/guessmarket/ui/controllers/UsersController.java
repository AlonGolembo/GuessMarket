package com.guessmarket.ui.controllers;

import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.UserDTO;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

public class UsersController {

    @FXML private TableView<UserDTO> usersTableView;
    @FXML private TableColumn<UserDTO, String> userNameCol;
    @FXML private TableColumn<UserDTO, Integer> userBalanceCol;

    @FXML private Label accountBalanceLabel;

    @FXML private TableView<?> userEventsTableView;
    @FXML private TableColumn<?, ?> userEventNameCol;
    @FXML private TableColumn<?, ?> userEventRoleCol;
    @FXML private TableColumn<?, ?> userEventSharesCol;

    @FXML private VBox singleEventTradeDetailsContainer;

    @FXML
    private void initialize() {
        // Setup table column cell value factories & listeners
        userNameCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().name()));
        userBalanceCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().initialCash()));
    }

    public void bindUsersList(ObservableList<UserDTO> sharedUsersList) {
        usersTableView.setItems(sharedUsersList);
    }
}