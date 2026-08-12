module guess.market.engine {
    // 1. Export ONLY the API package to external modules
    exports com.guessmarket.engine.api;
    exports com.guessmarket.engine.exception; // If UI needs to catch MarketException

    // 2. Open JAXB package to the JAXB runtime for reflection
    opens com.guessmarket.engine.xml.jaxb to jakarta.xml.bind;

    // 3. Require external module dependencies (e.g., DTO module)
    requires guess.market.dto;
    requires jakarta.xml.bind;
    requires org.jetbrains.annotations;
    requires org.junit.jupiter.api;
    requires org.junit.jupiter.params;

    opens com.guessmarket.engine.lmsr to org.junit.platform.commons;
    opens com.guessmarket.engine.xml to org.junit.platform.commons;
}