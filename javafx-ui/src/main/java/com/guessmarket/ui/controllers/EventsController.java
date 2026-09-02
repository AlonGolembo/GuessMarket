package com.guessmarket.ui.controllers;

import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.HoldingDTO;
import com.guessmarket.engine.api.MarketDataChangeListener;
import com.guessmarket.engine.api.MarketEngine;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

/**
 * Controller for the Events Tab: the events list, and for the selected event its
 * order books (not yet implemented) and participant holdings.
 */
public class EventsController implements MarketDataChangeListener {

    // =========================================================================
    // FXML UI Controls - Filter Section
    // =========================================================================
    @FXML private ComboBox<String> methodFilterComboBox;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private ComboBox<String> commissionFilterComboBox;

    // =========================================================================
    // FXML UI Controls - Main Events Table
    // =========================================================================
    @FXML private TableView<EventDTO> eventsTableView;
    @FXML private TableColumn<EventDTO, Integer> eventIdCol;
    @FXML private TableColumn<EventDTO, String> eventNameCol;
    @FXML private TableColumn<EventDTO, String> eventStatusCol;
    @FXML private TableColumn<EventDTO, String> commissionMethodCol;
    @FXML private TableColumn<EventDTO, String> eventMethodCol;

    // =========================================================================
    // FXML UI Controls - Event Details & Sub-Tables
    // =========================================================================
    @FXML private VBox eventTradeDetailsContainer;

    @FXML private TableView<?> option1OrderBookTable;
    @FXML private TableColumn<?, ?> opt1PriceCol;
    @FXML private TableColumn<?, ?> opt1AmountCol;

    @FXML private TableView<?> option2OrderBookTable;
    @FXML private TableColumn<?, ?> opt2PriceCol;
    @FXML private TableColumn<?, ?> opt2AmountCol;

    @FXML private TableView<HoldingDTO> participationsTableView;
    @FXML private TableColumn<HoldingDTO, String> partUserCol;
    @FXML private TableColumn<HoldingDTO, String> partOptionCol;
    @FXML private TableColumn<HoldingDTO, Integer> partSharesCol;

    // =========================================================================
    // Controller State & Observable Collections
    // =========================================================================
    private MarketEngine marketEngine;
    private final ObservableList<EventDTO> eventsList = FXCollections.observableArrayList();
    private final ObservableList<HoldingDTO> participationsList = FXCollections.observableArrayList();

    // =========================================================================
    // Lifecycle & Initialization
    // =========================================================================
    @FXML
    private void initialize() {
        eventsTableView.setItems(eventsList);
        participationsTableView.setItems(participationsList);
        setupEventsTableColumns();
        setupParticipationsColumns();
        eventsTableView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> handleEventSelected(newSelection));
    }

    private void setupEventsTableColumns() {
        eventIdCol.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().id()));
        eventNameCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().name()));
        eventStatusCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().status().name()));
        eventMethodCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().tradingMethod().name()));
        commissionMethodCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().commissionType().name()));
    }

    private void setupParticipationsColumns() {
        partUserCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().userName()));
        partOptionCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().optionName()));
        partSharesCol.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().shares()));
    }

    // =========================================================================
    // Dependency Injection & Engine Listener
    // =========================================================================
    public void setEngine(MarketEngine marketEngine) {
        this.marketEngine = marketEngine;
        this.marketEngine.addListener(this);
        onMarketDataChanged();
    }

    @Override
    public void onMarketDataChanged() {
        if (marketEngine == null) return;

        Platform.runLater(() -> {
            EventDTO currentSelectedEvent = eventsTableView.getSelectionModel().getSelectedItem();
            eventsList.setAll(marketEngine.getAllEvents());

            if (currentSelectedEvent != null) {
                eventsList.stream()
                        .filter(e -> e.id() == currentSelectedEvent.id())
                        .findFirst()
                        .ifPresentOrElse(
                                e -> {
                                    eventsTableView.getSelectionModel().select(e);
                                    handleEventSelected(e);
                                },
                                () -> handleEventSelected(null));
            } else {
                handleEventSelected(null);
            }
        });
    }

    // =========================================================================
    // Selection Handlers
    // =========================================================================
    private void handleEventSelected(EventDTO selectedEvent) {
        if (eventTradeDetailsContainer != null) {
            eventTradeDetailsContainer.setVisible(selectedEvent != null);
        }
        if (selectedEvent == null || marketEngine == null) {
            participationsList.clear();
            return;
        }
        participationsList.setAll(marketEngine.getEventDetails(selectedEvent.id()).participantHoldings());
    }
}