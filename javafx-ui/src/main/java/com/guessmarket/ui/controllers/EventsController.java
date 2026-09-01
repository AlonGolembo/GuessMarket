package com.guessmarket.ui.controllers;

import com.guessmarket.dto.EventDTO;
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
 * Controller for the Events Tab, displaying all market events,
 * their details, order books, and participant allocations.
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

    @FXML private TableView<?> participationsTableView;
    @FXML private TableColumn<?, ?> partUserCol;
    @FXML private TableColumn<?, ?> partOptionCol;
    @FXML private TableColumn<?, ?> partSharesCol;

    // =========================================================================
    // Controller State & Observable Collections
    // =========================================================================
    private MarketEngine marketEngine;
    private final ObservableList<EventDTO> eventsList = FXCollections.observableArrayList();

    // =========================================================================
    // Lifecycle & Initialization
    // =========================================================================
    @FXML
    private void initialize() {
        bindCollectionsToControls();
        setupEventsTableColumns();
        setupListeners();
    }

    /**
     * Binds internal observable collections to UI controls.
     */
    private void bindCollectionsToControls() {
        eventsTableView.setItems(eventsList);
    }

    /**
     * Configures cell value factories for the main Events table.
     */
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

    /**
     * Attaches selection listeners to the main events table.
     */
    private void setupListeners() {
        eventsTableView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> handleEventSelected(newSelection)
        );
    }

    // =========================================================================
    // Dependency Injection & Engine Listener
    // =========================================================================
    /**
     * Injects the shared MarketEngine instance, registers this controller
     * as a listener for state changes, and initiates the initial data pull.
     */
    public void setEngine(MarketEngine marketEngine) {
        this.marketEngine = marketEngine;
        this.marketEngine.addListener(this);
        onMarketDataChanged();
    }

    /**
     * Callback triggered whenever the MarketEngine mutates state (XML load, trade execution, etc.).
     * Refreshes the events list while preserving the user's active table selection.
     */
    @Override
    public void onMarketDataChanged() {
        if (marketEngine == null) return;

        // Ensure UI updates always run safely on the JavaFX Application Thread
        Platform.runLater(() -> {
            EventDTO currentSelectedEvent = eventsTableView.getSelectionModel().getSelectedItem();

            // Refresh master event list
            eventsList.setAll(marketEngine.getAllEvents());

            // Restore selection if the previously selected event still exists
            if (currentSelectedEvent != null) {
                eventsList.stream()
                        .filter(e -> e.id() == currentSelectedEvent.id())
                        .findFirst()
                        .ifPresent(e -> {
                            eventsTableView.getSelectionModel().select(e);
                            handleEventSelected(e);
                        });
            } else {
                handleEventSelected(null);
            }
        });
    }

    // =========================================================================
    // Selection Handlers
    // =========================================================================
    /**
     * Handles selection changes in the Events table to populate details,
     * order books, and participant tables.
     */
    private void handleEventSelected(EventDTO selectedEvent) {
        if (selectedEvent != null) {
            // Update order books and participant sub-tables based on selectedEvent
            if (eventTradeDetailsContainer != null) {
                eventTradeDetailsContainer.setVisible(true);
            }
        } else {
            if (eventTradeDetailsContainer != null) {
                eventTradeDetailsContainer.setVisible(false);
            }
        }
    }
}