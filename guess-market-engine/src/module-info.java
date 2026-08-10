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
}