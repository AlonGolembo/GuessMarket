package com.guessmarket.ui.controllers;

import com.guessmarket.dto.CommissionType;
import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.EventDetailsDTO;
import com.guessmarket.dto.EventStatus;
import com.guessmarket.dto.HoldingDTO;
import com.guessmarket.dto.OrderBookLevelDTO;
import com.guessmarket.dto.OrderBookQuoteDTO;
import com.guessmarket.dto.OrderSide;
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
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the Events Tab: a filterable events list, and for the selected
 * event its order-book depth (Order Book events only) and participant holdings.
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

    @FXML private VBox orderBookSection;
    @FXML private Label option1BookLabel;
    @FXML private Label option2BookLabel;
    @FXML private Label option1IndicatorsLabel;
    @FXML private Label option2IndicatorsLabel;

    @FXML private TableView<OrderBookLevelDTO> option1OrderBookTable;
    @FXML private TableColumn<OrderBookLevelDTO, String> opt1SideCol;
    @FXML private TableColumn<OrderBookLevelDTO, String> opt1PriceCol;
    @FXML private TableColumn<OrderBookLevelDTO, Integer> opt1AmountCol;

    @FXML private TableView<OrderBookLevelDTO> option2OrderBookTable;
    @FXML private TableColumn<OrderBookLevelDTO, String> opt2SideCol;
    @FXML private TableColumn<OrderBookLevelDTO, String> opt2PriceCol;
    @FXML private TableColumn<OrderBookLevelDTO, Integer> opt2AmountCol;

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
    private final ObservableList<OrderBookLevelDTO> option1Levels = FXCollections.observableArrayList();
    private final ObservableList<OrderBookLevelDTO> option2Levels = FXCollections.observableArrayList();

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
        option1OrderBookTable.setItems(option1Levels);
        option2OrderBookTable.setItems(option2Levels);

        setupEventsTableColumns();
        setupParticipationsColumns();
        setupOrderBookColumns();
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

    private void setupOrderBookColumns() {
        setupOrderBookColumns(opt1SideCol, opt1PriceCol, opt1AmountCol);
        setupOrderBookColumns(opt2SideCol, opt2PriceCol, opt2AmountCol);
    }

    private static void setupOrderBookColumns(TableColumn<OrderBookLevelDTO, String> sideCol,
                                               TableColumn<OrderBookLevelDTO, String> priceCol,
                                               TableColumn<OrderBookLevelDTO, Integer> amountCol) {
        sideCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().side() == OrderSide.BID ? "Bid" : "Ask"));
        priceCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("$%.2f", cellData.getValue().price())));
        amountCol.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().quantity()));
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
            option1Levels.clear();
            option2Levels.clear();
            orderBookSection.setVisible(false);
            orderBookSection.setManaged(false);
            return;
        }

        EventDetailsDTO details = marketEngine.getEventDetails(selectedEvent.id());
        participationsList.setAll(details.participantHoldings());

        boolean isOrderBook = selectedEvent.tradingMethod() == TradingMethodType.ORDERBOOK;
        orderBookSection.setVisible(isOrderBook);
        orderBookSection.setManaged(isOrderBook);
        if (isOrderBook && selectedEvent.options().size() == 2) {
            String option1 = selectedEvent.options().get(0);
            String option2 = selectedEvent.options().get(1);
            option1BookLabel.setText(option1 + " Order Book");
            option2BookLabel.setText(option2 + " Order Book");
            option1Levels.setAll(levelsFor(details, option1));
            option2Levels.setAll(levelsFor(details, option2));
            option1IndicatorsLabel.setText(indicatorsText(details, option1));
            option2IndicatorsLabel.setText(indicatorsText(details, option2));
        } else {
            option1Levels.clear();
            option2Levels.clear();
        }
    }

    private static List<OrderBookLevelDTO> levelsFor(EventDetailsDTO details, String optionName) {
        return details.orderBookLevels().stream()
                .filter(level -> level.optionName().equals(optionName))
                .toList();
    }

    /** Renders the five order-book indicators for one option; {@code null} fields show as "-". */
    private static String indicatorsText(EventDetailsDTO details, String optionName) {
        OrderBookQuoteDTO quote = details.orderBookQuotes().get(optionName);
        if (quote == null) {
            return "Last: - | Bid: - | Ask: - | Mid: - | Spread: -";
        }
        return String.format("Last: %s | Bid: %s | Ask: %s | Mid: %s | Spread: %s",
                money(quote.lastTrade()), money(quote.bestBid()), money(quote.bestAsk()),
                money(quote.mid()), money(quote.spread()));
    }

    private static String money(Double value) {
        return value == null ? "-" : String.format("$%.2f", value);
    }
}
