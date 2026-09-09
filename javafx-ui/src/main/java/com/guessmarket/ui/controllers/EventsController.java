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
import com.guessmarket.ui.common.AnimationSettings;
import com.guessmarket.ui.common.Charts;
import com.guessmarket.ui.common.EventFilters;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
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
import javafx.scene.chart.LineChart;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

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
    @FXML private Label closedSummaryLabel;

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
    // Graphs Tab
    // =========================================================================

    @FXML private Label leftGraphLabel;
    @FXML private Label rightGraphLabel;
    @FXML private AnchorPane leftGraph;
    @FXML private AnchorPane rightGraph;

    // =========================================================================
    // Controller State & Observable Collections
    // =========================================================================
    private MarketEngine marketEngine;
    private final ObservableList<EventDTO> eventsList = FXCollections.observableArrayList();
    private final FilteredList<EventDTO> filteredEvents = new FilteredList<>(eventsList);
    private final ObservableList<HoldingDTO> participationList = FXCollections.observableArrayList();
    private final ObservableList<OrderBookLevelDTO> option1Levels = FXCollections.observableArrayList();
    private final ObservableList<OrderBookLevelDTO> option2Levels = FXCollections.observableArrayList();

    // Trade-panel reveal animation (fade + slide-up when a new event is selected)
    private ParallelTransition detailReveal;
    private Integer lastRevealedEventId;

    // =========================================================================
    // Lifecycle & Initialization
    // =========================================================================
    @FXML
    private void initialize() {
        // FilteredList (predicate) -> SortedList (header clicks) -> table.
        SortedList<EventDTO> sortedEvents = new SortedList<>(filteredEvents);
        sortedEvents.comparatorProperty().bind(eventsTableView.comparatorProperty());
        eventsTableView.setItems(sortedEvents);
        participationsTableView.setItems(participationList);
        option1OrderBookTable.setItems(option1Levels);
        option2OrderBookTable.setItems(option2Levels);

        setupEventsTableColumns();
        setupParticipationColumns();
        setupOrderBookColumns();
        setupFilters();
        setupDetailReveal();

        eventsTableView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> handleEventSelected(newSelection));
    }

    /** Builds the reusable fade + slide-up transition played on the Trade panel. */
    private void setupDetailReveal() {
        if (eventTradeDetailsContainer == null) {
            return;
        }
        FadeTransition fade = new FadeTransition(Duration.millis(220), eventTradeDetailsContainer);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        TranslateTransition slide = new TranslateTransition(Duration.millis(260), eventTradeDetailsContainer);
        slide.setFromY(14.0);
        slide.setToY(0.0);
        slide.setInterpolator(Interpolator.EASE_OUT);
        detailReveal = new ParallelTransition(fade, slide);
    }

    /** Plays the reveal only when the selection actually changed to a different event. */
    private void revealDetailPanel(EventDTO selectedEvent) {
        if (eventTradeDetailsContainer == null) {
            return;
        }
        if (selectedEvent == null) {
            lastRevealedEventId = null;
            return;
        }
        boolean newSelection = lastRevealedEventId == null || lastRevealedEventId != selectedEvent.id();
        lastRevealedEventId = selectedEvent.id();
        if (newSelection && detailReveal != null && AnimationSettings.isEventPanelRevealEnabled()) {
            detailReveal.stop();
            eventTradeDetailsContainer.setOpacity(0.0);
            eventTradeDetailsContainer.setTranslateY(14.0);
            detailReveal.playFromStart();
        } else {
            // No animation this time - make sure an interrupted run left nothing behind.
            eventTradeDetailsContainer.setOpacity(1.0);
            eventTradeDetailsContainer.setTranslateY(0.0);
        }
    }

    private void setupEventsTableColumns() {
        eventIdCol.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().id()));
        eventNameCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().name()));
        eventStatusCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().status().toUIDisplay()));
        eventMethodCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().tradingMethod().name()));
        commissionMethodCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().commissionType().toUIDisplay()));
    }

    private void setupParticipationColumns() {
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
        if (marketEngine == null || !marketEngine.isFileLoaded()) return;

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
        revealDetailPanel(selectedEvent);
        if (selectedEvent == null || marketEngine == null) {
            participationList.clear();
            option1Levels.clear();
            option2Levels.clear();
            orderBookSection.setVisible(false);
            orderBookSection.setManaged(false);
            showClosedSummary(null, null);
            clearGraphs();
            return;
        }

        EventDetailsDTO details = marketEngine.getEventDetails(selectedEvent.id());
        participationList.setAll(details.participantHoldings());
        updateGraphs(selectedEvent, details);
        showClosedSummary(selectedEvent, details);

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

    /**
     * For a CLOSED event, shows the winning option and the total shares bought per
     * option; hidden otherwise.
     */
    private void showClosedSummary(EventDTO event, EventDetailsDTO details) {
        boolean closed = event != null && event.status() == EventStatus.CLOSED && details != null;
        closedSummaryLabel.setVisible(closed);
        closedSummaryLabel.setManaged(closed);
        if (!closed) {
            return;
        }
        StringBuilder sb = new StringBuilder("Event closed  •  Winner: ")
                .append(details.winningOption() == null ? "-" : details.winningOption());
        sb.append("  •  Total shares bought:");
        for (String option : event.options()) {
            int total = details.totalSharesBought() == null
                    ? 0 : details.totalSharesBought().getOrDefault(option, 0);
            sb.append("  ").append(option).append(" = ").append(total);
        }
        closedSummaryLabel.setText(sb.toString());
    }

    // =========================================================================
    // Graphs Tab - one price-over-time chart per option
    // =========================================================================

    /** Renders a price-history chart for each of the event's options. */
    private void updateGraphs(EventDTO event, EventDetailsDTO details) {
        List<String> options = event.options();
        if (options.size() < 2) {
            clearGraphs();
            return;
        }
        renderGraph(leftGraph, leftGraphLabel, options.get(0), details);
        renderGraph(rightGraph, rightGraphLabel, options.get(1), details);
    }

    private static void renderGraph(AnchorPane host, Label label, String optionName, EventDetailsDTO details) {
        label.setText(optionName + " Price");
        LineChart<String, Number> chart = Charts.createPriceHistoryChart(optionName, details);
        AnchorPane.setTopAnchor(chart, 0.0);
        AnchorPane.setRightAnchor(chart, 0.0);
        AnchorPane.setBottomAnchor(chart, 0.0);
        AnchorPane.setLeftAnchor(chart, 0.0);
        host.getChildren().setAll(chart);
    }

    private void clearGraphs() {
        leftGraph.getChildren().clear();
        rightGraph.getChildren().clear();
        leftGraphLabel.setText("Option 1 Price");
        rightGraphLabel.setText("Option 2 Price");
    }

    private static List<OrderBookLevelDTO> levelsFor(EventDetailsDTO details, String optionName) {
        return details.orderBookLevels().stream()
                .filter(level -> level.optionName().equals(optionName))
                .toList();
    }

    /** Renders the five order-book indicators for one option; {@code null} fields show as ""-". */
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
