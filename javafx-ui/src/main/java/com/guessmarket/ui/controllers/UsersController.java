package com.guessmarket.ui.controllers;

import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.EventDetailsDTO;
import com.guessmarket.dto.TradeHistoryDTO;
import com.guessmarket.dto.UserDTO;
import com.guessmarket.engine.api.MarketDataChangeListener;
import com.guessmarket.engine.api.MarketEngine;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.util.List;

/**
 * Controller for the Users Tab, managing the list of all users,
 * displaying individual user details/balances, showing participating events,
 * and maintaining active event options for trading.
 */
public class UsersController implements MarketDataChangeListener {

    // =========================================================================
    // FXML UI Controls - Main Users Table
    // =========================================================================
    @FXML private TableView<UserDTO> usersTableView;
    @FXML private TableColumn<UserDTO, String> userNameCol;
    @FXML private TableColumn<UserDTO, Double> userBalanceCol;

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
    @FXML private ComboBox<EventDTO> activeEventsComboBox;
    @FXML private Label eventMethodLabel;
    @FXML private Label eventBalanceLabel;
    @FXML private Label eventCommissionLabel;
    @FXML private Label eventWinningOptionLabel;
    @FXML private Label eventDescriptionLabel;
    @FXML private ComboBox<String> tradeOptionComboBox;
    @FXML private Label optionPriceLabel;
    @FXML private Spinner<Integer> sharesCountSpinner;
    @FXML private Button executeTradeButton;
    @FXML private ListView<TradeHistoryDTO> eventPurchaseHistory;

    // =========================================================================
    // Controller State & Observable Collections
    // =========================================================================
    private MarketEngine marketEngine;

    // Observable collections backing the tables and combo boxes
    private final ObservableList<UserDTO> usersList = FXCollections.observableArrayList();
    private final ObservableList<EventDTO> eventsList = FXCollections.observableArrayList();
    private final ObservableList<EventDTO> activeEventsList = FXCollections.observableArrayList();
    private final ObservableList<EventDTO> participatingEventsList = FXCollections.observableArrayList();
    private final ObservableList<String> availableOptionsList = FXCollections.observableArrayList();
    private final ObservableList<TradeHistoryDTO> tradeHistoryList = FXCollections.observableArrayList();

    // Observable properties
    private final DoubleProperty userBalance = new SimpleDoubleProperty(0.0);
    private final ObjectProperty<EventDetailsDTO> selectedEventDetails = new SimpleObjectProperty<>();
    private final IntegerProperty selectedSharesProperty = new SimpleIntegerProperty(1);

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
        setupPurchaseHistoryListView();
    }

    /**
     * Attaches observable lists to their respective UI components.
     */
    private void bindCollectionsToControls() {
        usersTableView.setItems(usersList);
        activeEventsComboBox.setItems(activeEventsList);
        userEventsTableView.setItems(participatingEventsList);
        tradeOptionComboBox.setItems(availableOptionsList);
    }

    /**
     * Configures display converters for UI controls (renders Event names in ComboBox).
     */
    private void setupComboBoxConverters() {
        activeEventsComboBox.setConverter(new StringConverter<>() {
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
                new SimpleObjectProperty<>(cellData.getValue().initialCash()));
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

            if (selectedUser != null && selectedUser.eventsIdUserIsMM() != null) {
                boolean isMarketMaker = selectedUser.eventsIdUserIsMM().contains(event.id());
                return new SimpleStringProperty(isMarketMaker ? "Market Maker" : "Participant");
            }
            return new SimpleStringProperty("Participant");
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
        activeEventsComboBox.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldEvent, newEvent) -> handleActiveEventSelected(newEvent)
        );

        // Bind event method label with selected event
        eventMethodLabel.textProperty().bind(
                selectedEventDetails
                        .map(EventDetailsDTO::eventInfo)
                        .map(EventDTO::tradingMethod)
                        .orElse("-")
        );

        eventBalanceLabel.textProperty().bind(
                selectedEventDetails
                        .map(EventDetailsDTO::eventAccountBalance)
                        .map(balance -> String.format("$.2f", balance))
                        .orElse("$0.00")
        );

        eventCommissionLabel.textProperty().bind(
                selectedEventDetails
                        .map(EventDetailsDTO::totalCommissionCollected)
                        .map(balance -> String.format("$%,.2f", balance))
                        .orElse("$0.00")
        );

        eventWinningOptionLabel.textProperty().bind(
                selectedEventDetails
                        .map(EventDetailsDTO::winningOption)
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

        executeTradeButton.disableProperty().bind(
                usersTableView.getSelectionModel().selectedItemProperty().isNull()
                        .or(activeEventsComboBox.getSelectionModel().selectedItemProperty().isNull())
                        .or(tradeOptionComboBox.getSelectionModel().selectedItemProperty().isNull())
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
            EventDTO currentSelectedEvent = activeEventsComboBox.getValue();

            // Refresh master lists from engine
            usersList.setAll(marketEngine.getAllUsers().values());
            eventsList.setAll(marketEngine.getAllEvents());

            // Filter and populate only active events for trading
            List<EventDTO> activeList = eventsList.stream()
                    .filter(EventDTO::isActive)
                    .toList();
            activeEventsList.setAll(activeList);

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

            // Restore active event ComboBox selection
            if (currentSelectedEvent != null) {
                activeEventsList.stream()
                        .filter(e -> e.id() == currentSelectedEvent.id())
                        .findFirst()
                        .ifPresentOrElse(
                                e -> activeEventsComboBox.setValue(e),
                                () -> activeEventsComboBox.setValue(null)
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
        if (selectedUser != null) {
            userBalance.set(selectedUser.initialCash() != null ? selectedUser.initialCash() : 0.0);

            if (selectedUser.participatingEvents() != null) {
                participatingEventsList.setAll(selectedUser.participatingEvents().values());
            } else {
                participatingEventsList.clear();
            }
        } else {
            userBalance.set(0.0);
            participatingEventsList.clear();
        }
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
                // Print / display value when changed via arrows or typing
                System.out.println("Current shares: " + newValue);
            }
        });
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
        EventDTO selectedEvent = activeEventsComboBox.getValue();
        int selectedOption = tradeOptionComboBox.getSelectionModel().getSelectedIndex() + 1;
        int shares = selectedSharesProperty.get();

        // 2. Defensive checks
        if (selectedUser == null || selectedEvent == null || selectedOption == 0 || shares <= 0) {
            showErrorAlert("Invalid Selection", "Please select a user, an active event, an option, and a valid quantity of shares.");
            return;
        }

        if (marketEngine == null) {
            showErrorAlert("Engine Error", "Market Engine is not initialized.");
            return;
        }

        // 3. Execute trade
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
            showErrorAlert("Trade Execution Failed", ex.getMessage());
        }
    }

    /**
     * Utility dialog to show error messages to the user.
     */
    private void showErrorAlert(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR
        );
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void setupPurchaseHistoryListView() {
        // 1. Bind items to the observable list
        eventPurchaseHistory.setItems(tradeHistoryList);

        // 2. Custom ListCell rendering
        eventPurchaseHistory.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(TradeHistoryDTO trade, boolean empty) {
                super.updateItem(trade, empty);

                if (empty || trade == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // Adjust field names to match your TradeHistoryDTO record
                    // e.g., trade.userName(), trade.optionName(), trade.sharesCount(), trade.pricePerShare()

                    // FIXME: Add a timestamp and user to TradeHistoryDTO and then change the format to: "[%s] User: %s | Option: %s | %d Shares @ $%.2f"
                    setText(String.format("User: %s | Option: %s | %d Shares @ $%.2f",
//                            trade.formattedTimestamp() != null ? trade.formattedTimestamp() : "Trade",
                            trade.buyer().name(),
                            trade.optionName(),
                            trade.quantity(),
                            trade.pricePaid()
                    ));
                }
            }
        });
    }
}