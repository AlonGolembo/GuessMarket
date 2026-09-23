package com.guessmarket.client;

import com.guessmarket.client.common.Dialogs;
import com.guessmarket.client.controllers.MainController;
import com.guessmarket.client.http.HttpMarketEngine;
import com.guessmarket.engine.api.MarketEngine;
import com.guessmarket.engine.exception.MarketException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

/**
 * Entry point for the client-server build: unlike {@code javafx-ui}, which
 * embeds {@code MarketEngineImpl} directly, this app talks to a running
 * {@code guess-market-server} over HTTP via {@link HttpMarketEngine} - every
 * controller below it is unaware of the difference, since both are just a
 * {@link MarketEngine}.
 *
 * <p>This client represents exactly one logged-in identity for its whole run,
 * so login happens once here, before the main window is even built, rather
 * than as a repeatable in-app action.
 */
public class GuessMarketApp extends Application {

    private static final Logger LOG = LogManager.getLogger(GuessMarketApp.class);

    public static void main(String[] args) {
        LOG.info("Guess Market client starting");
        launch(args);
        LOG.info("Guess Market client stopped");
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        String baseUrl = System.getProperty("guessmarket.server.url", HttpMarketEngine.DEFAULT_BASE_URL);
        MarketEngine engine = new HttpMarketEngine(baseUrl);

        String currentUserName = promptForLogin(engine);
        if (currentUserName == null) {
            LOG.info("Login cancelled; exiting.");
            Platform.exit();
            return;
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/guessmarket/client/views/main-view.fxml"));
        Parent root = loader.load();

        MainController mainController = loader.getController();
        mainController.setContext(engine, currentUserName);

        primaryStage.setTitle("Guess Market (client-server) — logged in as " + currentUserName);
        primaryStage.setScene(new Scene(root));
        // Below this the tabs can't lay out without clipping controls; smaller
        // than this and the user should scroll rather than resize.
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.show();
        LOG.info("Main window shown; talking to server at {}, logged in as '{}'", baseUrl, currentUserName);
    }

    /**
     * Blocking modal login: keeps prompting until the name is accepted (unique,
     * non-blank) or the user cancels, in which case the app never opens a window.
     */
    private String promptForLogin(MarketEngine engine) {
        while (true) {
            Optional<String> name = Dialogs.prompt("Guess Market Login", "Choose a unique user name", "");
            if (name.isEmpty()) {
                return null;
            }
            try {
                engine.login(name.get());
                return name.get().trim();
            } catch (MarketException ex) {
                LOG.warn("Login rejected: {}", ex.getMessage());
                Dialogs.error("Could not log in", ex);
            }
        }
    }
}
