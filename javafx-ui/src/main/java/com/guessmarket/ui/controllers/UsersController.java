package com.guessmarket.ui.controllers;

import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.EventDetailsDTO;
import com.guessmarket.dto.TradeHistoryDTO;
import com.guessmarket.dto.TradeQuoteDTO;
import com.guessmarket.dto.UserDTO;
import com.guessmarket.engine.api.MarketDataChangeListener;
import com.guessmarket.engine.api.MarketEngine;
import com.guessmarket.ui.common.Dialogs;
import com.guessmarket.ui.common.TradeRules;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * Controller for the Users Tab, managing the list of all users,
 * displaying individual user details/balances, showing participating events,
 * and maintaining active event options for trading.
 */
public class UsersController implements MarketDataChangeListener {

    private static final Logger LOG = LogManager.getLogger(UsersController.class);

    // =========================================================================
    // FXML UI Controls - Main Users Table
    // =========================================================================
    @FXML private TableView<UserDTO> usersTableView;
    @FXML private TableColumn<UserDTO, String> userNameCol;
    @FXML private TableColumn<UserDTO, String> userBalanceCol;

    // =========================================================================
    // FXML UI Controls - Single User Details & Sub-Table
    // =========================================================================
    @FXML private Label accountBalanceLabel;
    @FXML private TableView<EventDTO> userEventsTableView;
    @FXML private TableColumn<EventDTO, String> userEventNameCol;
    @FXML private TableColumn<EventDTO, String> userEventRoleCol;
    @FXML private TableColumn<EventDTO, Integer> userEventSharesCol;

    // =========================================================================
    // FXML UI Controls - Event Trading Section
    // =========================================================================
    @FXML private ComboBox<EventDTO> eventsComboBox;
    @FXML private Label eventMethodLabel;
    @FXML private Label eventBalanceLabel;
    @FXML private Label eventCommissionLabel;
    @FXML private Label eventStatusOrWinningOption;
    @FXML private Label eventDescriptionLabel;
    @FXML private ComboBox<String> tradeOptionComboBox;
    @FXML private Label optionPriceLabel;
    @FXML private Spinner<Integer> sharesCountSpinner;
    @FXML private VBox buySharesSection;
    @FXML private Label totalSharesPrice;
    @FXML private Button activateEventButton;
    @FXML private Button endEventButton;
    @FXML private ComboBox<String> winningOptionComboBox;
    @FXML private Label commissionToPayLabel;
    @FXML private Label payNowOrLaterLabel;

    // =========================================================================
    // FXML UI Controls - Purchase History table
    // =========================================================================

    @FXML private TableView<TradeHistoryDTO> tradeHistoryTableView;
    @FXML private TableColumn<TradeHistoryDTO, String> tradeDateCol;
    @FXML private TableColumn<TradeHistoryDTO, String> tradeUserCol;
    @FXML private TableColumn<TradeHistoryDTO, String> tradeOptionCol;
    @FXML private TableColumn<TradeHistoryDTO, Integer> tradeSharesCol;
    @FXML private TableColumn<TradeHistoryDTO, String> tradePriceCol;

    // =========================================================================
    // Controller State & Observable Collections
    // =========================================================================
    private MarketEngine marketEngine;

    // Observable collections backing the tables and combo boxes
    private final ObservableList<UserDTO> usersList = FXCollections.observableArrayList();
    private final ObservableList<EventDTO> eventsList = FXCollections.observableArrayList();
    private final ObservableList<EventDTO> participatingEventsList = FXCollections.observableArrayList();
    private final ObservableList<String> availableOptionsList = FXCollections.observableArrayList();
    private final ObservableList<TradeHistoryDTO> tradeHistoryList = FXCollections.observableArrayList();

    // Observable properties
    private final DoubleProperty userBalance = new SimpleDoubleProperty(0.0);
    private final ObjectProperty<EventDetailsDTO> selectedEventDetails = new SimpleObjectProperty<>();
    private final IntegerProperty selectedSharesProperty = new SimpleIntegerProperty(1);
    private final DoubleProperty totalPriceToPay = new SimpleDoubleProperty(0.0);

    // =========================================================================
    // Lifecycle & Initialization
    // =========================================================================
    @FXML
    private void initialize() {
        bindCollectionsToControls();
        setupUsersTableColumns();
        setupUserEventsTableColumns();
        setupComboBoxConverters();
        setupBindingsAndListeners();
        setupSpinner();
        setupPurchaseHistoryTableView();
    }

    /**
     * Attaches observable lists to their respective UI components.
     */
    private void bindCollectionsToControls() {
        usersTableView.setItems(usersList);
        eventsComboBox.setItems(eventsList);
        userEventsTableView.setItems(participatingEventsList);
        tradeOptionComboBox.setItems(availableOptionsList);
        winningOptionComboBox.setItems(availableOptionsList);
    }

    /**
     * Configures display converters for UI controls (renders Event names in ComboBox).
     */
    private void setupComboBoxConverters() {
        eventsComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(EventDTO event) {
                return event != null ? event.name() : "";
            }

            @Override
            public EventDTO fromString(String string) {
                return null; // Not needed for non-editable combo box
            }
        });
    }

    /**
     * Configures cell value factories for the main Users table.
     */
    private void setupUsersTableColumns() {
        userNameCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().name()));

        userBalanceCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("$%.2f", cellData.getValue().balance())));
    }

    /**
     * Configures cell value factories for the Participating Events sub-table.
     */
    private void setupUserEventsTableColumns() {
        userEventNameCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().name()));

        userEventRoleCol.setCellValueFactory(cellData -> {
            EventDTO event = cellData.getValue();
            UserDTO selectedUser = usersTableView.getSelectionModel().getSelectedItem();

            if (selectedUser != null && selectedUser.marketMakerEventIds() != null) {
                boolean isMarketMaker = selectedUser.marketMakerEventIds().contains(event.id());
                return new SimpleStringProperty(isMarketMaker ? "Market Maker" : "Participant");
            }
            return new SimpleStringProperty("Participant");
        });

        userEventSharesCol.setCellValueFactory(cellData -> {
            UserDTO selectedUser = usersTableView.getSelectionModel().getSelectedItem();
            int shares = selectedUser == null ? 0 : totalSharesHeld(selectedUser, cellData.getValue().id());
            return new SimpleObjectProperty<>(shares);
        });
    }

    /**
     * Sets up UI property bindings and selection listeners.
     */
    private void setupBindingsAndListeners() {
        // Bind the formatted account balance to the label
        accountBalanceLabel.textProperty().bind(userBalance.asString("$%,.2f"));

        // Update details and sub-table when user selection changes
        usersTableView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> handleRowSelected(newSelection)
        );

        // Fetch detailed DTO when an active event is selected in the ComboBox
        eventsComboBox.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldEvent, newEvent) -> handleActiveEventSelected(newEvent)
        );

        // Bind event method label with selected event
        eventMethodLabel.textProperty().bind(
                selectedEventDetails
                        .map(EventDetailsDTO::eventInfo)
                        .map(e -> e.tradingMethod().name())
                        .orElse("-")
        );

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

        eventStatusOrWinningOption.textProperty().bind(
                selectedEventDetails
                        .map(EventDetailsDTO::eventInfo)
                        .map(e -> e.status().name())
        );

        eventDescriptionLabel.textProperty().bind(
                selectedEventDetails
                        .map(EventDetailsDTO::eventInfo)
                        .map(EventDTO::description)
        );

        selectedEventDetails.addListener((obs, oldDetails, newDetails) -> {
            tradeOptionComboBox.getSelectionModel().clearSelection();

            if (newDetails != null && newDetails.eventInfo() != null && newDetails.eventInfo().options() != null) {
                // Populate with options from the event (e.g., ["Yes", "No"])
                availableOptionsList.setAll(newDetails.eventInfo().options());

                // Auto-select the first option by default
                if (!availableOptionsList.isEmpty()) {
                    tradeOptionComboBox.getSelectionModel().selectFirst();
                }
            } else {
                availableOptionsList.clear();
            }
        });

        // The trade panel is enabled only for a non-market-maker on an open event.
        buySharesSection.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> !TradeRules.canTrade(
                                usersTableView.getSelectionModel().getSelectedItem(),
                                eventsComboBox.getValue()),
                        usersTableView.getSelectionModel().selectedItemProperty(),
                        eventsComboBox.valueProperty())
        );

        // The activate button is enabled only for the market maker of a not-yet-open event.
        activateEventButton.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> !TradeRules.canActivate(
                                usersTableView.getSelectionModel().getSelectedItem(),
                                eventsComboBox.getValue()),
                        usersTableView.getSelectionModel().selectedItemProperty(),
                        eventsComboBox.valueProperty())
        );

        // The end even button is enabled only for the market maker of an open event.
        endEventButton.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> !TradeRules.canEnd(
                                usersTableView.getSelectionModel().getSelectedItem(),
                                eventsComboBox.getValue()),
                        usersTableView.getSelectionModel().selectedItemProperty(),
                        eventsComboBox.valueProperty())
        );

        // The winning option combobox is enabled only for the market maker of an open event.
        winningOptionComboBox.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> !TradeRules.canEnd(
                                usersTableView.getSelectionModel().getSelectedItem(),
                                eventsComboBox.getValue()),
                        usersTableView.getSelectionModel().selectedItemProperty(),
                        eventsComboBox.valueProperty())
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
                }, selectedEventDetails, eventsComboBox.valueProperty(),
                   tradeOptionComboBox.valueProperty(), selectedSharesProperty)
        );

        commissionToPayLabel.textProperty().bind(
                Bindings.createStringBinding(() -> {
                    TradeQuoteDTO q = currentQuote();
                    return q == null ? "$0.00" : String.format("$%.2f", q.commission());
                }, selectedEventDetails, eventsComboBox.valueProperty(),
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

        selectedEventDetails.addListener((obs, oldDetails, newDetails) -> {
            if (newDetails != null && newDetails.tradeHistory() != null) {
                tradeHistoryList.setAll(newDetails.tradeHistory());
            } else {
                tradeHistoryList.clear();
            }
        });
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
     * Refreshes the local ObservableLists while preserving active user and event selections.
     */
    @Override
    public void onMarketDataChanged() {
        if (marketEngine == null) return;

        // Ensure UI updates always run safely on the JavaFX Application Thread
        Platform.runLater(() -> {
            UserDTO currentSelectedUser = usersTableView.getSelectionModel().getSelectedItem();
            EventDTO currentSelectedEvent = eventsComboBox.getValue();

            // Refresh master lists from engine
            usersList.setAll(marketEngine.getAllUsers().values());
            eventsList.setAll(marketEngine.getAllEvents());

            // Restore user selection if still present in the updated list
            if (currentSelectedUser != null) {
                usersList.stream()
                        .filter(u -> u.name().equals(currentSelectedUser.name()))
                        .findFirst()
                        .ifPresentOrElse(
                                u -> {
                                    usersTableView.getSelectionModel().select(u);
                                    handleRowSelected(u);
                                },
                                () -> handleRowSelected(null)
                        );
            } else {
                handleRowSelected(null);
            }

            // Restore the event ComboBox selection from the refreshed list
            if (currentSelectedEvent != null) {
                eventsList.stream()
                        .filter(e -> e.id() == currentSelectedEvent.id())
                        .findFirst()
                        .ifPresentOrElse(
                                eventsComboBox::setValue,
                                () -> eventsComboBox.setValue(null)
                        );
            }
        });
    }

    // =========================================================================
    // Selection Handlers
    // =========================================================================
    /**
     * Updates the balance display and participating events table based on the selected user.
     */
    private void handleRowSelected(UserDTO selectedUser) {
        if (selectedUser == null) {
            userBalance.set(0.0);
            participatingEventsList.clear();
            return;
        }
        userBalance.set(selectedUser.balance());

        java.util.Set<Integer> ids = selectedUser.holdings().keySet();
        if (ids.isEmpty() || marketEngine == null) {
            participatingEventsList.clear();
        } else {
            participatingEventsList.setAll(marketEngine.getAllEvents().stream()
                    .filter(e -> ids.contains(e.id()))
                    .toList());
        }
    }

    /** Total shares the given user holds in the given event, across all options. */
    private static int totalSharesHeld(UserDTO user, int eventId) {
        Map<String, Integer> byOption = user.holdings().get(eventId);
        if (byOption == null) {
            return 0;
        }
        return byOption.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Updates the selected event details when an active event is chosen.
     */
    private void handleActiveEventSelected(EventDTO newEvent) {
        if (newEvent != null && marketEngine != null) {
            EventDetailsDTO details = marketEngine.getEventDetails(newEvent.id());
            selectedEventDetails.set(details);

            // Populate trade history directly
            if (details != null && details.tradeHistory() != null) {
                tradeHistoryList.setAll(details.tradeHistory());
            } else {
                tradeHistoryList.clear();
            }
        } else {
            selectedEventDetails.set(null);
        }
    }

    /**
     * Exposes the selected event details property for UI bindings.
     */
    public ObjectProperty<EventDetailsDTO> selectedEventDetailsProperty() {
        return selectedEventDetails;
    }

    /**
     * Set up the shares Spinner
     */
    private void setupSpinner() {
        // 1. Configure the value factory: (min: 1, max: 1,000,000, initial: 1, step: 1)
        SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1_000_000, 1, 1);
        sharesCountSpinner.setValueFactory(valueFactory);
        sharesCountSpinner.setEditable(true);

        // 2. Commit typed text immediately when the user presses Enter or clicks away (loses focus)
        sharesCountSpinner.getEditor().setOnAction(event -> commitEditorText());
        sharesCountSpinner.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                commitEditorText();
            }
        });

        // 3. Keep selectedSharesProperty in sync with the spinner's value
        sharesCountSpinner.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                selectedSharesProperty.set(newValue);
            }
        });
    }

    /**
     * The engine's quote for the trade the panel currently describes, or
     * {@code null} if the selection is incomplete or the event cannot be priced.
     */
    private TradeQuoteDTO currentQuote() {
        EventDTO event = eventsComboBox.getValue();
        String option = tradeOptionComboBox.getValue();
        int shares = selectedSharesProperty.get();
        if (marketEngine == null || event == null || option == null || shares <= 0) {
            return null;
        }
        int optionIndex1Based = tradeOptionComboBox.getItems().indexOf(option) + 1;
        if (optionIndex1Based < 1) {
            return null;
        }
        try {
            return marketEngine.quoteTrade(
                    usersTableView.getSelectionModel().getSelectedItem(), event, optionIndex1Based, shares);
        } catch (RuntimeException notQuotable) {
            return null;
        }
    }

    /**
     * Safely parses and commits typed text into the SpinnerValueFactory.
     */
    private void commitEditorText() {
        String text = sharesCountSpinner.getEditor().getText();
        SpinnerValueFactory<Integer> factory = sharesCountSpinner.getValueFactory();
        if (factory != null && factory.getConverter() != null) {
            try {
                Integer value = factory.getConverter().fromString(text);
                factory.setValue(value);
            } catch (Exception ex) {
                // Revert invalid text (e.g., non-numeric) back to current factory value
                sharesCountSpinner.getEditor().setText(factory.getConverter().toString(factory.getValue()));
            }
        }
    }

    /**
     * Handle execute trade button
     */
    @FXML
    private void handleExecuteTrade() {
        // 1. Ensure any typed text in the spinner is committed
        commitEditorText();

        UserDTO selectedUser = usersTableView.getSelectionModel().getSelectedItem();
        EventDTO selectedEvent = eventsComboBox.getValue();
        int selectedOption = tradeOptionComboBox.getSelectionModel().getSelectedIndex() + 1;
        int shares = selectedSharesProperty.get();

        // 2. Defensive checks
        if (selectedUser == null || selectedEvent == null || selectedOption == 0 || shares <= 0) {
            Dialogs.error("Invalid Selection", "Please select a user, an active event, an option, and a valid quantity of shares.");
            return;
        }

        if (marketEngine == null) {
            Dialogs.error("Engine Error", "Market Engine is not initialized.");
            return;
        }

        // 3. Execute trade
        LOG.info("Trade requested: user='{}', event={}, option#={}, shares={}",
                selectedUser.name(), selectedEvent.id(), selectedOption, shares);
        try {
            marketEngine.buyShares(
                    selectedUser,
                    selectedEvent,
                    selectedOption,
                    shares
            );

            // Reset shares count back to 1 on success
            sharesCountSpinner.getValueFactory().setValue(1);

        } catch (Exception ex) {
            LOG.warn("Trade failed for user '{}' on event {}: {}",
                    selectedUser.name(), selectedEvent.id(), ex.getMessage());
            Dialogs.error("Trade Execution Failed", ex);
        }
    }

    private void setupPurchaseHistoryTableView() {
        // 1. Bind items to the observable list
        tradeHistoryTableView.setItems(tradeHistoryList);

        // 2. Custom ListCell rendering
        tradeDateCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().timestamp()));

        // Bind buyer name to User column
        tradeUserCol.setCellValueFactory(cellData->
                new SimpleStringProperty(cellData.getValue().buyerName()));

        // Bind option name to Option column
        tradeOptionCol.setCellValueFactory(cellData->
                new SimpleStringProperty(cellData.getValue().optionName()));

        // Bind quantity to Shares column
        tradeSharesCol.setCellValueFactory(cellData->
                new SimpleObjectProperty<>(cellData.getValue().quantity()));

        // Bind pricePaid to Price Paid column
        tradePriceCol.setCellValueFactory(cellData-> {
            Double price = cellData.getValue().pricePaid();
            return new SimpleStringProperty(String.format("$%.2f", price));
        });
    }

    @FXML
    private void handleActivateEvent() {
        UserDTO selectedUser = usersTableView.getSelectionModel().getSelectedItem();
        EventDTO selectedEvent = eventsComboBox.getValue();
        if (selectedUser == null || selectedEvent == null || marketEngine == null) {
            Dialogs.error("Invalid Selection", "Select a user and an event first.");
            return;
        }
        // The engine enforces that the user is the event's market maker and can
        // fund the subsidy; surface whatever it rejects.
        LOG.info("Activate event {} requested by '{}'", selectedEvent.id(), selectedUser.name());
        try {
            marketEngine.activateEvent(selectedEvent, selectedUser);
        } catch (RuntimeException ex) {
            LOG.warn("Activate event {} failed: {}", selectedEvent.id(), ex.getMessage());
            Dialogs.error("Could not activate event", ex);
        }
    }

    @FXML
    private void handleEndEvent() {
        UserDTO selectedUser = usersTableView.getSelectionModel().getSelectedItem();
        EventDTO selectedEvent = eventsComboBox.getValue();
        int selectedOption = winningOptionComboBox.getSelectionModel().getSelectedIndex() + 1;
        if (selectedUser == null || selectedEvent == null || marketEngine == null) {
            Dialogs.error("Invalid Selection", "Select a user and an event first.");
            return;
        }
        LOG.info("End event {} requested by '{}' with winning option #{}",
                selectedEvent.id(), selectedUser.name(), selectedOption);
        try {
            marketEngine.closeEvent(selectedEvent.id(), selectedOption);
        } catch (RuntimeException ex) {
            LOG.warn("End event {} failed: {}", selectedEvent.id(), ex.getMessage());
            Dialogs.error("Could not close event", ex);
        }
    }
}