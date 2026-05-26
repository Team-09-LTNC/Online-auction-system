package com.auction.server.networkserver.handler;

import com.auction.common.model.bid.Auction;
import com.auction.common.model.item.Item;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;

final class ProductResponseMapper {
  private ProductResponseMapper() {
  }

  static JsonArray buildSellerProductList(List<Auction> auctions) {
    JsonArray data = new JsonArray();
    for (Auction auction : auctions) {
      data.add(toSellerProduct(auction));
    }
    return data;
  }

  static int getItemIdFromRequest(JsonObject request) {
    if (request == null || !request.has("itemId") || request.get("itemId").isJsonNull()) {
      return -1;
    }
    try {
      return request.get("itemId").getAsInt();
    } catch (RuntimeException e) {
      return -1;
    }
  }

  private static JsonObject toSellerProduct(Auction auction) {
    Item item = auction.getItem();
    JsonObject obj = new JsonObject();
    obj.addProperty("id", item.getId());
    obj.addProperty("itemId", item.getId());
    obj.addProperty("auctionId", auction.getId());
    obj.addProperty("name", item.getName());
    obj.addProperty("description", item.getDescription());
    obj.addProperty("startingPrice", item.getStartingPrice());
    obj.addProperty("currentPrice", auction.getCurrentHighestBid());
    obj.addProperty("status", auction.getStoredStatus().name());
    obj.addProperty("category", item.getCategory());
    obj.addProperty("imageUrl", item.getImageUrl());
    obj.addProperty("imageThumbUrl", item.getImageThumbUrl());
    obj.addProperty("startTime", auction.getStartTime() != null ? auction.getStartTime().toString() : null);
    obj.addProperty("endTime", auction.getEndTime() != null ? auction.getEndTime().toString() : null);
    return obj;
  }
}
