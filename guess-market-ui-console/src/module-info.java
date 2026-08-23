module guess.market.ui.console {
    requires guess.market.engine;
    requires guess.market.dto;

    requires org.junit.jupiter.api;

    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;

    opens com.guessmarket.ui.console.menu to org.junit.platform.commons;
    opens com.guessmarket.ui.console.view to org.junit.platform.commons;
}