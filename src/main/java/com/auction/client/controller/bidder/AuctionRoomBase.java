package com.auction.client.controller.bidder;

import com.google.gson.JsonObject;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

abstract class AuctionRoomBase {
  protected static final String SELLER_SELF_BID_MESSAGE = "Không được tự bid sản phẩm của chính mình.";
  protected static final String AUTO_BID_REGISTER_TEXT = "ĐĂNG KÝ AUTO-BID";
  protected static final String AUTO_BID_REMOVE_TEXT = "XÓA AUTO-BID";
  protected static final double IMAGE_FRAME_HEIGHT = 260.0;

  @FXML protected Label lblProductName;
  @FXML protected Label lblDescription;
  @FXML protected Label lblCategory;
  @FXML protected Label lblStartingPrice;
  @FXML protected Label lblBidIncrement;
  @FXML protected Label lblBuyNowPrice;
  @FXML protected Label lblAntiSniping;
  @FXML protected Label lblCurrentPrice;
  @FXML protected Label lblCountdown;
  @FXML protected Label lblLeader;
  @FXML protected ListView<String> lvBidHistory;
  @FXML protected TextField txtBidAmount;
  @FXML protected Button btnPlaceBid;
  @FXML protected TextField txtMaxAutoBid;
  @FXML protected TextField txtAutoBidStep;
  @FXML protected Button btnEnableAutoBid;
  @FXML protected ImageView imgProduct;
  @FXML protected StackPane imageFrame;
  @FXML protected AreaChart<String, Number> priceChart;
  @FXML protected CategoryAxis timeAxis;
  @FXML protected NumberAxis priceAxis;

  protected XYChart.Series<String, Number> priceSeries;
  protected final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm-dd");
  protected final DateTimeFormatter historyTimeFormatter = DateTimeFormatter.ofPattern("dd HH:mm:ss");
  protected int totalSeconds = 0;
  protected Timeline countdownTimeline;
  protected int currentAuctionId = -1;
  protected boolean isAuctionStarted = false;
  protected String currentStatus = "OPEN";
  protected Long currentBuyNowPrice;
  protected int currentSellerId = -1;
  protected long currentDisplayedPrice = 0;
  protected long currentBidIncrement = 0;
  protected boolean hasCurrentWinner = false;
  protected boolean currentUserOwnsAuction = false;
  protected boolean ownerBidWarningShown = false;
  protected boolean autoBidActive = false;
  protected final Set<String> displayedBidKeys = new HashSet<>();
  protected AuctionRoomImageHelper productImageHelper;
  protected AuctionRoomImageLoader imageLoader;

  protected abstract void refreshAuctionState();

  protected abstract void loadBidHistoryFromServer();

  protected void startCountdown() {
    if (countdownTimeline != null) {
      countdownTimeline.stop();
    }

    countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
      if (totalSeconds > 0) {
        totalSeconds--;
        updateCountdownLabel();
        if (isAuctionStarted && totalSeconds <= 30 && lblCountdown != null) {
          lblCountdown.setStyle("-fx-text-fill: #A64452; -fx-font-weight: bold;");
        }
      } else {
        countdownTimeline.stop();
        if ("OPEN".equalsIgnoreCase(currentStatus)) {
          refreshAuctionState();
        } else {
          setExpiredUI();
        }
      }
    }));
    countdownTimeline.setCycleCount(Timeline.INDEFINITE);
    countdownTimeline.play();
    updateCountdownLabel();
  }

  protected void updateCountdownLabel() {
    AuctionRoomViewHelper.updateCountdownLabel(lblCountdown, isAuctionStarted, totalSeconds);
  }

  protected void setExpiredUI() {
    isAuctionStarted = false;
    totalSeconds = 0;
    AuctionRoomViewHelper.setExpiredUI(lblCountdown, btnPlaceBid, btnEnableAutoBid, txtBidAmount);
  }

  protected long getMinimumManualBid() {
    long currentPrice = currentDisplayedPrice > 0
        ? currentDisplayedPrice
        : AuctionRoomMoneyFormatter.extractMoneyValue(lblCurrentPrice.getText());
    long stepPrice = currentBidIncrement > 0
        ? currentBidIncrement
        : AuctionRoomMoneyFormatter.extractMoneyValue(lblBidIncrement.getText());
    return hasCurrentWinner ? currentPrice + stepPrice : currentPrice;
  }

  protected String resolveUserDisplayName(JsonObject userObj) {
    if (AuctionJsonReader.hasValue(userObj, "fullName")) {
      return userObj.get("fullName").getAsString();
    }
    if (AuctionJsonReader.hasValue(userObj, "username")) {
      return userObj.get("username").getAsString();
    }
    if (AuctionJsonReader.hasValue(userObj, "id")) {
      return "Người dùng #" + userObj.get("id").getAsInt();
    }
    return "Người dùng ẩn danh";
  }

  public void loadProductImage(String urlString) {
    loadProductImage(urlString, null);
  }

  public void loadProductImage(String detailUrl, String fallbackUrl) {
    if (imageLoader == null) {
      imageLoader = new AuctionRoomImageLoader(imgProduct);
    }
    imageLoader.load(detailUrl, fallbackUrl);
  }

  protected void showAlert(String title, String content) {
    AuctionRoomViewHelper.showWarning(title, content);
  }
}
