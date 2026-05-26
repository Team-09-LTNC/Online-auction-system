package com.auction.client.controller.components;

import com.google.gson.JsonObject;

final class ProductCardSnapshotFactory {
  private ProductCardSnapshotFactory() {
  }

  static JsonObject create(
      int auctionId,
      String name,
      double price,
      long countdownSeconds,
      String status,
      String imageUrl,
      String imageThumbUrl
  ) {
    JsonObject snapshot = new JsonObject();
    snapshot.addProperty("auctionId", auctionId);
    snapshot.addProperty("itemName", name);
    snapshot.addProperty("currentPrice", (long) price);
    snapshot.addProperty("currentHighestBid", (long) price);
    snapshot.addProperty("countdownSeconds", countdownSeconds);
    snapshot.addProperty("status", status);
    snapshot.addProperty("imageUrl", imageUrl);
    if (imageThumbUrl != null && !imageThumbUrl.isBlank()) {
      snapshot.addProperty("imageThumbUrl", imageThumbUrl);
    }
    return snapshot;
  }
}
