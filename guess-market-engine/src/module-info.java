/**
 * The engine. Only the API and its exceptions are exported; the domain model,
 * pricing, XML and persistence packages are internal.
 */
module guess.market.engine {
    exports com.guessmarket.engine.api;
    exports com.guessmarket.engine.exception;

    // JAXB reflects over the binding classes to unmarshal the XML.
    opens com.guessmarket.engine.xml.jaxb to jakarta.xml.bind;

    requires guess.market.dto;
    requires jakarta.xml.bind;
    requires static org.jetbrains.annotations;
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;
}