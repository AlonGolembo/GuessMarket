package DataTypes;

public class Option {
    private String optionTitle;
    private double optionValue;
    private int purchasedStocksAmount;

    public Option(String optionTitle) {
        this.optionTitle = optionTitle;
        this.optionValue = 0.5;
        this.purchasedStocksAmount = 0;
    }

    public String getOptionTitle() {
        return optionTitle;
    }

    public double getOptionValue() {
        return optionValue;
    }

    public void setOptionValue(double optionValue) {
        this.optionValue = optionValue;
    }

    public int getPurchasedStocksAmount() {
        return purchasedStocksAmount;
    }

    public void setPurchasedStocksAmount(int purchasedStocksAmount) {
        this.purchasedStocksAmount = purchasedStocksAmount;
    }
}
