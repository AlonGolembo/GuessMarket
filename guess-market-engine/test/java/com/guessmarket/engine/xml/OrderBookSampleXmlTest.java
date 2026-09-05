package com.guessmarket.engine.xml;

import com.guessmarket.engine.model.Event;
import com.guessmarket.engine.model.OrderBookMethod;
import com.guessmarket.engine.model.OrderOutcome;
import com.guessmarket.engine.model.User;
import com.guessmarket.dto.OrderSide;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Loads the shipped {@code order-book-market.xml} sample (referenced from
 * README.md) and drives it through a full lifecycle, so the sample stays
 * valid and demonstrative as the format evolves.
 */
class OrderBookSampleXmlTest {

    private Path samplePath() throws URISyntaxException {
        return Paths.get(getClass().getResource("/order-book-market.xml").toURI());
    }

    @Test
    void sampleParsesOpensTradesAndSettles() throws URISyntaxException {
        ParsedMarket market = GuessMarketXmlParser.parseAndValidateXml(samplePath().toString());

        Event event = market.events().get(1);
        assertEquals("Will it rain tomorrow?", event.getName());
        assertTrue(event.getTradingMethod() instanceof OrderBookMethod);

        User mm = market.users().get("mm");
        User alice = market.users().get("alice");
        User bob = market.users().get("bob");

        event.open(mm);
        assertEquals(900.0, mm.getAccountBalance(), 1e-9);              // paid 100 pairs @ $1
        assertArrayEquals(new int[]{100, 100}, event.holdingsOf("mm"));

        // Alice crosses the market maker's resting ask directly.
        OrderOutcome outcome = event.placeOrder(alice, 0, OrderSide.BID, 20, 0.6);
        assertEquals(20, outcome.filledQuantity());

        // Bob market-buys the other option.
        var receipt = event.buy(bob, 1, 15);
        assertEquals(15, receipt.filledQuantity());

        event.settleAndClose(0);   // Rain wins

        assertEquals(0.0, event.getEventAccountBalance(), 1e-9);
    }
}
