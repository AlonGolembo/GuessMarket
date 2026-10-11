package com.guessmarket.client.common;

import com.guessmarket.client.controllers.ChatController;
import com.guessmarket.client.http.ChatClient;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;

/**
 * Opens the chat window - a non-modal {@link Stage} (unlike {@link NewEventDialog}'s
 * modal dialogs) so it can stay open alongside the main window. Keeps the one
 * instance around so a second "Open Chat" click brings it to front instead of
 * spawning a duplicate window and poller.
 */
public final class ChatWindow {

    private ChatWindow() {}

    private static Stage openStage;
    private static ChatController openController;

    public static void open(Window owner, ChatClient chatClient, String currentUserName) {
        if (openStage != null) {
            openController.setContext(chatClient, currentUserName);
            openStage.toFront();
            openStage.requestFocus();
            return;
        }

        FXMLLoader loader = new FXMLLoader(
                ChatWindow.class.getResource("/com/guessmarket/client/views/chat-view.fxml"));
        Parent root;
        try {
            root = loader.load();
        } catch (IOException e) {
            throw new IllegalStateException("Could not load chat-view.fxml", e);
        }

        ChatController controller = loader.getController();
        controller.setContext(chatClient, currentUserName);

        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.setTitle("Guess Market Chat");
        stage.setScene(new Scene(root));

        openStage = stage;
        openController = controller;
        controller.startPolling();

        stage.setOnHidden(e -> {
            controller.stopPolling();
            openStage = null;
            openController = null;
        });

        stage.show();
    }
}
