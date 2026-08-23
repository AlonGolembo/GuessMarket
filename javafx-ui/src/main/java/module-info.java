module com.guessmarket.ui {
    requires javafx.controls;
    requires javafx.fxml;
    requires guess.market.engine;
    requires guess.market.dto;

    // Opens FXML controllers to JavaFX loader reflection
    opens com.guessmarket.ui.controllers to javafx.fxml;
    opens com.guessmarket.ui to javafx.fxml;

    exports com.guessmarket.ui;
}