package com.auction.client.controller.bidder;

import com.auction.client.networkclient.ClientSocket;
import com.auction.common.enums.ActionType;
import com.google.gson.JsonObject;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.Initializable;
import javafx.scene.chart.XYChart;

public class AuctionRoomController extends AuctionRoomCommandSupport implements Initializable {
  @Override
  public void initialize(URL location, ResourceBundle resources) {
    com.auction.client.networkclient.PushHandler.currentRoomController = this;

    productImageHelper = new AuctionRoomImageHelper(imgProduct, imageFrame, IMAGE_FRAME_HEIGHT);
    productImageHelper.install();
    imageLoader = new AuctionRoomImageLoader(imgProduct);
    AuctionRoomMoneyFormatter.install(txtBidAmount);
    AuctionRoomMoneyFormatter.install(txtMaxAutoBid);
    AuctionRoomMoneyFormatter.install(txtAutoBidStep);

    priceSeries = new XYChart.Series<>();
    priceSeries.setName("Giá đấu");
    if (priceChart != null) {
      priceChart.getData().add(priceSeries);
    }
  }

  public void initData(int auctionId, String imageUrl) {
    initData(auctionId, imageUrl, null);
  }

  public void initData(int auctionId, String imageUrl, JsonObject auctionSnapshot) {
    currentAuctionId = auctionId;
    if (auctionSnapshot != null) {
      applyAuctionSnapshot(auctionSnapshot, imageUrl);
    } else {
      loadProductImage(imageUrl);
    }

    JsonObject joinReq = new JsonObject();
    joinReq.addProperty("type", ActionType.JOIN_AUCTION);
    joinReq.addProperty("auctionId", auctionId);
    joinReq.addProperty("requestId", java.util.UUID.randomUUID().toString());
    ClientSocket.getInstance().sendJsonRequest(joinReq, null, null);

    refreshAuctionState();
    loadBidHistoryFromServer();
  }
}
