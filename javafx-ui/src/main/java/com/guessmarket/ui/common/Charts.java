package com.guessmarket.ui.common;

import com.guessmarket.dto.EventDetailsDTO;
import com.guessmarket.dto.TradeHistoryDTO;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

import java.util.List;

/** Builds the JavaFX charts shown in the Events tab. */
public class Charts {

    private Charts() {}

    /**
     * A line chart of one option's traded price over time. Each point is a single
     * trade, priced per share ({@code pricePaid / quantity}); the x-axis walks the
     * trades oldest-to-newest. Returns an empty (but labelled) chart when the
     * option has no trades yet.
     */
    public static LineChart<String, Number> createPriceHistoryChart(String optionName, EventDetailsDTO eventDetails) {
        // tradeHistory is newest-first - reverse it so the timeline reads left-to-right.
        List<TradeHistoryDTO> trades = eventDetails.tradeHistory().stream()
                .filter(trade -> optionName.equals(trade.optionName()))
                .toList();

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Time");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Price per share ($)");

        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle(optionName);
        lineChart.setAnimated(false);   // prevents rendering glitches when the series is swapped
        lineChart.setLegendVisible(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(optionName);

        for (int i = trades.size() - 1; i >= 0; i--) {
            TradeHistoryDTO trade = trades.get(i);
            int seq = trades.size() - i;   // 1-based, oldest trade first
            double pricePerShare = trade.quantity() > 0
                    ? trade.pricePaid() / trade.quantity()
                    : trade.pricePaid();
            // Prefix with the sequence number so same-minute trades stay distinct on the axis.
            String label = seq + ". " + trade.timestamp();
            series.getData().add(new XYChart.Data<>(label, pricePerShare));
        }

        lineChart.getData().add(series);
        return lineChart;
    }
}
