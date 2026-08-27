package com.guessmarket.ui.controllers;

import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.UserDTO;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

public class UsersController {

    // Main Users Table
    @FXML private TableView<UserDTO> usersTableView;
    @FXML private TableColumn<UserDTO, String> userNameCol;
    @FXML private TableColumn<UserDTO, Double> userBalanceCol;

    @FXML private Label accountBalanceLabel;

    // Participating Events Table (Holds EventDTOs, not UserDTOs)
    @FXML private TableView<EventDTO> userEventsTableView;
    @FXML private TableColumn<EventDTO, String> userEventNameCol;
    @FXML private TableColumn<EventDTO, String> userEventRoleCol;
    @FXML private TableColumn<EventDTO, Integer> userEventSharesCol;

    @FXML private VBox singleEventTradeDetailsContainer;

    // Observable list backing the user's events table
    private final ObservableList<EventDTO> participatingEventsList = FXCollections.observableArrayList();
    private final DoubleProperty userBalance = new SimpleDoubleProperty();

    @FXML
    private void initialize() {
        // Bind list to the sub-table
        userEventsTableView.setItems(participatingEventsList);

        // 1. Setup Users Table Column Factories
        userNameCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().name()));
        userBalanceCol.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().initialCash()));

        // 2. Setup Participating Events Table Column Factories
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

        // Bind user balance display
        accountBalanceLabel.textProperty().bind(userBalance.asString());

        // Single Selection Listener
        usersTableView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> handleRowSelected(newSelection)
        );
    }

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

    public void bindUsersList(ObservableList<UserDTO> sharedUsersList) {
        usersTableView.setItems(sharedUsersList);
    }
}