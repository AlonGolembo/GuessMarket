package com.guessmarket.ui.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

public class EventsController {

    @FXML private ComboBox<String> methodFilterComboBox;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private ComboBox<String> commissionFilterComboBox;

    @FXML private TableView<?> eventsTableView;
    @FXML private TableColumn<?, ?> eventIdCol;
    @FXML private TableColumn<?, ?> eventNameCol;
    @FXML private TableColumn<?, ?> eventStatusCol;
    @FXML private TableColumn<?, ?> eventMethodCol;

    @FXML private VBox eventTradeDetailsContainer;

    @FXML private TableView<?> option1OrderBookTable;
    @FXML private TableColumn<?, ?> opt1PriceCol;
    @FXML private TableColumn<?, ?> opt1AmountCol;

    @FXML private TableView<?> option2OrderBookTable;
    @FXML private TableColumn<?, ?> opt2PriceCol;
    @FXML private TableColumn<?, ?> opt2AmountCol;

    @FXML private TableView<?> participationsTableView;
    @FXML private TableColumn<?, ?> partUserCol;
    @FXML private TableColumn<?, ?> partOptionCol;
    @FXML private TableColumn<?, ?> partSharesCol;

    @FXML
    private void initialize() {
        // Setup table column cell value factories & listeners
    }
}