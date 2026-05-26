package com.auction.client.controller.bidder;

import com.auction.client.networkclient.ClientSocket;
import com.auction.client.util.AuctionTimeUtil;
import com.auction.common.enums.ActionType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.scene.chart.XYChart;

abstract class AuctionRoomHistorySupport extends AuctionRoomSnapshotSupport {
  @Override
  protected void loadBidHistoryFromServer() {
    if (currentAuctionId == -1) {
      return;
    }

    JsonObject historyReq = new JsonObject();
    historyReq.addProperty("type", ActionType.GET_BID_HISTORY);
    historyReq.addProperty("auctionId", currentAuctionId);
    historyReq.addProperty("requestId", java.util.UUID.randomUUID().toString());
    ClientSocket.getInstance().sendJsonRequest(historyReq, ActionType.GET_BID_HISTORY, response ->
        Platform.runLater(() -> applyBidHistoryResponse(response)));
  }

  private void applyBidHistoryResponse(JsonObject response) {
    if (!(response.has("success") && response.get("success").getAsBoolean()) || !response.has("data")) {
      return;
    }

    JsonArray historyArray = response.getAsJsonArray("data");
    if (lvBidHistory != null) {
      lvBidHistory.getItems().clear();
    }
    displayedBidKeys.clear();
    if (priceChart != null && priceSeries != null) {
      priceChart.getData().remove(priceSeries);
    }
    priceSeries = new XYChart.Series<>();
    priceSeries.setName("Giá đấu");

    List<XYChart.Data<String, Number>> chartPoints = new ArrayList<>();
    String latestLeader = null;
    for (JsonElement el : historyArray) {
      AuctionRoomChartHelper.BidHistoryEntry entry =
          AuctionRoomChartHelper.parseBidHistoryEntry(el.getAsJsonObject(), timeFormatter, historyTimeFormatter);
      latestLeader = entry.bidderName();
      addBidHistoryEntry(entry.bidderName(), entry.amount(), entry.historyTime());
      XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(entry.chartTime(), entry.amount());
      AuctionRoomChartHelper.setupHoverEffect(dataPoint);
      chartPoints.add(dataPoint);
    }

    priceSeries.getData().addAll(chartPoints);
    if (priceChart != null) {
      priceChart.getData().add(priceSeries);
    }
    if (lblLeader != null) {
      lblLeader.setText(latestLeader != null && !latestLeader.isBlank()
          ? "Người dẫn đầu: " + latestLeader
          : "Chưa có ai đặt giá");
    }
    hasCurrentWinner = latestLeader != null && !latestLeader.isBlank();
  }

  public void updateRealtimeBid(
      long newPrice,
      String bidderName,
      String endTime,
      String serverNow,
      String status,
      int auctionId,
      String bidTime
  ) {
    if (auctionId > 0 && currentAuctionId != auctionId) {
      return;
    }

    if (lblCurrentPrice != null) {
      lblCurrentPrice.setText(AuctionRoomMoneyFormatter.formatVnd(newPrice));
    }
    currentDisplayedPrice = newPrice;
    hasCurrentWinner = true;
    if (lblLeader != null) {
      lblLeader.setText("Người dẫn đầu: " + bidderName);
    }

    LocalDateTime bidDateTime = AuctionTimeUtil.parse(bidTime);
    if (bidDateTime == null) {
      bidDateTime = LocalDateTime.now();
    }
    String historyTime = bidDateTime.format(historyTimeFormatter);
    boolean addedHistoryEntry = addBidHistoryEntry(bidderName, newPrice, historyTime);
    if (!addedHistoryEntry) {
      applyServerCountdown(endTime, serverNow, status);
      return;
    }

    applyServerCountdown(endTime, serverNow, status);
    if (priceSeries != null) {
      XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(bidDateTime.format(timeFormatter), newPrice);
      AuctionRoomChartHelper.setupHoverEffect(dataPoint);
      priceSeries.getData().add(dataPoint);
      if (priceSeries.getData().size() > 30) {
        priceSeries.getData().remove(0);
      }
    }
  }

  protected boolean addBidHistoryEntry(String bidderName, long bidAmount, String historyTime) {
    String key = bidderName + "|" + bidAmount + "|" + historyTime;
    if (!displayedBidKeys.add(key)) {
      return false;
    }
    if (lvBidHistory != null) {
      lvBidHistory.getItems().add(
          0,
          "(" + historyTime + ") " + bidderName + " đã đặt: "
              + AuctionRoomMoneyFormatter.formatVnd(bidAmount));
    }
    return true;
  }

  protected void applyServerCountdown(String endTime, String serverNow, String status) {
    if (endTime == null || endTime.isBlank()) {
      refreshAuctionState();
      return;
    }

    LocalDateTime serverTime = AuctionTimeUtil.parse(serverNow);
    LocalDateTime serverEndTime = AuctionTimeUtil.parse(endTime);
    if (serverTime == null || serverEndTime == null) {
      refreshAuctionState();
      return;
    }

    int newTotalSeconds = (int) Math.max(0, ChronoUnit.SECONDS.between(serverTime, serverEndTime));
    totalSeconds = newTotalSeconds;
    currentStatus = status == null || status.isBlank() ? "RUNNING" : status;
    isAuctionStarted = "RUNNING".equalsIgnoreCase(currentStatus);

    if (newTotalSeconds <= 0 || !"RUNNING".equalsIgnoreCase(currentStatus)) {
      setExpiredUI();
      return;
    }

    boolean allowManualBid = !autoBidActive && !currentUserOwnsAuction;
    if (btnPlaceBid != null) {
      btnPlaceBid.setDisable(!allowManualBid);
      btnPlaceBid.setText(autoBidActive ? "ĐANG AUTO-BID" : "ĐẶT GIÁ");
    }
    if (txtBidAmount != null) {
      txtBidAmount.setEditable(allowManualBid);
    }
    if (btnEnableAutoBid != null) {
      btnEnableAutoBid.setDisable(false);
    }
    startCountdown();
  }

  public void updateRealtimeStatus(String newStatus) {
    currentStatus = newStatus;
    if ("FINISHED".equalsIgnoreCase(newStatus)
        || "PAID".equalsIgnoreCase(newStatus)
        || "CANCELED".equalsIgnoreCase(newStatus)) {
      setExpiredUI();
    }
  }
}
