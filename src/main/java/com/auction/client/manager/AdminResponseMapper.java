package com.auction.client.manager;

import com.auction.common.dto.AdminDTOs;
import com.auction.common.dto.AdminDTOs.AuctionSummaryDTO;
import com.auction.common.dto.AdminDTOs.PendingAuctionDTO;
import com.auction.common.dto.AdminDTOs.UserSummaryDTO;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

final class AdminResponseMapper {
  private AdminResponseMapper() {
  }

  static List<UserSummaryDTO> toUsers(JsonArray data) {
    List<UserSummaryDTO> list = new ArrayList<>();
    if (data != null) {
      data.forEach(el -> {
        JsonObject obj = el.getAsJsonObject();
        list.add(new UserSummaryDTO(
            obj.get("username").getAsString(),
            obj.get("fullname").getAsString(),
            safeGetString(obj, "status", "ACTIVE"),
            safeGetString(obj, "lockUntil", null)));
      });
    }
    return list;
  }

  static List<AuctionSummaryDTO> toAuctions(JsonArray data) {
    List<AuctionSummaryDTO> list = new ArrayList<>();
    if (data != null) {
      data.forEach(el -> {
        JsonObject obj = el.getAsJsonObject();
        list.add(new AuctionSummaryDTO(
            obj.get("auctionId").getAsInt(),
            obj.get("itemname").getAsString(),
            obj.get("starttime").getAsString(),
            obj.get("endtime").getAsString(),
            obj.has("status") ? obj.get("status").getAsString() : "OPEN",
            obj.has("imageurl") ? obj.get("imageurl").getAsString() : null));
      });
    }
    return list;
  }

  static List<PendingAuctionDTO> toPendingAuctions(JsonArray data) {
    List<PendingAuctionDTO> list = new ArrayList<>();
    if (data != null) {
      data.forEach(el -> {
        JsonObject obj = el.getAsJsonObject();
        list.add(new PendingAuctionDTO(
            safeGetInt(obj, "id", -1),
            safeGetString(obj, "itemName", ""),
            safeGetInt(obj, "sellerId", -1),
            safeGetString(obj, "description", ""),
            safeGetLong(obj, "startingPrice", 0L),
            safeGetString(obj, "category", ""),
            safeGetString(obj, "startTime", ""),
            safeGetString(obj, "endTime", ""),
            safeGetString(obj, "imageUrl", null)));
      });
    }
    return list;
  }

  static List<AdminDTOs.TransactionDTO> toTransactions(JsonArray data) {
    List<AdminDTOs.TransactionDTO> list = new ArrayList<>();
    if (data != null) {
      data.forEach(el -> {
        JsonObject obj = el.getAsJsonObject();
        list.add(new AdminDTOs.TransactionDTO(
            safeGetInt(obj, "auctionId", -1),
            safeGetInt(obj, "itemId", -1),
            safeGetString(obj, "itemName", ""),
            safeGetString(obj, "startTime", ""),
            safeGetString(obj, "endTime", ""),
            safeGetString(obj, "status", ""),
            safeGetInt(obj, "winnerId", 0),
            safeGetLong(obj, "finalPrice", 0L)));
      });
    }
    return list;
  }

  static List<AdminDTOs.InvoiceDTO> toInvoices(JsonArray data) {
    List<AdminDTOs.InvoiceDTO> list = new ArrayList<>();
    if (data != null) {
      data.forEach(el -> {
        JsonObject obj = el.getAsJsonObject();
        list.add(new AdminDTOs.InvoiceDTO(
            obj.get("auctionId").getAsInt(),
            obj.get("itemId").getAsInt(),
            obj.get("itemName").getAsString(),
            obj.get("sellerId").getAsInt(),
            obj.get("winnerId").getAsInt(),
            obj.get("highestBid").getAsLong()));
      });
    }
    return list;
  }

  static String safeGetString(JsonObject obj, String key, String defaultValue) {
    return (obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsString() : defaultValue;
  }

  private static int safeGetInt(JsonObject obj, String key, int defaultValue) {
    return (obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsInt() : defaultValue;
  }

  private static long safeGetLong(JsonObject obj, String key, long defaultValue) {
    return (obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsLong() : defaultValue;
  }
}
