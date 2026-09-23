module com.guessmarket.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires guess.market.engine;
    requires guess.market.dto;
    requires org.apache.logging.log4j;
    requires com.google.gson;
    requires java.net.http;

    // Opens FXML controllers to JavaFX loader reflection
    opens com.guessmarket.client.controllers to javafx.fxml;
    opens com.guessmarket.client to javafx.fxml;
    // Gson reflects over the request/response record types to (de)serialize them
    opens com.guessmarket.client.http to com.google.gson;

    exports com.guessmarket.client;
    exports com.guessmarket.client.http;
}