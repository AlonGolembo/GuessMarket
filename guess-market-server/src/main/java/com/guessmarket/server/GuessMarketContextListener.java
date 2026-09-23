package com.guessmarket.server;

import com.guessmarket.engine.api.MarketEngine;
import com.guessmarket.engine.api.MarketEngineImpl;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Creates the one {@link MarketEngine} instance every servlet shares for the
 * life of the deployment, wrapped for thread-safety since it now serves many
 * concurrent clients instead of one JavaFX thread. There is no persistence
 * wired in, so the market (events, users, everything) really does disappear
 * when the server stops - that's intentional, not a gap.
 */
@WebListener
public class GuessMarketContextListener implements ServletContextListener {

    static final String ENGINE_ATTRIBUTE = "marketEngine";

    private static final Logger LOG = LogManager.getLogger(GuessMarketContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent event) {
        MarketEngine engine = new SynchronizedMarketEngine(new MarketEngineImpl());
        event.getServletContext().setAttribute(ENGINE_ATTRIBUTE, engine);
        LOG.info("Guess Market server started; one shared MarketEngine instance is live.");
    }
}
