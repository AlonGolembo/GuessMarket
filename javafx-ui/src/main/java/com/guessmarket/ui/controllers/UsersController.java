package com.guessmarket.ui.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

public class UsersController {

    @FXML private TableView<?> usersTableView;
    @FXML private TableColumn<?, ?> userIdCol;
    @FXML private TableColumn<?, ?> userNameCol;
    @FXML private TableColumn<?, ?> userBalanceCol;

    @FXML private Label accountBalanceLabel;

    @FXML private TableView<?> userEventsTableView;
    @FXML private TableColumn<?, ?> userEventNameCol;
    @FXML private TableColumn<?, ?> userEventRoleCol;
    @FXML private TableColumn<?, ?> userEventSharesCol;

    @FXML private VBox singleEventTradeDetailsContainer;

    @FXML
    private void initialize() {
        // Setup table column cell value factories & listeners
    }
}