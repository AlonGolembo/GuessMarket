package com.guessmarket.ui.controllers;

import com.guessmarket.dto.EventDTO;
import com.guessmarket.engine.api.MarketEngine;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.util.List;

public class EventsController {

    @FXML private ComboBox<String> methodFilterComboBox;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private ComboBox<String> commissionFilterComboBox;

    @FXML private TableView<EventDTO> eventsTableView;
    @FXML private TableColumn<EventDTO, Integer> eventIdCol;
    @FXML private TableColumn<EventDTO, String> eventNameCol;
    @FXML private TableColumn<EventDTO, String> eventStatusCol;
    @FXML private TableColumn<EventDTO, String> commissionMethodCol;
    @FXML private TableColumn<EventDTO, String> eventMethodCol;

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

        eventIdCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().id())); // or .id() if record

        eventNameCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().name()));

        eventStatusCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().isActive() ? "Active" : "Not Active"));

        eventMethodCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().tradingMethod()));

        commissionMethodCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().commissionType()));
    }

    public void bindEventsList(ObservableList<EventDTO> sharedEventsList) {
        eventsTableView.setItems(sharedEventsList);
    }
}