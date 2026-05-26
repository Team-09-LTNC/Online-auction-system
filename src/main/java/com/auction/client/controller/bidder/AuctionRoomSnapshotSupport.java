package com.auction.client.controller.bidder;

import com.auction.client.networkclient.ClientSocket;
import com.auction.client.util.AuctionTimeUtil;
import com.auction.common.enums.ActionType;
import com.google.gson.JsonObject;
import javafx.application.Platform;

abstract class AuctionRoomSnapshotSupport extends AuctionRoomBase {
  protected void applyAuctionSnapshot(JsonObject snapshot, String fallbackImageUrl) {
    String imageUrl = AuctionJsonReader.getString(snapshot, "imageUrl", fallbackImageUrl);
    String imageThumbUrl = AuctionJsonReader.getString(snapshot, "imageThumbUrl", imageUrl);
    loadProductImage(imageUrl, imageThumbUrl);

    if (lblProductName != null) {
      lblProductName.setText(AuctionJsonReader.getString(
          snapshot, "itemName", AuctionJsonReader.getString(snapshot, "name", "Sản phẩm")));
    }
    if (lblDescription != null) {
      lblDescription.setText(AuctionJsonReader.getString(snapshot, "description", "Đang đồng bộ mô tả..."));
    }
    if (lblCategory != null) {
      lblCategory.setText("Danh mục: " + AuctionJsonReader.getString(snapshot, "category", "Khác"));
    }

    long startingPrice = AuctionJsonReader.getLong(
        snapshot, "startingPrice", AuctionJsonReader.getLong(snapshot, "currentPrice", 0));
    long bidIncrement = AuctionJsonReader.getLong(snapshot, "bidIncrement", 0);
    currentBidIncrement = bidIncrement;
    if (lblStartingPrice != null) {
      lblStartingPrice.setText(AuctionRoomMoneyFormatter.formatVnd(startingPrice));
    }
    if (lblBidIncrement != null) {
      lblBidIncrement.setText(AuctionRoomMoneyFormatter.formatVnd(bidIncrement));
    }

    currentBuyNowPrice = AuctionJsonReader.hasValue(snapshot, "buyNowPrice")
        ? snapshot.get("buyNowPrice").getAsLong()
        : null;
    if (lblBuyNowPrice != null) {
      lblBuyNowPrice.setText(currentBuyNowPrice != null
          ? AuctionRoomMoneyFormatter.formatVnd(currentBuyNowPrice)
          : "Không hỗ trợ");
    }
    if (lblAntiSniping != null) {
      lblAntiSniping.setText(AuctionJsonReader.getBoolean(snapshot, "antiSnipingEnabled", false) ? "Có" : "Không");
    }

    long displayPrice = AuctionJsonReader.getLong(
        snapshot, "currentHighestBid", AuctionJsonReader.getLong(snapshot, "currentPrice", startingPrice));
    currentDisplayedPrice = displayPrice;
    hasCurrentWinner = false;
    if (lblCurrentPrice != null) {
      lblCurrentPrice.setText(AuctionRoomMoneyFormatter.formatVnd(displayPrice));
    }
    if (lblLeader != null) {
      lblLeader.setText("Chưa có ai đặt giá");
    }

    currentSellerId = (int) AuctionJsonReader.getLong(snapshot, "sellerId", -1);
    currentUserOwnsAuction =
        "SELLER".equalsIgnoreCase(com.auction.client.controller.auth.UserSession.getCurrentRole())
            && currentSellerId == com.auction.client.controller.auth.UserSession.getUserId();

    AuctionTimeUtil.AuctionState state = AuctionTimeUtil.calculateState(
        AuctionJsonReader.getString(snapshot, "startTime", null),
        AuctionJsonReader.getString(snapshot, "endTime", null),
        AuctionJsonReader.getString(snapshot, "serverNow", null));
    totalSeconds = (int) AuctionJsonReader.getLong(snapshot, "countdownSeconds", state.countdownSeconds);
    currentStatus = AuctionJsonReader.getString(
        snapshot, "displayStatus", AuctionJsonReader.getString(snapshot, "status", state.finalStatus));
    applyInteractionState(false);
  }

  protected void applyInteractionState(boolean showOwnerWarning) {
    if ("OPEN".equalsIgnoreCase(currentStatus) || "RUNNING".equalsIgnoreCase(currentStatus)) {
      isAuctionStarted = "RUNNING".equalsIgnoreCase(currentStatus);
      if (btnPlaceBid != null) {
        btnPlaceBid.setDisable(!isAuctionStarted || currentUserOwnsAuction);
        btnPlaceBid.setText(isAuctionStarted ? "ĐẶT GIÁ" : "CHỜ MỞ BÁN");
      }
      if (txtBidAmount != null) {
        txtBidAmount.setEditable(isAuctionStarted && !currentUserOwnsAuction);
      }
      if (txtMaxAutoBid != null) {
        txtMaxAutoBid.setEditable(isAuctionStarted && !currentUserOwnsAuction);
      }
      if (btnEnableAutoBid != null) {
        btnEnableAutoBid.setDisable(!isAuctionStarted || currentUserOwnsAuction);
      }
      if (currentUserOwnsAuction && showOwnerWarning && !ownerBidWarningShown) {
        ownerBidWarningShown = true;
        showAlert("Không thể đặt giá", SELLER_SELF_BID_MESSAGE);
      }
      startCountdown();
    } else {
      setExpiredUI();
    }
  }

  @Override
  protected void refreshAuctionState() {
    if (currentAuctionId == -1) {
      return;
    }

    JsonObject getReq = new JsonObject();
    getReq.addProperty("type", ActionType.GET_AUCTION_BY_ID);
    getReq.addProperty("auctionId", currentAuctionId);
    getReq.addProperty("requestId", java.util.UUID.randomUUID().toString());
    ClientSocket.getInstance().sendJsonRequest(getReq, ActionType.GET_AUCTION_BY_ID, response ->
        Platform.runLater(() -> applyAuctionStateResponse(response)));
  }

  private void applyAuctionStateResponse(JsonObject response) {
    if (!(response.has("success") && response.get("success").getAsBoolean())) {
      return;
    }

    JsonObject data = response.getAsJsonObject("data");
    JsonObject itemData = data.getAsJsonObject("item");
    applyItemData(data, itemData);
    applyLeaderData(data);
    applyTimeState(response, data);
  }

  private void applyItemData(JsonObject data, JsonObject itemData) {
    currentSellerId = AuctionJsonReader.hasValue(itemData, "sellerId") ? itemData.get("sellerId").getAsInt() : -1;
    currentUserOwnsAuction =
        "SELLER".equalsIgnoreCase(com.auction.client.controller.auth.UserSession.getCurrentRole())
            && currentSellerId == com.auction.client.controller.auth.UserSession.getUserId();
    if (lblProductName != null) {
      lblProductName.setText(AuctionJsonReader.getString(itemData, "name", "Sản phẩm"));
    }
    String imageUrl = AuctionJsonReader.getString(itemData, "imageUrl", null);
    loadProductImage(imageUrl, AuctionJsonReader.getString(itemData, "imageThumbUrl", imageUrl));
    if (lblDescription != null) {
      lblDescription.setText(AuctionJsonReader.getString(itemData, "description", "Không có mô tả."));
    }
    if (lblCategory != null) {
      lblCategory.setText("Danh mục: " + AuctionJsonReader.getString(itemData, "category", "Khác"));
    }

    long startingPrice = AuctionJsonReader.getLong(itemData, "startingPrice", 0);
    currentBidIncrement = AuctionJsonReader.getLong(itemData, "bidIncrement", 0);
    if (lblStartingPrice != null) {
      lblStartingPrice.setText(AuctionRoomMoneyFormatter.formatVnd(startingPrice));
    }
    if (lblBidIncrement != null) {
      lblBidIncrement.setText(AuctionRoomMoneyFormatter.formatVnd(currentBidIncrement));
    }
    currentBuyNowPrice = AuctionJsonReader.hasValue(data, "buyNowPrice") ? data.get("buyNowPrice").getAsLong() : null;
    if (lblBuyNowPrice != null) {
      lblBuyNowPrice.setText(currentBuyNowPrice != null
          ? AuctionRoomMoneyFormatter.formatVnd(currentBuyNowPrice)
          : "Không hỗ trợ");
    }
    if (lblAntiSniping != null) {
      lblAntiSniping.setText(AuctionJsonReader.getBoolean(data, "antiSnipingEnabled", false) ? "Có" : "Không");
    }
    currentDisplayedPrice = AuctionJsonReader.getLong(data, "currentHighestBid", startingPrice);
  }

  private void applyLeaderData(JsonObject data) {
    String leaderText = "Chưa có ai đặt giá";
    JsonObject userObj = AuctionJsonReader.hasValue(data, "currentWinner") ? data.getAsJsonObject("currentWinner")
        : AuctionJsonReader.hasValue(data, "currentHighestBidder") ? data.getAsJsonObject("currentHighestBidder")
        : AuctionJsonReader.hasValue(data, "highestBidder") ? data.getAsJsonObject("highestBidder") : null;
    hasCurrentWinner = userObj != null;
    if (hasCurrentWinner) {
      leaderText = resolveUserDisplayName(userObj);
    }
    if (lblCurrentPrice != null) {
      lblCurrentPrice.setText(AuctionRoomMoneyFormatter.formatVnd(currentDisplayedPrice));
    }
    if (lblLeader != null) {
      lblLeader.setText(hasCurrentWinner ? "Người dẫn đầu: " + leaderText : leaderText);
    }
  }

  private void applyTimeState(JsonObject response, JsonObject data) {
    AuctionTimeUtil.AuctionState state = AuctionTimeUtil.calculateState(
        AuctionJsonReader.getString(data, "startTime", ""),
        AuctionJsonReader.getString(data, "endTime", ""),
        AuctionJsonReader.getString(response, "serverNow", null));
    totalSeconds = state.countdownSeconds;
    String storedStatus = AuctionJsonReader.getString(
        data, "status", AuctionJsonReader.getString(data, "storedStatus", null));
    currentStatus = storedStatus != null && !storedStatus.isBlank() ? storedStatus : state.finalStatus;
    if ("OPEN".equalsIgnoreCase(currentStatus) || "RUNNING".equalsIgnoreCase(currentStatus)) {
      applyActiveControls(data);
      startCountdown();
    } else {
      setExpiredUI();
    }
  }

  private void applyActiveControls(JsonObject data) {
    isAuctionStarted = "RUNNING".equalsIgnoreCase(currentStatus);
    if (btnPlaceBid != null) {
      btnPlaceBid.setDisable(!isAuctionStarted || autoBidActive);
      btnPlaceBid.setText(isAuctionStarted ? "ĐẶT GIÁ" : "CHỜ MỞ BÁN");
    }
    if (txtBidAmount != null) {
      txtBidAmount.setEditable(isAuctionStarted && !autoBidActive);
    }
    if (btnEnableAutoBid != null) {
      btnEnableAutoBid.setDisable(!isAuctionStarted);
    }
    applyOwnerAndAutoBidControls(data);
  }

  private void applyOwnerAndAutoBidControls(JsonObject data) {
    if (currentUserOwnsAuction) {
      if (txtBidAmount != null) {
        txtBidAmount.setEditable(false);
      }
      if (txtMaxAutoBid != null) {
        txtMaxAutoBid.setEditable(false);
      }
      if (txtAutoBidStep != null) {
        txtAutoBidStep.setEditable(false);
      }
      if (!ownerBidWarningShown) {
        ownerBidWarningShown = true;
        showAlert("Không thể đặt giá", SELLER_SELF_BID_MESSAGE);
      }
    } else if (txtMaxAutoBid != null) {
      txtMaxAutoBid.setEditable(isAuctionStarted);
      if (txtAutoBidStep != null) {
        txtAutoBidStep.setEditable(isAuctionStarted);
      }
    }
    applySavedAutoBid(data);
  }

  private void applySavedAutoBid(JsonObject data) {
    if (AuctionJsonReader.hasValue(data, "userMaxAutoBid")) {
      autoBidActive = true;
      if (txtMaxAutoBid != null) {
        txtMaxAutoBid.setText(String.valueOf(data.get("userMaxAutoBid").getAsLong()));
      }
      if (txtAutoBidStep != null && AuctionJsonReader.hasValue(data, "userAutoBidStep")) {
        txtAutoBidStep.setText(String.valueOf(data.get("userAutoBidStep").getAsLong()));
      }
      if (btnEnableAutoBid != null) {
        btnEnableAutoBid.setText(AUTO_BID_REMOVE_TEXT);
      }
    } else {
      autoBidActive = false;
      if (txtMaxAutoBid != null) {
        txtMaxAutoBid.clear();
      }
      if (txtAutoBidStep != null) {
        txtAutoBidStep.clear();
      }
      if (btnEnableAutoBid != null) {
        btnEnableAutoBid.setText(AUTO_BID_REGISTER_TEXT);
      }
    }
    if (!currentUserOwnsAuction && isAuctionStarted) {
      if (btnPlaceBid != null) {
        btnPlaceBid.setDisable(autoBidActive);
      }
      if (txtBidAmount != null) {
        txtBidAmount.setEditable(!autoBidActive);
      }
    }
  }
}
