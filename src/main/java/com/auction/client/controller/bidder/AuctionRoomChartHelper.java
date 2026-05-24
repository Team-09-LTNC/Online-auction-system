package com.auction.client.controller.bidder;

import com.google.gson.JsonObject;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

final class AuctionRoomChartHelper {

    record BidHistoryEntry(String bidderName, long amount, String chartTime, String historyTime) {}

    private AuctionRoomChartHelper() {
    }

    static BidHistoryEntry parseBidHistoryEntry(
            JsonObject bidObj,
            DateTimeFormatter chartFormatter,
            DateTimeFormatter historyFormatter
    ) {
        String name = bidObj.has("fullName") ? bidObj.get("fullName").getAsString() :
                (bidObj.has("bidderName") ? bidObj.get("bidderName").getAsString() :
                        (bidObj.has("username") ? bidObj.get("username").getAsString() : "Người dùng ẩn danh"));

        long amount = bidObj.has("bidAmount") ? bidObj.get("bidAmount").getAsLong() :
                (bidObj.has("amount") ? bidObj.get("amount").getAsLong() : 0);

        String rawTime = bidObj.has("bidTime") ? bidObj.get("bidTime").getAsString() : LocalDateTime.now().toString();

        String chartTimeStr;
        String historyTimeStr;
        try {
            LocalDateTime dt = LocalDateTime.parse(rawTime);
            chartTimeStr = dt.format(chartFormatter);
            historyTimeStr = dt.format(historyFormatter);
        } catch (Exception e) {
            chartTimeStr = LocalDateTime.now().format(chartFormatter);
            historyTimeStr = LocalDateTime.now().format(historyFormatter);
        }
        return new BidHistoryEntry(name, amount, chartTimeStr, historyTimeStr);
    }

    static void setupHoverEffect(XYChart.Data<String, Number> dataPoint) {
        dataPoint.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) {
                String info = String.format("Thời gian: %s\nGiá: %,d đ", dataPoint.getXValue(), dataPoint.getYValue().longValue());
                Tooltip tooltip = new Tooltip(info);
                tooltip.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-background-color: linear-gradient(#3E2723, #5D4037); -fx-text-fill: white; -fx-padding: 8px; -fx-background-radius: 5px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 5);");

                tooltip.setShowDelay(Duration.ZERO);
                tooltip.setHideDelay(Duration.ZERO);
                Tooltip.install(newNode, tooltip);

                ScaleTransition st = new ScaleTransition(Duration.millis(600), newNode);
                st.setFromX(0);
                st.setFromY(0);
                st.setToX(1.0);
                st.setToY(1.0);
                st.setInterpolator(Interpolator.EASE_OUT);
                st.play();

                newNode.setOnMouseEntered(e -> {
                    newNode.setStyle("-fx-scale-x: 2.2; -fx-scale-y: 2.2; -fx-cursor: hand; -fx-background-color: #E65100, white;");
                    newNode.toFront();
                });

                newNode.setOnMouseExited(e -> {
                    newNode.setStyle("-fx-scale-x: 1.0; -fx-scale-y: 1.0; -fx-background-color: white, #A64452;");
                });
            }
        });
    }
}
