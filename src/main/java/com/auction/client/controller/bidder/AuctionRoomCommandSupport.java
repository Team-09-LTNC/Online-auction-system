package com.auction.client.controller.bidder;

import com.auction.client.networkclient.ClientSocket;
import com.auction.common.dto.AuctionDTOs;
import com.auction.common.enums.ActionType;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;

abstract class AuctionRoomCommandSupport extends AuctionRoomHistorySupport {
  @FXML
  protected void handlePlaceBid() {
    if (!isAuctionStarted || currentAuctionId == -1) {
      return;
    }
    if (currentUserOwnsAuction) {
      showAlert("Không thể đặt giá", SELLER_SELF_BID_MESSAGE);
      return;
    }
    String input = txtBidAmount.getText().trim();
    if (input.isEmpty()) {
      return;
    }

    try {
      long bidAmount = AuctionRoomMoneyFormatter.parseMoneyValue(input);
      long minValidBid = getMinimumManualBid();
      if (currentBuyNowPrice != null && currentBuyNowPrice > 0 && bidAmount >= currentBuyNowPrice) {
        if (AuctionRoomViewHelper.confirmBuyNow()) {
          sendBuyNowConfirmation();
        }
        return;
      }
      if (bidAmount < minValidBid) {
        showAlert("Lỗi đặt giá", "Giá tối thiểu: " + AuctionRoomMoneyFormatter.formatVnd(minValidBid));
        return;
      }
      sendBidRequest(bidAmount);
    } catch (NumberFormatException e) {
      showAlert("Lỗi", "Số tiền không hợp lệ.");
    }
  }

  private void sendBidRequest(long bidAmount) {
    AuctionDTOs.BidRequest request = new AuctionDTOs.BidRequest(currentAuctionId, bidAmount, 1);
    btnPlaceBid.setDisable(true);
    JsonObject jsonRequest = new Gson().toJsonTree(request).getAsJsonObject();
    jsonRequest.addProperty("type", ActionType.PLACE_BID);
    jsonRequest.addProperty("requestId", java.util.UUID.randomUUID().toString());

    ClientSocket.getInstance().sendJsonRequest(jsonRequest, "BID_RESPONSE", response ->
        Platform.runLater(() -> {
          btnPlaceBid.setDisable(false);
          if (!(response.has("success") && response.get("success").getAsBoolean())) {
            showAlert("Lỗi", response.has("message") ? response.get("message").getAsString() : "Lỗi đặt giá.");
          } else {
            refreshAuctionState();
            loadBidHistoryFromServer();
            txtBidAmount.clear();
          }
        }));
  }

  private void sendBuyNowConfirmation() {
    if (currentUserOwnsAuction) {
      showAlert("Không thể mua đứt", SELLER_SELF_BID_MESSAGE);
      return;
    }
    JsonObject request = new JsonObject();
    request.addProperty("type", ActionType.CONFIRM_BUY_NOW);
    request.addProperty("auctionId", currentAuctionId);
    request.addProperty("requestId", java.util.UUID.randomUUID().toString());

    if (btnPlaceBid != null) {
      btnPlaceBid.setDisable(true);
    }
    ClientSocket.getInstance().sendJsonRequest(request, "BUY_NOW_RESPONSE", response ->
        Platform.runLater(() -> handleBuyNowResponse(response)));
  }

  private void handleBuyNowResponse(JsonObject response) {
    if (btnPlaceBid != null) {
      btnPlaceBid.setDisable(false);
    }
    if (!(response.has("success") && response.get("success").getAsBoolean())) {
      showAlert("Không thể mua đứt",
          response.has("message") ? response.get("message").getAsString() : "Mua đứt thất bại.");
      return;
    }

    AuctionRoomViewHelper.showInfo(
        "Chiến thắng phiên đấu giá",
        "Chúc mừng! Bạn đã là người chiến thắng ở phiên đấu giá này.",
        "Vui lòng thanh toán để chính thức sở hữu sản phẩm.\n\n"
            + "Nếu hủy thanh toán, bạn sẽ chịu phạt 10% tiền đặt giá.");
    if (txtBidAmount != null) {
      txtBidAmount.clear();
    }
    refreshAuctionState();
    loadBidHistoryFromServer();
  }

  @FXML
  protected void handleEnableAutoBid() {
    if (!isAuctionStarted || currentAuctionId == -1 || txtMaxAutoBid == null) {
      return;
    }
    if (currentUserOwnsAuction) {
      showAlert("Không thể Auto-bid", SELLER_SELF_BID_MESSAGE);
      return;
    }
    if (btnEnableAutoBid != null && AUTO_BID_REMOVE_TEXT.equals(btnEnableAutoBid.getText())) {
      removeAutoBid();
      return;
    }
    if (txtMaxAutoBid.getText().trim().isEmpty()) {
      return;
    }
    if (txtAutoBidStep == null || txtAutoBidStep.getText().trim().isEmpty()) {
      showAlert("Lỗi Auto-bid", "Vui lòng nhập bước giá Auto-bid.");
      return;
    }
    registerAutoBid();
  }

  private void registerAutoBid() {
    try {
      long maxPrice = AuctionRoomMoneyFormatter.parseMoneyValue(txtMaxAutoBid.getText());
      long bidStep = AuctionRoomMoneyFormatter.parseMoneyValue(txtAutoBidStep.getText());
      if (!validateAutoBidInput(maxPrice, bidStep)) {
        return;
      }

      JsonObject jsonRequest = new JsonObject();
      jsonRequest.addProperty("type", ActionType.REGISTER_AUTO_BID);
      jsonRequest.addProperty("auctionId", currentAuctionId);
      jsonRequest.addProperty("maxBid", maxPrice);
      jsonRequest.addProperty("bidStep", bidStep);
      jsonRequest.addProperty("requestId", java.util.UUID.randomUUID().toString());
      btnEnableAutoBid.setDisable(true);

      ClientSocket.getInstance().sendJsonRequest(jsonRequest, "AUTO_BID_RESPONSE", response ->
          Platform.runLater(() -> handleRegisterAutoBidResponse(response)));
    } catch (Exception e) {
      showAlert("Lỗi Auto-bid", "Vui lòng nhập số hợp lệ cho mức tối đa và bước giá.");
    }
  }

  private boolean validateAutoBidInput(long maxPrice, long bidStep) {
    long currentPrice = AuctionRoomMoneyFormatter.extractMoneyValue(lblCurrentPrice.getText());
    long sellerBidStep = AuctionRoomMoneyFormatter.extractMoneyValue(lblBidIncrement.getText());
    long minValidBid = currentPrice + sellerBidStep;
    long autoNextBid = currentPrice + Math.max(bidStep, sellerBidStep);
    boolean autoBidWouldReachBuyNow = currentBuyNowPrice != null
        && currentBuyNowPrice > 0
        && currentPrice < currentBuyNowPrice
        && autoNextBid >= currentBuyNowPrice
        && maxPrice >= currentBuyNowPrice;
    if (!autoBidWouldReachBuyNow && maxPrice < minValidBid) {
      showAlert("Lỗi Auto-bid", "Mức giá tối đa phải >= " + AuctionRoomMoneyFormatter.formatVnd(minValidBid));
      return false;
    }
    if (bidStep < sellerBidStep) {
      showAlert("Lỗi Auto-bid", "Bước giá Auto-bid phải >= bước giá người bán ("
          + AuctionRoomMoneyFormatter.formatVnd(sellerBidStep) + ").");
      return false;
    }
    return true;
  }

  private void handleRegisterAutoBidResponse(JsonObject response) {
    btnEnableAutoBid.setDisable(false);
    if (response.has("success") && response.get("success").getAsBoolean()) {
      Alert alert = new Alert(Alert.AlertType.INFORMATION);
      alert.setTitle("Thành công");
      alert.setHeaderText(null);
      alert.setContentText("Kích hoạt Auto-bid thành công!");
      alert.showAndWait();
      autoBidActive = true;
      btnEnableAutoBid.setText(AUTO_BID_REMOVE_TEXT);
      if (btnPlaceBid != null) {
        btnPlaceBid.setDisable(true);
        btnPlaceBid.setText("ĐANG AUTO-BID");
      }
      if (txtBidAmount != null) {
        txtBidAmount.setEditable(false);
      }
    } else {
      showAlert("Lỗi Auto-bid", response.has("message") ? response.get("message").getAsString() : "Lỗi đăng ký.");
    }
  }

  private void removeAutoBid() {
    JsonObject request = new JsonObject();
    request.addProperty("type", ActionType.REMOVE_AUTO_BID);
    request.addProperty("auctionId", currentAuctionId);
    request.addProperty("requestId", java.util.UUID.randomUUID().toString());
    btnEnableAutoBid.setDisable(true);
    ClientSocket.getInstance().sendJsonRequest(request, "AUTO_BID_RESPONSE", response ->
        Platform.runLater(() -> handleRemoveAutoBidResponse(response)));
  }

  private void handleRemoveAutoBidResponse(JsonObject response) {
    btnEnableAutoBid.setDisable(false);
    if (!(response.has("success") && response.get("success").getAsBoolean())) {
      showAlert("Lỗi Auto-bid", response.has("message")
          ? response.get("message").getAsString() : "Không thể xóa Auto-bid.");
      return;
    }
    if (txtMaxAutoBid != null) {
      txtMaxAutoBid.clear();
    }
    if (txtAutoBidStep != null) {
      txtAutoBidStep.clear();
    }
    autoBidActive = false;
    btnEnableAutoBid.setText(AUTO_BID_REGISTER_TEXT);
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle("Thành công");
    alert.setHeaderText(null);
    alert.setContentText("Đã xóa đăng ký Auto-bid.");
    alert.showAndWait();
    if (!currentUserOwnsAuction && isAuctionStarted) {
      if (btnPlaceBid != null) {
        btnPlaceBid.setDisable(false);
        btnPlaceBid.setText("ĐẶT GIÁ");
      }
      if (txtBidAmount != null) {
        txtBidAmount.setEditable(true);
      }
    }
  }
}
