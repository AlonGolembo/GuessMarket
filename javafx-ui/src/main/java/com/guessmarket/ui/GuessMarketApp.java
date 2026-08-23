package com.guessmarket.ui;

import com.guessmarket.engine.api.MarketEngine;
import com.guessmarket.engine.api.MarketEngineImpl;
import com.guessmarket.ui.controllers.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GuessMarketApp extends Application {

    public static void main(String[] args) {
        launch(args);
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
        primaryStage.show();
    }
}
