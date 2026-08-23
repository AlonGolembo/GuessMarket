package engine.test.java;

import com.guessmarket.engine.lmsr.LmsrCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class LmsrCalculatorTest {

    private static final double EPSILON = 0.001;

    @Test
    @DisplayName("Initial 50-50 market option prices should be 0.5")
    void testInitialPrices() {
        double priceOption1 = LmsrCalculator.calculateOptionPrice(0, 0, 100);
        double priceOption2 = LmsrCalculator.calculateOptionPrice(0, 0, 100);

        assertEquals(0.5, priceOption1, EPSILON);
        assertEquals(0.5, priceOption2, EPSILON);
    }

    @Test
    @DisplayName("Buying 50 shares with b=100 should adjust prices to 62.25% and 37.75%")
    void testPriceAfterTrade() {
        double priceTarget = LmsrCalculator.calculateOptionPrice(50, 0, 100);
        double priceOther = LmsrCalculator.calculateOptionPrice(0, 50, 100);

        assertEquals(0.62245, priceTarget, EPSILON);
        assertEquals(0.37754, priceOther, EPSILON);
    }

    @Test
    @DisplayName("Cost to buy 50 shares with b=100 should equal $28.093")
    void testCalculateCostForTrade() {
        double initialCost = LmsrCalculator.calculateCost(0, 0, 100);
        double costAfterTrade = LmsrCalculator.calculateCost(50, 0, 100);
        double tradeCost = costAfterTrade - initialCost;

        assertEquals(28.0931, tradeCost, EPSILON);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    @DisplayName("Invalid liquidity parameter b <= 0 should throw IllegalArgumentException")
    void testInvalidLiquidityParameter(int invalidB) {
        assertThrows(IllegalArgumentException.class, () ->
                LmsrCalculator.calculateOptionPrice(0, 0, invalidB)
        );
        assertThrows(IllegalArgumentException.class, () ->
                LmsrCalculator.calculateCost(0, 0, invalidB)
        );
    }
}