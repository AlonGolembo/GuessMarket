package com.guessmarket.ui.controllers;

import com.guessmarket.dto.CommissionType;
import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.EventStatus;
import com.guessmarket.dto.HoldingDTO;
import com.guessmarket.dto.TradingMethodType;
import com.guessmarket.engine.api.MarketDataChangeListener;
import com.guessmarket.engine.api.MarketEngine;
import com.guessmarket.ui.common.EventFilters;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the Events Tab: a filterable events list, and for the selected
 * event its order books (not yet implemented) and participant holdings.
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
    private final FilteredList<EventDTO> filteredEvents = new FilteredList<>(eventsList);
    private final ObservableList<HoldingDTO> participationsList = FXCollections.observableArrayList();

    // =========================================================================
    // Lifecycle & Initialization
    // =========================================================================
    @FXML
    private void initialize() {
        // FilteredList (predicate) -> SortedList (header clicks) -> table.
        SortedList<EventDTO> sortedEvents = new SortedList<>(filteredEvents);
        sortedEvents.comparatorProperty().bind(eventsTableView.comparatorProperty());
        eventsTableView.setItems(sortedEvents);
        participationsTableView.setItems(participationsList);

        setupEventsTableColumns();
        setupParticipationsColumns();
        setupFilters();

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
    // Filters
    // =========================================================================
    private void setupFilters() {
        fillFilterCombo(methodFilterComboBox, TradingMethodType.LMSR.name(), TradingMethodType.ORDERBOOK.name());
        fillFilterCombo(statusFilterComboBox,
                EventStatus.NOT_ACTIVE.name(), EventStatus.ACTIVE.name(), EventStatus.CLOSED.name());
        fillFilterCombo(commissionFilterComboBox,
                CommissionType.ON_PURCHASE.name(), CommissionType.ON_CLOSE.name());

        methodFilterComboBox.valueProperty().addListener((obs, oldV, newV) -> applyFilter());
        statusFilterComboBox.valueProperty().addListener((obs, oldV, newV) -> applyFilter());
        commissionFilterComboBox.valueProperty().addListener((obs, oldV, newV) -> applyFilter());

        applyFilter();
    }

    /** Loads {@code "All"} plus the given values and selects {@code "All"}. */
    private static void fillFilterCombo(ComboBox<String> combo, String... values) {
        List<String> items = new ArrayList<>();
        items.add(EventFilters.NO_FILTER);
        items.addAll(List.of(values));
        combo.getItems().setAll(items);
        combo.getSelectionModel().select(EventFilters.NO_FILTER);
    }

    private void applyFilter() {
        filteredEvents.setPredicate(EventFilters.predicate(
                methodFilterComboBox.getValue(),
                statusFilterComboBox.getValue(),
                commissionFilterComboBox.getValue()));
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
            eventsList.setAll(marketEngine.getAllEvents());   // FilteredList re-applies its predicate

            if (currentSelectedEvent != null) {
                eventsTableView.getItems().stream()
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
