package com.guessmarket.client.controllers;

import com.guessmarket.dto.CommissionType;
import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.EventDetailsDTO;
import com.guessmarket.dto.EventStatus;
import com.guessmarket.dto.HoldingDTO;
import com.guessmarket.dto.LimitOrderDTO;
import com.guessmarket.dto.OrderBookLevelDTO;
import com.guessmarket.dto.OrderBookQuoteDTO;
import com.guessmarket.dto.OrderResultDTO;
import com.guessmarket.dto.OrderSide;
import com.guessmarket.dto.TradeHistoryDTO;
import com.guessmarket.dto.TradeQuoteDTO;
import com.guessmarket.dto.TradingMethodType;
import com.guessmarket.dto.UserDTO;
import com.guessmarket.engine.api.MarketDataChangeListener;
import com.guessmarket.engine.api.MarketEngine;
import com.guessmarket.client.common.Charts;
import com.guessmarket.client.common.Dialogs;
import com.guessmarket.client.common.EventFilters;
import com.guessmarket.client.common.TradeRules;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Controller for the Events tab: a filterable events list, and for the
 * selected event both its read-only details (order-book depth, participant
 * holdings, price graphs) and every action on it - activate/close for its
 * market maker, buy/place/cancel orders for anyone else. This client
 * represents exactly one logged-in identity (see {@link #setContext}), so
 * every action here always acts as that fixed user; there is no "act as"
 * picker.
 */
public class EventsController implements MarketDataChangeListener {

    private static final Logger LOG = LogManager.getLogger(EventsController.class);

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
    // FXML UI Controls - Financials & Market-Maker Control
    // =========================================================================
    @FXML private Label eventBalanceLabel;
    @FXML private Label eventCommissionLabel;
    @FXML private Label eventDescriptionLabel;
    @FXML private Button activateEventButton;
    @FXML private ComboBox<String> winningOptionComboBox;
    @FXML private Button endEventButton;

    // =========================================================================
    // FXML UI Controls - Purchase History
    // =========================================================================
    @FXML private TableView<TradeHistoryDTO> tradeHistoryTableView;
    @FXML private TableColumn<TradeHistoryDTO, String> tradeDateCol;
    @FXML private TableColumn<TradeHistoryDTO, String> tradeUserCol;
    @FXML private TableColumn<TradeHistoryDTO, String> tradeOptionCol;
    @FXML private TableColumn<TradeHistoryDTO, Integer> tradeSharesCol;
    @FXML private TableColumn<TradeHistoryDTO, String> tradePriceCol;

    // --- LMSR trade section (shown only for LMSR events) ---
    @FXML private ComboBox<String> tradeOptionComboBox;
    @FXML private Label optionPriceLabel;
    @FXML private Spinner<Integer> sharesCountSpinner;
    @FXML private VBox buySharesSection;
    @FXML private Label totalSharesPrice;
    @FXML private Label commissionToPayLabel;
    @FXML private Label payNowOrLaterLabel;

    // =========================================================================
    // FXML UI Controls - Order Book Limit-Order Entry (Order Book events only)
    // =========================================================================
    @FXML private VBox orderEntrySection;
    @FXML private ComboBox<String> orderOptionComboBox;
    @FXML private RadioButton bidRadioButton;
    @FXML private RadioButton askRadioButton;
    @FXML private TextField orderPriceField;
    @FXML private Spinner<Integer> orderQuantitySpinner;
    @FXML private Label orderEntryStatusLabel;
    @FXML private TableView<LimitOrderDTO> myOpenOrdersTable;
    @FXML private TableColumn<LimitOrderDTO, String> myOrderOptionCol;
    @FXML private TableColumn<LimitOrderDTO, String> myOrderSideCol;
    @FXML private TableColumn<LimitOrderDTO, String> myOrderPriceCol;
    @FXML private TableColumn<LimitOrderDTO, Integer> myOrderRemainingCol;

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
    /** The one identity this client acts as for its whole run - set once, at login. */
    private String currentUserName;
    /** Refreshed on every poll tick from the engine; {@code null} until the first tick lands. */
    private final ObjectProperty<UserDTO> myUser = new SimpleObjectProperty<>();

    private final ObservableList<EventDTO> eventsList = FXCollections.observableArrayList();
    private final FilteredList<EventDTO> filteredEvents = new FilteredList<>(eventsList);
    private final ObservableList<HoldingDTO> participationList = FXCollections.observableArrayList();
    private final ObservableList<OrderBookLevelDTO> option1Levels = FXCollections.observableArrayList();
    private final ObservableList<OrderBookLevelDTO> option2Levels = FXCollections.observableArrayList();
    private final ObservableList<String> availableOptionsList = FXCollections.observableArrayList();
    private final ObservableList<TradeHistoryDTO> tradeHistoryList = FXCollections.observableArrayList();
    private final ObservableList<LimitOrderDTO> myOpenOrdersList = FXCollections.observableArrayList();

    private final ObjectProperty<EventDetailsDTO> selectedEventDetails = new SimpleObjectProperty<>();
    private final IntegerProperty selectedSharesProperty = new SimpleIntegerProperty(1);

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
        tradeOptionComboBox.setItems(availableOptionsList);
        orderOptionComboBox.setItems(availableOptionsList);
        winningOptionComboBox.setItems(availableOptionsList);
        tradeHistoryTableView.setItems(tradeHistoryList);
        myOpenOrdersTable.setItems(myOpenOrdersList);

        setupEventsTableColumns();
        setupParticipationColumns();
        setupOrderBookColumns();
        setupPurchaseHistoryColumns();
        setupMyOpenOrdersColumns();
        setupFilters();
        setupTradingBindings();
        setupSpinners();

        eventsTableView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> handleEventSelected(newSelection));
    }

    private void setupEventsTableColumns() {
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

    private void setupPurchaseHistoryColumns() {
        tradeDateCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().timestamp()));
        tradeUserCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().buyerName()));
        tradeOptionCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().optionName()));
        tradeSharesCol.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().quantity()));
        tradePriceCol.setCellValueFactory(cellData -> {
            Double price = cellData.getValue().pricePaid();
            return new SimpleStringProperty(String.format("$%.2f", price));
        });
    }

    private void setupMyOpenOrdersColumns() {
        myOrderOptionCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().optionName()));
        myOrderSideCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().side() == OrderSide.BID ? "Bid" : "Ask"));
        myOrderPriceCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("$%.2f", cellData.getValue().price())));
        myOrderRemainingCol.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().remaining()));
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
    // Trading bindings (financials, activate/close, LMSR buy, Order Book entry)
    // =========================================================================
    private void setupTradingBindings() {
        eventBalanceLabel.textProperty().bind(
                selectedEventDetails
                        .map(EventDetailsDTO::eventAccountBalance)
                        .map(balance -> String.format("$%.2f", balance))
                        .orElse("$0.00")
        );

        eventCommissionLabel.textProperty().bind(
                selectedEventDetails
                        .map(EventDetailsDTO::totalCommissionCollected)
                        .map(balance -> String.format("$%,.2f", balance))
                        .orElse("$0.00")
        );

        eventDescriptionLabel.textProperty().bind(
                selectedEventDetails
                        .map(EventDetailsDTO::eventInfo)
                        .map(EventDTO::description)
        );

        selectedEventDetails.addListener((obs, oldDetails, newDetails) -> {
            tradeOptionComboBox.getSelectionModel().clearSelection();
            orderOptionComboBox.getSelectionModel().clearSelection();

            if (newDetails != null && newDetails.eventInfo() != null && newDetails.eventInfo().options() != null) {
                availableOptionsList.setAll(newDetails.eventInfo().options());
                if (!availableOptionsList.isEmpty()) {
                    tradeOptionComboBox.getSelectionModel().selectFirst();
                    orderOptionComboBox.getSelectionModel().selectFirst();
                }
            } else {
                availableOptionsList.clear();
            }

            if (newDetails != null && newDetails.tradeHistory() != null) {
                tradeHistoryList.setAll(newDetails.tradeHistory());
            } else {
                tradeHistoryList.clear();
            }

            orderEntryStatusLabel.setText(" ");
            refreshMyOpenOrders();
        });

        var selectedEventProperty = eventsTableView.getSelectionModel().selectedItemProperty();

        // The LMSR trade section is shown only for an LMSR event, and within
        // that is enabled only for a non-market-maker on an open event.
        bindVisibleToMethod(buySharesSection, TradingMethodType.LMSR);
        buySharesSection.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> !TradeRules.canTrade(myUser.get(), selectedEventProperty.get()),
                        myUser, selectedEventProperty)
        );

        // The activate button is enabled only for the market maker of a not-yet-open event.
        activateEventButton.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> !TradeRules.canActivate(myUser.get(), selectedEventProperty.get()),
                        myUser, selectedEventProperty)
        );

        // The end event button is enabled only for the market maker of an open event.
        endEventButton.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> !TradeRules.canEnd(myUser.get(), selectedEventProperty.get()),
                        myUser, selectedEventProperty)
        );

        // The winning option combobox is enabled only for the market maker of an open event.
        winningOptionComboBox.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> !TradeRules.canEnd(myUser.get(), selectedEventProperty.get()),
                        myUser, selectedEventProperty)
        );

        optionPriceLabel.textProperty().bind(
                Bindings.createStringBinding(() -> {
                    EventDetailsDTO details = selectedEventDetails.get();
                    String selectedOption = tradeOptionComboBox.getValue();

                    if (details != null && selectedOption != null && details.currentOptionPrices() != null) {
                        Double price = details.currentOptionPrices().get(selectedOption);
                        if (price != null) {
                            return String.format("$%.2f", price);
                        }
                    }
                    return "$0.00";
                }, selectedEventDetails, tradeOptionComboBox.valueProperty())
        );

        // Total and commission come straight from the engine's quote, so the
        // numbers shown here are exactly what buyShares() will charge.
        totalSharesPrice.textProperty().bind(
                Bindings.createStringBinding(() -> {
                    TradeQuoteDTO q = currentQuote();
                    return q == null ? "$0.00" : String.format("$%.2f", q.total());
                }, selectedEventDetails, selectedEventProperty,
                   tradeOptionComboBox.valueProperty(), selectedSharesProperty)
        );

        commissionToPayLabel.textProperty().bind(
                Bindings.createStringBinding(() -> {
                    TradeQuoteDTO q = currentQuote();
                    return q == null ? "$0.00" : String.format("$%.2f", q.commission());
                }, selectedEventDetails, selectedEventProperty,
                   tradeOptionComboBox.valueProperty(), selectedSharesProperty)
        );

        payNowOrLaterLabel.textProperty().bind(
                Bindings.createStringBinding(() -> {
                    EventDetailsDTO details = selectedEventDetails.get();
                    return details == null || details.eventInfo() == null
                            ? ""
                            : TradeRules.payTimingLabel(details.eventInfo().commissionType());
                }, selectedEventDetails)
        );

        bindVisibleToMethod(orderEntrySection, TradingMethodType.ORDERBOOK);
        // Same rule as the LMSR trade panel: a non-market-maker on an open event,
        // and never a blocked user.
        orderEntrySection.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> !TradeRules.canTrade(myUser.get(), selectedEventProperty.get()),
                        myUser, selectedEventProperty)
        );
    }

    /**
     * Binds a section's {@code visible} (and {@code managed}) property so it
     * shows only while the selected event trades through {@code method}.
     */
    private void bindVisibleToMethod(javafx.scene.Node section, TradingMethodType method) {
        section.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> {
                    EventDetailsDTO details = selectedEventDetails.get();
                    return details != null && details.eventInfo() != null
                            && details.eventInfo().tradingMethod() == method;
                },
                selectedEventDetails));
        section.managedProperty().bind(section.visibleProperty());
    }

    private void setupSpinners() {
        sharesCountSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1_000_000, 1, 1));
        sharesCountSpinner.setEditable(true);
        sharesCountSpinner.getEditor().setOnAction(event -> commitEditorText(sharesCountSpinner));
        sharesCountSpinner.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                commitEditorText(sharesCountSpinner);
            }
        });
        sharesCountSpinner.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                selectedSharesProperty.set(newValue);
            }
        });

        orderQuantitySpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1_000_000, 1, 1));
        orderQuantitySpinner.setEditable(true);
        orderQuantitySpinner.getEditor().setOnAction(event -> commitEditorText(orderQuantitySpinner));
        orderQuantitySpinner.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                commitEditorText(orderQuantitySpinner);
            }
        });
    }

    /** Safely parses and commits typed text into the given spinner's value factory. */
    private void commitEditorText(Spinner<Integer> spinner) {
        String text = spinner.getEditor().getText();
        SpinnerValueFactory<Integer> factory = spinner.getValueFactory();
        if (factory != null && factory.getConverter() != null) {
            try {
                Integer value = factory.getConverter().fromString(text);
                factory.setValue(value);
            } catch (Exception ex) {
                // Revert invalid text (e.g., non-numeric) back to current factory value
                spinner.getEditor().setText(factory.getConverter().toString(factory.getValue()));
            }
        }
    }

    /**
     * The engine's quote for the trade the panel currently describes, or
     * {@code null} if the selection is incomplete or the event cannot be priced.
     */
    private TradeQuoteDTO currentQuote() {
        EventDTO event = eventsTableView.getSelectionModel().getSelectedItem();
        String option = tradeOptionComboBox.getValue();
        int shares = selectedSharesProperty.get();
        if (marketEngine == null || currentUserName == null || event == null || option == null || shares <= 0) {
            return null;
        }
        int optionIndex1Based = tradeOptionComboBox.getItems().indexOf(option) + 1;
        if (optionIndex1Based < 1) {
            return null;
        }
        try {
            return marketEngine.quoteTrade(currentUserName, event.name(), optionIndex1Based, shares);
        } catch (RuntimeException notQuotable) {
            return null;
        }
    }

    /** My resting orders on the currently selected event. */
    private void refreshMyOpenOrders() {
        EventDetailsDTO details = selectedEventDetails.get();
        if (details == null || currentUserName == null || details.restingOrders() == null) {
            myOpenOrdersList.clear();
            return;
        }
        myOpenOrdersList.setAll(details.restingOrders().stream()
                .filter(order -> order.userName().equals(currentUserName))
                .toList());
    }

    // =========================================================================
    // Dependency Injection & Engine Listener
    // =========================================================================
    /**
     * Injects the shared MarketEngine instance and the identity this client is
     * logged in as, registers this controller as a listener for state changes,
     * and initiates the initial data pull.
     */
    public void setContext(MarketEngine marketEngine, String currentUserName) {
        this.marketEngine = marketEngine;
        this.currentUserName = currentUserName;
        this.marketEngine.addListener(this);
        onMarketDataChanged();
    }

    @Override
    public void onMarketDataChanged() {
        if (marketEngine == null) return;

        Platform.runLater(() -> {
            EventDTO currentSelectedEvent = eventsTableView.getSelectionModel().getSelectedItem();

            myUser.set(marketEngine.getAllUsers().get(currentUserName));
            eventsList.setAll(marketEngine.getAllEvents());   // FilteredList re-applies its predicate

            if (currentSelectedEvent != null) {
                eventsTableView.getItems().stream()
                        .filter(e -> e.name().equals(currentSelectedEvent.name()))
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
            participationList.clear();
            option1Levels.clear();
            option2Levels.clear();
            orderBookSection.setVisible(false);
            orderBookSection.setManaged(false);
            showClosedSummary(null, null);
            clearGraphs();
            selectedEventDetails.set(null);
            return;
        }

        EventDetailsDTO details = marketEngine.getEventDetails(selectedEvent.name());
        participationList.setAll(details.participantHoldings());
        updateGraphs(selectedEvent, details);
        showClosedSummary(selectedEvent, details);
        selectedEventDetails.set(details);

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

    // =========================================================================
    // Trading & Market-Maker Actions
    // =========================================================================
    @FXML
    private void handleExecuteTrade() {
        commitEditorText(sharesCountSpinner);

        EventDTO selectedEvent = eventsTableView.getSelectionModel().getSelectedItem();
        int selectedOption = tradeOptionComboBox.getSelectionModel().getSelectedIndex() + 1;
        int shares = selectedSharesProperty.get();

        if (selectedEvent == null || selectedOption == 0 || shares <= 0) {
            Dialogs.error("Invalid Selection", "Please select an active event, an option, and a valid quantity of shares.");
            return;
        }
        if (marketEngine == null) {
            Dialogs.error("Engine Error", "Market Engine is not initialized.");
            return;
        }

        LOG.info("Trade requested: user='{}', event={}, option#={}, shares={}",
                currentUserName, selectedEvent.name(), selectedOption, shares);
        try {
            marketEngine.buyShares(currentUserName, selectedEvent.name(), selectedOption, shares);
            sharesCountSpinner.getValueFactory().setValue(1);
        } catch (Exception ex) {
            LOG.warn("Trade failed for user '{}' on event {}: {}",
                    currentUserName, selectedEvent.name(), ex.getMessage());
            Dialogs.error("Trade Execution Failed", ex);
        }
    }

    @FXML
    private void handleActivateEvent() {
        EventDTO selectedEvent = eventsTableView.getSelectionModel().getSelectedItem();
        if (selectedEvent == null || marketEngine == null) {
            Dialogs.error("Invalid Selection", "Select an event first.");
            return;
        }
        // The engine enforces that I am the event's market maker and can fund
        // the subsidy; surface whatever it rejects.
        LOG.info("Activate event {} requested by '{}'", selectedEvent.name(), currentUserName);
        try {
            marketEngine.activateEvent(selectedEvent.name(), currentUserName);
        } catch (RuntimeException ex) {
            LOG.warn("Activate event {} failed: {}", selectedEvent.name(), ex.getMessage());
            Dialogs.error("Could not activate event", ex);
        }
    }

    @FXML
    private void handleEndEvent() {
        EventDTO selectedEvent = eventsTableView.getSelectionModel().getSelectedItem();
        int selectedOption = winningOptionComboBox.getSelectionModel().getSelectedIndex() + 1;
        if (selectedEvent == null || marketEngine == null) {
            Dialogs.error("Invalid Selection", "Select an event first.");
            return;
        }
        LOG.info("End event {} requested by '{}' with winning option #{}",
                selectedEvent.name(), currentUserName, selectedOption);
        try {
            marketEngine.closeEvent(selectedEvent.name(), selectedOption);
        } catch (RuntimeException ex) {
            LOG.warn("End event {} failed: {}", selectedEvent.name(), ex.getMessage());
            Dialogs.error("Could not close event", ex);
        }
    }

    @FXML
    private void handlePlaceOrder() {
        commitEditorText(orderQuantitySpinner);

        EventDTO selectedEvent = eventsTableView.getSelectionModel().getSelectedItem();
        String selectedOption = orderOptionComboBox.getValue();
        int optionIndex1Based = orderOptionComboBox.getItems().indexOf(selectedOption) + 1;
        int quantity = orderQuantitySpinner.getValue() == null ? 0 : orderQuantitySpinner.getValue();
        OrderSide side = bidRadioButton.isSelected() ? OrderSide.BID : OrderSide.ASK;

        if (selectedEvent == null || optionIndex1Based == 0 || quantity <= 0 || marketEngine == null) {
            Dialogs.error("Invalid Selection", "Select an active Order Book event, an option, and a valid quantity.");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(orderPriceField.getText().trim());
        } catch (NumberFormatException | NullPointerException ex) {
            Dialogs.error("Invalid Price", "Enter a numeric price.");
            return;
        }

        LOG.info("Order requested: user='{}', event={}, option#={}, side={}, qty={}, price={}",
                currentUserName, selectedEvent.name(), optionIndex1Based, side, quantity, price);
        try {
            OrderResultDTO result = marketEngine.placeOrder(
                    currentUserName, selectedEvent.name(), optionIndex1Based, side, quantity, price);
            orderEntryStatusLabel.setText(String.format(
                    "Filled %d, resting %d.", result.filledQuantity(), result.restingQuantity()));
        } catch (RuntimeException ex) {
            LOG.warn("Place order failed for user '{}' on event {}: {}",
                    currentUserName, selectedEvent.name(), ex.getMessage());
            Dialogs.error("Order Failed", ex);
        }
    }

    @FXML
    private void handleCancelOrder() {
        EventDTO selectedEvent = eventsTableView.getSelectionModel().getSelectedItem();
        LimitOrderDTO selectedOrder = myOpenOrdersTable.getSelectionModel().getSelectedItem();

        if (selectedEvent == null || selectedOrder == null || marketEngine == null) {
            Dialogs.error("Invalid Selection", "Select one of your open orders to cancel.");
            return;
        }

        LOG.info("Cancel order {} requested by '{}' on event {}", selectedOrder.id(), currentUserName, selectedEvent.name());
        try {
            marketEngine.cancelOrder(currentUserName, selectedEvent.name(), selectedOrder.id());
        } catch (RuntimeException ex) {
            LOG.warn("Cancel order {} failed: {}", selectedOrder.id(), ex.getMessage());
            Dialogs.error("Cancel Failed", ex);
        }
    }
}
