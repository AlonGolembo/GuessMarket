module guess.market.engine {
    // 1. Export API and exceptions to consumers (javafx-ui, console, etc.)
    exports com.guessmarket.engine.api;
    exports com.guessmarket.engine.exception;

    // 2. Open JAXB package to JAXB runtime for XML unmarshalling
    opens com.guessmarket.engine.xml.jaxb to jakarta.xml.bind;

    // 3. Module dependencies
    requires guess.market.dto;
    requires jakarta.xml.bind;
    requires static org.jetbrains.annotations;

    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;
}