package com.guessmarket.engine.lmsr;

public class LmsrCalculator {

    /**
     * Calculates the market cost function C(q_yes, q_no) for a given state and liquidity b.
     * C(q_yes, q_no) = b * ln( e^(q_yes / b) + e^(q_no / b) )
     */
    public static double calculateCost(int qYes, int qNo, int b) {
        if (b <= 0) {
            throw new IllegalArgumentException("Liquidity parameter b must be strictly positive. Got: " + b);
        }
        double expYes = Math.exp((double) qYes / b);
        double expNo = Math.exp((double) qNo / b);
        return b * Math.log(expYes + expNo);
    }

    /**
     * Calculates the current price (implied probability) of a target option.
     * p_target = e^(q_target / b) / ( e^(q_yes / b) + e^(q_no / b) )
     */
    public static double calculateOptionPrice(int qTarget, int qOther, int b) {
        if (b <= 0) {
            throw new IllegalArgumentException("Liquidity parameter b must be strictly positive. Got: " + b);
        }
        double expTarget = Math.exp((double) qTarget / b);
        double expOther = Math.exp((double) qOther / b);
        return expTarget / (expTarget + expOther);
    }

    /**
     * Calculates the cost to purchase a specific quantity of shares for an option.
     * Returns: Cost_after - Cost_before
     */
    public static double calculateTradeCost(int currentQYes, int currentQNo, int b, boolean isYesOption, int quantityToBuy) {
        if (quantityToBuy <= 0) {
            throw new IllegalArgumentException("Quantity to buy must be positive. Got: " + quantityToBuy);
        }

        double costBefore = calculateCost(currentQYes, currentQNo, b);

        int newQYes = isYesOption ? currentQYes + quantityToBuy : currentQYes;
        int newQNo = !isYesOption ? currentQNo + quantityToBuy : currentQNo;

        double costAfter = calculateCost(newQYes, newQNo, b);

        return costAfter - costBefore;
    }

    /**
     * Calculates the initial Market Maker subsidy required to initialize an LMSR market.
     * C(0, 0) = b * ln(2)
     */
    public static double calculateInitialSubsidy(int b) {
        return calculateCost(0, 0, b);
    }
}