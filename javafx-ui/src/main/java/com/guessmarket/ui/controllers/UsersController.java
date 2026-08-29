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
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.Objects;

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
    @FXML private Button activateEventButton;

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
        eventsComboBox.getSelectionModel().selectedItemProperty().addListener(
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
                        .map(EventDTO::status)
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

        // Disable buySharesContainer if:
        // 1. No user is selected
        // 2. No active event is selected
        // 3. The selected user is the Market Maker (MM) for the selected event
        buySharesSection.disableProperty().bind(
                Bindings.createBooleanBinding(() -> {
                            UserDTO selectedUser = usersTableView.getSelectionModel().getSelectedItem();
                            EventDTO selectedEvent = eventsComboBox.getValue();

                            // Must have both a selected user and a selected event
                            if (selectedUser == null || selectedEvent == null) {
                                return true;
                            }

                            // Check if user is the Market Maker for this event
                            if (selectedUser.eventsIdUserIsMM() != null) {
                                return selectedUser.eventsIdUserIsMM().contains(selectedEvent.id());
                            }

                            return false;
                        },
                        usersTableView.getSelectionModel().selectedItemProperty(),
                        eventsComboBox.valueProperty())
        );

        activateEventButton.disableProperty().bind(
                Bindings.createBooleanBinding(() -> {
                            UserDTO selectedUser = usersTableView.getSelectionModel().getSelectedItem();
                            EventDTO selectedEvent = eventsComboBox.getValue();

                            // 1. Missing selections -> Disabled
                            if (selectedUser == null || selectedEvent == null) {
                                return true;
                            }

                            // 2. User is NOT the Market Maker for this event -> Disabled
                            boolean isMM = selectedUser.eventsIdUserIsMM() != null
                                    && selectedUser.eventsIdUserIsMM().contains(selectedEvent.id());
                            if (!isMM) {
                                return true;
                            }

                            // 3. Event is already "ACTIVE" (or not in a pending state) -> Disabled
                            if ("ACTIVE".equalsIgnoreCase(selectedEvent.status())) {
                                return true;
                            }

                            // Otherwise (User is MM AND event is PENDING) -> Enabled (not disabled)
                            return false;
                        },
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

            // Restore active event ComboBox selection
            if (currentSelectedEvent != null) {
                activeEventsList.stream()
                        .filter(e -> e.id() == currentSelectedEvent.id())
                        .findFirst()
                        .ifPresentOrElse(
                                e -> eventsComboBox.setValue(e),
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
        EventDTO selectedEvent = eventsComboBox.getValue();
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

    private void setupPurchaseHistoryTableView() {
        // 1. Bind items to the observable list
        tradeHistoryTableView.setItems(tradeHistoryList);

        // 2. Custom ListCell rendering
        tradeDateCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().timestamp()));

        // Bind buyer name to User column
        tradeUserCol.setCellValueFactory(cellData->
                new SimpleStringProperty(cellData.getValue().buyer().name()));

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
    private void handleActivateEvent(){
        UserDTO selectedUser = usersTableView.getSelectionModel().getSelectedItem();
        EventDTO selectedEvent = eventsComboBox.getValue();

        // Verify selectedUser is selectedEvent MM
        if(!selectedUser.eventsIdUserIsMM().contains(selectedEvent.id())){
            // FIXME: Handle case that user isn't MM of the event
        }

        this.marketEngine.activateEvent(selectedEvent, selectedUser);
        onMarketDataChanged();
    }
}