package com.guessmarket.ui;

import com.guessmarket.engine.api.MarketEngine;
import com.guessmarket.engine.api.MarketEngineImpl;
import com.guessmarket.ui.controllers.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GuessMarketApp extends Application {

    private static final Logger LOG = LogManager.getLogger(GuessMarketApp.class);

    public static void main(String[] args) {
        LOG.info("Guess Market starting");
        launch(args);
        LOG.info("Guess Market stopped");
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        MarketEngine engine = new MarketEngineImpl();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/guessmarket/ui/views/main-view.fxml"));
        Parent root = loader.load();

        MainController mainController = loader.getController();
        mainController.setEngine(engine);

        primaryStage.setTitle("Guess Market");
        primaryStage.setScene(new Scene(root));
        // Below this the tabs can't lay out without clipping controls; smaller
        // than this and the user should scroll rather than resize.
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.show();
        LOG.info("Main window shown");
    }
}
