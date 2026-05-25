package com.auction.client.manager;

import com.auction.client.networkclient.ClientSocket;
import com.auction.common.dto.AdminDTOs;
import com.auction.common.dto.AdminDTOs.AuctionSummaryDTO;
import com.auction.common.dto.AdminDTOs.PendingAuctionDTO;
import com.auction.common.dto.AdminDTOs.UserSummaryDTO;
import com.auction.common.enums.ActionType;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminManager {

  private static final Logger logger = LoggerFactory.getLogger(AdminManager.class);
  private static AdminManager instance;

  private AdminManager() {
  }

  public static AdminManager getInstance() {
    if (instance == null) {
      instance = new AdminManager();
    }
    return instance;
  }

  /**
   * Lay danh sach Bidder tu server.
   */
  public void getBidders(Consumer<List<UserSummaryDTO>> onSuccess, Consumer<String> onError) {
    JsonObject request = buildRequest(ActionType.ADMIN_GET_ALL_BIDDERS);
    ClientSocket.getInstance().sendJsonRequest(request, "GET_ALL_BIDDERS_RESPONSE",
        response -> handleUserListResponse(response, onSuccess, onError));
  }

  /**
   * Lay danh sach Seller tu server.
   */
  public void getSellers(Consumer<List<UserSummaryDTO>> onSuccess, Consumer<String> onError) {
    JsonObject request = buildRequest(ActionType.ADMIN_GET_ALL_SELLERS);
    ClientSocket.getInstance().sendJsonRequest(request, "GET_ALL_SELLERS_RESPONSE",
        response -> handleUserListResponse(response, onSuccess, onError));
  }

  public void getAuctions(Consumer<List<AuctionSummaryDTO>> onSuccess, Consumer<String> onError) {
    JsonObject request = buildRequest(ActionType.ADMIN_GET_ALL_AUCTIONS);
    ClientSocket.getInstance().sendJsonRequest(request, "GET_ALL_AUCTIONS_RESPONSE",
        response -> handleAuctionListResponse(response, onSuccess, onError));
  }

  public void toggleAccountLock(
      String username,
      String newStatus,
      Consumer<JsonObject> onSuccess,
      Consumer<String> onError) {
    JsonObject request = buildRequest(ActionType.ADMIN_TOGGLE_LOCK_USER);
    request.addProperty("username", username);
    request.addProperty("newStatus", newStatus);

    ClientSocket.getInstance().sendJsonRequest(request, "TOGGLE_LOCK_USER_RESPONSE", response -> {
      boolean ok = response.has("success") && response.get("success").getAsBoolean();
      if (ok) {
        onSuccess.accept(response);
      } else {
        String msg = response.has("message")
            ? response.get("message").getAsString()
            : "Loi khong xac dinh";
        onError.accept(msg);
      }
    });
  }

  public void getBidderCount(Consumer<Integer> onSuccess, Consumer<String> onError) {
    getBidders(list -> onSuccess.accept(list.size()), onError);
  }

  public void getSellerCount(Consumer<Integer> onSuccess, Consumer<String> onError) {
    getSellers(list -> onSuccess.accept(list.size()), onError);
  }

  public void getAuctionCount(Consumer<Integer> onSuccess, Consumer<String> onError) {
    getAuctions(list -> onSuccess.accept(list.size()), onError);
  }

  private void handleAuctionListResponse(
      JsonObject response,
      Consumer<List<AuctionSummaryDTO>> onSuccess,
      Consumer<String> onError) {
    boolean ok = response.has("success") && response.get("success").getAsBoolean();
    if (!ok) {
      String msg = response.has("message")
          ? response.get("message").getAsString()
          : "Loi khong xac dinh";
      onError.accept(msg);
      return;
    }

    List<AuctionSummaryDTO> list = new ArrayList<>();
    JsonArray data = response.getAsJsonArray("data");
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
    onSuccess.accept(list);
  }

  public void getPendingAuctions(
      Consumer<List<PendingAuctionDTO>> onSuccess,
      Consumer<String> onError) {
    JsonObject request = buildRequest(ActionType.ADMIN_GET_PENDING_AUCTIONS);
    ClientSocket.getInstance().sendJsonRequest(request, "ADMIN_GET_PENDING_AUCTIONS_RESPONSE", response -> {
      boolean ok = response.has("success") && response.get("success").getAsBoolean();
      if (!ok) {
        String msg = response.has("message")
            ? response.get("message").getAsString()
            : "Loi khong xac dinh";
        onError.accept(msg);
        return;
      }
      try {
        List<PendingAuctionDTO> list = new ArrayList<>();
        JsonArray data = response.getAsJsonArray("data");
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
          onSuccess.accept(list);
          logger.info("Da lay danh sach phien dau gia cho duyet tu server.");
        }
      } catch (Exception e) {
        onError.accept("Du lieu phan hoi khong hop le.");
        logger.error("Loi xu ly response layDanhSachChoDuyet.", e);
      }
    });
  }

  public void approveAuction(int auctionId, Consumer<String> onSuccess, Consumer<String> onError) {
    JsonObject request = buildRequest(ActionType.ADMIN_APPROVE_AUCTION);
    request.addProperty("auctionId", auctionId);
    ClientSocket.getInstance().sendJsonRequest(request, "ADMIN_APPROVE_AUCTION_RESPONSE", response -> {
      boolean ok = response.has("success") && response.get("success").getAsBoolean();
      String msg = response.has("message") ? response.get("message").getAsString() : "";
      if (ok) {
        onSuccess.accept(msg);
      } else {
        onError.accept(msg);
      }
    });
  }

  public void rejectAuction(int auctionId, Consumer<String> onSuccess, Consumer<String> onError) {
    JsonObject request = buildRequest(ActionType.ADMIN_REJECT_AUCTION);
    request.addProperty("auctionId", auctionId);
    ClientSocket.getInstance().sendJsonRequest(request, "ADMIN_REJECT_AUCTION_RESPONSE", response -> {
      boolean ok = response.has("success") && response.get("success").getAsBoolean();
      String msg = response.has("message") ? response.get("message").getAsString() : "";
      if (ok) {
        onSuccess.accept(msg);
      } else {
        onError.accept(msg);
      }
    });
  }

  public void getInvoices(
      Consumer<List<AdminDTOs.InvoiceDTO>> onSuccess,
      Consumer<String> onError,
      Consumer<Long> onTongDoanhThu) {
    JsonObject request = buildRequest(ActionType.ADMIN_GET_INVOICES);
    ClientSocket.getInstance().sendJsonRequest(request, "ADMIN_GET_INVOICES_RESPONSE", response -> {
      boolean ok = response.has("success") && response.get("success").getAsBoolean();
      if (!ok) {
        onError.accept(response.has("message")
            ? response.get("message").getAsString()
            : "Loi khong xac dinh");
        return;
      }

      long tongDoanhThu = response.has("tongDoanhThu")
          ? response.get("tongDoanhThu").getAsLong()
          : 0L;
      onTongDoanhThu.accept(tongDoanhThu);

      List<AdminDTOs.InvoiceDTO> list = new ArrayList<>();
      JsonArray data = response.getAsJsonArray("data");
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
      onSuccess.accept(list);
    });
  }

  private JsonObject buildRequest(String type) {
    JsonObject request = new JsonObject();
    request.addProperty("type", type);
    request.addProperty("requestId", UUID.randomUUID().toString());
    return request;
  }

  private void handleUserListResponse(
      JsonObject response,
      Consumer<List<UserSummaryDTO>> onSuccess,
      Consumer<String> onError) {
    boolean ok = response.has("success") && response.get("success").getAsBoolean();
    if (!ok) {
      String msg = response.has("message")
          ? response.get("message").getAsString()
          : "Loi khong xac dinh";
      onError.accept(msg);
      return;
    }

    List<UserSummaryDTO> list = new ArrayList<>();
    JsonArray data = response.getAsJsonArray("data");
    if (data != null) {
      data.forEach(el -> {
        JsonObject obj = el.getAsJsonObject();
        list.add(new UserSummaryDTO(
            obj.get("username").getAsString(),
            obj.get("fullname").getAsString(),
            obj.has("status") ? obj.get("status").getAsString() : "ACTIVE"));
      });
    }
    onSuccess.accept(list);
  }

  private String safeGetString(JsonObject obj, String key, String defaultValue) {
    return (obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsString() : defaultValue;
  }

  private int safeGetInt(JsonObject obj, String key, int defaultValue) {
    return (obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsInt() : defaultValue;
  }

  private long safeGetLong(JsonObject obj, String key, long defaultValue) {
    return (obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsLong() : defaultValue;
  }

  public void changeAuctionStatus(int auctionId, String newStatus,
      Consumer<String> onSuccess, Consumer<String> onError) {
    JsonObject request = buildRequest(ActionType.ADMIN_CHANGE_AUCTION_STATUS);
    request.addProperty("auctionId", auctionId);
    request.addProperty("newStatus", newStatus);
    ClientSocket.getInstance().sendJsonRequest(request,
        "ADMIN_CHANGE_AUCTION_STATUS_RESPONSE", response -> {
          boolean ok = response.has("success") && response.get("success").getAsBoolean();
          String msg = response.has("message") ? response.get("message").getAsString() : "";
          if (ok)
            onSuccess.accept(msg);
          else
            onError.accept(msg);
        });
  }
}
