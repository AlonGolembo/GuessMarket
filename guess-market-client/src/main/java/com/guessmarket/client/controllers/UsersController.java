package com.guessmarket.client.controllers;

import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.EventDetailsDTO;
import com.guessmarket.dto.UserDTO;
import com.guessmarket.dto.UserDetailsDTO;
import com.guessmarket.engine.api.MarketDataChangeListener;
import com.guessmarket.engine.api.MarketEngine;
import com.guessmarket.client.common.Charts;
import com.guessmarket.client.common.Dialogs;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Controller for the Users tab. This client represents exactly one logged-in
 * identity (see {@link #setContext}), so this tab is a pure viewer: the table
 * just lists every user in the system by name; selecting any row expands into
 * that user's details (name/balance/market-maker flag - per the spec, that is
 * everything a user may see about anyone else). Selecting myself additionally
 * reveals my full account details (balance graph, participating events,
 * blocked warning) and lets me deposit funds. All event activation/trading/
 * closing now lives in the Events tab.
 */
public class UsersController implements MarketDataChangeListener {

    private static final Logger LOG = LogManager.getLogger(UsersController.class);

    // =========================================================================
    // FXML UI Controls - All Users Table
    // =========================================================================
    @FXML private TableView<UserDTO> usersTableView;
    @FXML private TableColumn<UserDTO, String> userNameCol;

    // =========================================================================
    // FXML UI Controls - Selected User Details (any user)
    // =========================================================================
    @FXML private Label userDetailsHeaderLabel;
    @FXML private GridPane userInfoGrid;
    @FXML private Label selectedUserNameLabel;
    @FXML private Label selectedUserBalanceLabel;
    @FXML private Label selectedUserIsMmLabel;
    @FXML private Label noSelectionLabel;

    // =========================================================================
    // FXML UI Controls - My Account (self only)
    // =========================================================================
    @FXML private VBox selfOnlyPanel;
    @FXML private Button depositButton;
    @FXML private Label userBlockedLabel;
    @FXML private TableView<EventDTO> userEventsTableView;
    @FXML private TableColumn<EventDTO, String> userEventNameCol;
    @FXML private TableColumn<EventDTO, String> userEventRoleCol;
    @FXML private TableColumn<EventDTO, Integer> userEventSharesCol;
    @FXML private Label userEventInvolvementLabel;

    // =========================================================================
    // FXML UI Controls - Graphs tab (self only)
    // =========================================================================
    @FXML private AnchorPane graph;
    @FXML private Label graphPlaceholderLabel;

    // =========================================================================
    // Controller State & Observable Collections
    // =========================================================================
    private MarketEngine marketEngine;
    /** The one identity this client acts as for its whole run - set once, at login. */
    private String currentUserName;
    /** Refreshed on every poll tick from the engine; {@code null} until the first tick lands. */
    private final ObjectProperty<UserDTO> myUser = new SimpleObjectProperty<>();

    /** Whether I was already blocked at the last refresh - to notify only when it newly happens. */
    private boolean wasBlocked;

    private final ObservableList<UserDTO> usersList = FXCollections.observableArrayList();
    private final ObservableList<EventDTO> participatingEventsList = FXCollections.observableArrayList();

    // =========================================================================
    // Lifecycle & Initialization
    // =========================================================================
    @FXML
    private void initialize() {
        usersTableView.setItems(usersList);
        userEventsTableView.setItems(participatingEventsList);

        setupUsersTableColumns();
        setupUserEventsTableColumns();

        usersTableView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldUser, newUser) -> showSelectedUser(newUser));

        showSelectedUser(null);
    }

    /** Configures cell value factories for the all-users table. */
    private void setupUsersTableColumns() {
        userNameCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().name()));
    }

    private static boolean isMarketMaker(UserDTO user) {
        Set<String> mmEvents = user.marketMakerEventNames();
        return mmEvents != null && !mmEvents.isEmpty();
    }

    /** Configures cell value factories for my Participating Events sub-table. */
    private void setupUserEventsTableColumns() {
        userEventNameCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().name()));

        userEventRoleCol.setCellValueFactory(cellData -> {
            EventDTO event = cellData.getValue();
            UserDTO me = myUser.get();

            if (me != null && me.marketMakerEventNames() != null) {
                boolean isMarketMaker = me.marketMakerEventNames().contains(event.name());
                return new SimpleStringProperty(isMarketMaker ? "Market Maker" : "Participant");
            }
            return new SimpleStringProperty("Participant");
        });

        userEventSharesCol.setCellValueFactory(cellData -> {
            UserDTO me = myUser.get();
            int shares = me == null ? 0 : totalSharesHeld(me, cellData.getValue().name());
            return new SimpleObjectProperty<>(shares);
        });

        userEventsTableView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldEvent, newEvent) -> showUserEventInvolvement(newEvent));
    }

    /**
     * Details of my involvement in one of my events: shares held per option,
     * and for a closed event the winner and my net cash result (the sum of
     * every ledger entry tagged with this event).
     */
    private void showUserEventInvolvement(EventDTO event) {
        UserDTO me = myUser.get();
        if (event == null || me == null || marketEngine == null) {
            userEventInvolvementLabel.setText("Select one of your events above to see your involvement in it.");
            return;
        }

        Map<String, Integer> held = me.holdings().get(event.name());
        StringBuilder sb = new StringBuilder(event.name()).append("  •  ").append(event.status().name());
        if (held != null && !held.isEmpty()) {
            sb.append("  •  Your shares:");
            held.forEach((option, qty) -> sb.append("  ").append(option).append(" = ").append(qty));
        }

        if (event.status() == com.guessmarket.dto.EventStatus.CLOSED) {
            EventDetailsDTO details = marketEngine.getEventDetails(event.name());
            sb.append("\nWinner: ").append(details.winningOption() == null ? "-" : details.winningOption());
            double net = marketEngine.getUserDetails(currentUserName).balanceHistory().stream()
                    .filter(entry -> event.name().equals(entry.eventName()))
                    .mapToDouble(com.guessmarket.dto.LedgerEntryDTO::delta)
                    .sum();
            sb.append("  •  Your net result on this event: ")
                    .append(String.format("%s$%,.2f", net < 0 ? "-" : "+", Math.abs(net)));
        }
        userEventInvolvementLabel.setText(sb.toString());
    }

    // =========================================================================
    // Selection Handling - shows minimal info for anyone, full account for self
    // =========================================================================
    /**
     * Renders the details panel for whichever user row is selected: basic
     * name/balance/market-maker info for anyone, plus the self-only account
     * panel (balance graph, participating events, deposit) when I select myself.
     */
    private void showSelectedUser(UserDTO selected) {
        boolean hasSelection = selected != null;
        boolean isSelf = hasSelection && currentUserName != null && currentUserName.equals(selected.name());

        noSelectionLabel.setVisible(!hasSelection);
        noSelectionLabel.setManaged(!hasSelection);
        userDetailsHeaderLabel.setVisible(hasSelection);
        userDetailsHeaderLabel.setManaged(hasSelection);
        userInfoGrid.setVisible(hasSelection);
        userInfoGrid.setManaged(hasSelection);

        if (hasSelection) {
            userDetailsHeaderLabel.setText("User Details: " + selected.name());
            selectedUserNameLabel.setText(selected.name());
            selectedUserBalanceLabel.setText(String.format("$%.2f", selected.balance()));
            selectedUserIsMmLabel.setText(isMarketMaker(selected) ? "Yes" : "No");
        }

        selfOnlyPanel.setVisible(isSelf);
        selfOnlyPanel.setManaged(isSelf);
        if (isSelf) {
            refreshMyAccountPanel(selected);
        }

        boolean showGraph = isSelf;
        graph.setVisible(showGraph);
        graph.setManaged(showGraph);
        graphPlaceholderLabel.setVisible(!showGraph);
        graphPlaceholderLabel.setManaged(!showGraph);
        if (showGraph) {
            renderBalanceGraph();
        } else {
            graph.getChildren().clear();
        }
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

    /**
     * Callback triggered whenever the MarketEngine's data changes (a poll tick
     * that found something new, in the client-server build). Refreshes the
     * all-users table and, if a selection exists, the details panel below it.
     */
    @Override
    public void onMarketDataChanged() {
        if (marketEngine == null) return;

        Platform.runLater(() -> {
            UserDTO currentSelection = usersTableView.getSelectionModel().getSelectedItem();

            Map<String, UserDTO> allUsers = marketEngine.getAllUsers();
            UserDTO me = allUsers.get(currentUserName);
            myUser.set(me);

            usersList.setAll(allUsers.values());

            notifyIfNewlyBlocked(me);

            if (currentSelection != null) {
                UserDTO refreshed = allUsers.get(currentSelection.name());
                if (refreshed != null) {
                    usersTableView.getSelectionModel().select(refreshed);
                } else {
                    usersTableView.getSelectionModel().clearSelection();
                }
                showSelectedUser(refreshed);
            }
        });
    }

    /**
     * Pops a warning the first time I become blocked (my balance was forced
     * negative to honour a resting order) - satisfies the "notify the user"
     * part of the negative-balance rule.
     */
    private void notifyIfNewlyBlocked(UserDTO me) {
        boolean blockedNow = me != null && me.blocked();
        if (blockedNow && !wasBlocked) {
            Dialogs.error("You are blocked",
                    "Your balance was forced negative while a resting order was filled, "
                            + "and you can no longer perform any action.");
        }
        wasBlocked = blockedNow;
    }

    // =========================================================================
    // My Account (self only)
    // =========================================================================
    /**
     * Refreshes the blocked flag and participating-events table from my
     * current snapshot. The balance graph is refreshed separately in
     * {@link #showSelectedUser}, since it only needs to redraw when the
     * selection changes, not on every poll tick.
     */
    private void refreshMyAccountPanel(UserDTO me) {
        if (me == null) {
            userBlockedLabel.setVisible(false);
            userBlockedLabel.setManaged(false);
            participatingEventsList.clear();
            return;
        }

        userBlockedLabel.setVisible(me.blocked());
        userBlockedLabel.setManaged(me.blocked());

        Set<String> names = me.holdings().keySet();
        participatingEventsList.setAll(names.isEmpty() ? List.of() : marketEngine.getAllEvents().stream()
                .filter(e -> names.contains(e.name()))
                .toList());
    }

    /** Draws my balance-over-time chart into the Graphs sub-tab. */
    private void renderBalanceGraph() {
        if (marketEngine == null || currentUserName == null) {
            graph.getChildren().clear();
            return;
        }
        UserDetailsDTO details = marketEngine.getUserDetails(currentUserName);
        LineChart<String, Number> chart = Charts.createBalanceOverTimeChart(details);
        AnchorPane.setTopAnchor(chart, 0.0);
        AnchorPane.setRightAnchor(chart, 0.0);
        AnchorPane.setBottomAnchor(chart, 0.0);
        AnchorPane.setLeftAnchor(chart, 0.0);
        graph.getChildren().setAll(chart);
    }

    /** Total shares the given user holds in the given event, across all options. */
    private static int totalSharesHeld(UserDTO user, String eventName) {
        Map<String, Integer> byOption = user.holdings().get(eventName);
        if (byOption == null) {
            return 0;
        }
        return byOption.values().stream().mapToInt(Integer::intValue).sum();
    }

    @FXML
    private void handleDeposit() {
        if (marketEngine == null || currentUserName == null) {
            return;
        }
        Optional<String> amountText = Dialogs.prompt("Deposit Funds", "Amount to add to your account", "");
        amountText.ifPresent(text -> {
            double amount;
            try {
                amount = Double.parseDouble(text.trim());
            } catch (NumberFormatException ex) {
                Dialogs.error("Invalid amount", "Enter a positive number.");
                return;
            }
            if (amount <= 0) {
                Dialogs.error("Invalid amount", "Amount must be positive.");
                return;
            }
            try {
                marketEngine.deposit(currentUserName, amount);
            } catch (RuntimeException ex) {
                LOG.warn("Deposit failed for '{}': {}", currentUserName, ex.getMessage());
                Dialogs.error("Deposit Failed", ex);
            }
        });
    }
}
