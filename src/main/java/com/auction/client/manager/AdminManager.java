package com.auction.client.manager;

import com.auction.client.networkclient.ClientSocket;
import com.auction.common.enums.ActionType;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.auction.common.dto.AdminDTOs.AuctionSummaryDTO;
import com.auction.common.dto.AdminDTOs.UserSummaryDTO;
import com.auction.common.dto.AdminDTOs.PendingAuctionDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class AdminManager {

    private static AdminManager instance;

    private AdminManager() {
    }

    public static AdminManager getInstance() {
        if (instance == null)
            instance = new AdminManager();
        return instance;
    }

    // ── Model nội bộ ──────────────────────────────────────────

    // public static class UserInfo {
    // private final String username;
    // private final String fullname;
    // private String status;

    // public UserInfo(String username, String fullname, String status) {
    // this.username = username;
    // this.fullname = fullname;
    // this.status = status;
    // }

    // public String getUsername() {
    // return username;
    // }

    // public String getFullname() {
    // return fullname;
    // }

    // public String getStatus() {
    // return status;
    // }

    // public void setStatus(String status) {
    // this.status = status;
    // }
    // }

    // public static class AuctionInfo {
    // private final String auctionId;
    // private final String startTime;
    // private final String endTime;
    // private final String status;

    // public AuctionInfo(String auctionId, String startTime, String endTime, String
    // status) {
    // this.auctionId = auctionId;
    // this.startTime = startTime;
    // this.endTime = endTime;
    // this.status = status;
    // }

    // public String getAuctionId() {
    // return auctionId;
    // }

    // public String getStartTime() {
    // return startTime;
    // }

    // public String getEndTime() {
    // return endTime;
    // }

    // public String getStatus() {
    // return status;
    // }
    // }

    // ── API cho Controller gọi ────────────────────────────────

    /**
     * Lấy danh sách Bidder từ server.
     * Controller chỉ cần truyền callback nhận List<UserInfo>.
     */
    public void layDanhSachBidder(Consumer<List<UserSummaryDTO>> onSuccess, Consumer<String> onError) {
        JsonObject request = buildRequest(ActionType.ADMIN_GET_ALL_BIDDERS);

        ClientSocket.getInstance().sendJsonRequest(request, "GET_ALL_BIDDERS_RESPONSE", response -> {
            xuLyDanhSachUserResponse(response, "GET_ALL_BIDDERS_RESPONSE", onSuccess, onError);
        });
    }

    /**
     * Lấy danh sách Seller từ server.
     */
    public void layDanhSachSeller(Consumer<List<UserSummaryDTO>> onSuccess, Consumer<String> onError) {
        JsonObject request = buildRequest(ActionType.ADMIN_GET_ALL_SELLERS);

        ClientSocket.getInstance().sendJsonRequest(request, "GET_ALL_SELLERS_RESPONSE", response -> {
            xuLyDanhSachUserResponse(response, "GET_ALL_SELLERS_RESPONSE", onSuccess, onError);
        });
    }

    public void layDanhSachAuction(Consumer<List<AuctionSummaryDTO>> onSuccess, Consumer<String> onError) {
        JsonObject request = buildRequest(ActionType.ADMIN_GET_ALL_AUCTIONS);

        ClientSocket.getInstance().sendJsonRequest(request, "GET_ALL_AUCTIONS_RESPONSE", response -> {
            xuLyDanhSachAuctionResponse(response, "GET_ALL_AUCTIONS_RESPONSE", onSuccess, onError);
        });
    }

    /**
     * Khoá hoặc mở khoá tài khoản.
     */
    public void toggleKhoaTaiKhoan(String username, String newStatus,
            Consumer<JsonObject> onSuccess, Consumer<String> onError) {
        JsonObject request = buildRequest(ActionType.ADMIN_TOGGLE_LOCK_USER);
        request.addProperty("username", username);
        request.addProperty("newStatus", newStatus);

        ClientSocket.getInstance().sendJsonRequest(request, "TOGGLE_LOCK_USER_RESPONSE", response -> {
            boolean ok = response.has("success") && response.get("success").getAsBoolean();
            if (ok) {
                onSuccess.accept(response);
            } else {
                String msg = response.has("message") ? response.get("message").getAsString() : "Lỗi không xác định";
                onError.accept(msg);
            }
        });
    }

    /**
     * Lấy tổng số người đấu giá, người bán, phiên đấu giá để hiển thị trên
     * Dashboard.
     * Controller chỉ cần gọi API này và nhận về số lượng, không cần quan tâm cách
     * thức lấy dữ liệu.
     */
    // AdminManager.java
    public void layTongSoBidder(Consumer<Integer> onSuccess, Consumer<String> onError) {
        layDanhSachBidder(
                list -> onSuccess.accept(list.size()),
                onError);
    }

    public void layTongSoSeller(Consumer<Integer> onSuccess, Consumer<String> onError) {
        layDanhSachSeller(
                list -> onSuccess.accept(list.size()),
                onError);
    }

    public void layTongSoAuction(Consumer<Integer> onSuccess, Consumer<String> onError) {
        layDanhSachAuction(
                list -> onSuccess.accept(list.size()),
                onError);
    }

    // ── Helpers ───────────────────────────────────────────────

    private JsonObject buildRequest(String type) {
        JsonObject request = new JsonObject();
        request.addProperty("type", type);
        request.addProperty("requestId", UUID.randomUUID().toString());
        return request;
    }

    private void xuLyDanhSachUserResponse(JsonObject response, String expectedType,
            Consumer<List<UserSummaryDTO>> onSuccess, Consumer<String> onError) {
        boolean ok = response.has("success") && response.get("success").getAsBoolean();
        if (!ok) {
            String msg = response.has("message") ? response.get("message").getAsString() : "Lỗi không xác định";
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

    private void xuLyDanhSachAuctionResponse(JsonObject response, String expectedType,
            Consumer<List<AuctionSummaryDTO>> onSuccess, Consumer<String> onError) {
        boolean ok = response.has("success") && response.get("success").getAsBoolean();
        if (!ok) {
            String msg = response.has("message") ? response.get("message").getAsString() : "Lỗi không xác định";
            onError.accept(msg);
            return;
        }

        List<AuctionSummaryDTO> list = new ArrayList<>();
        JsonArray data = response.getAsJsonArray("data");
        if (data != null) {
            data.forEach(el -> {
                JsonObject obj = el.getAsJsonObject();
                list.add(new AuctionSummaryDTO(
                        obj.get("itemname").getAsString(),
                        obj.get("starttime").getAsString(),
                        obj.get("endtime").getAsString(),
                        obj.has("status") ? obj.get("status").getAsString() : "OPEN",
                        obj.has("imageurl") ? obj.get("imageurl").getAsString() : null));
            });
        }
        onSuccess.accept(list);
    }

    public void layDanhSachChoDuyet(Consumer<List<PendingAuctionDTO>> onSuccess, Consumer<String> onError) {
        JsonObject request = buildRequest(ActionType.ADMIN_GET_PENDING_AUCTIONS);
        ClientSocket.getInstance().sendJsonRequest(request, "ADMIN_GET_PENDING_AUCTIONS_RESPONSE", response -> {
            boolean ok = response.has("success") && response.get("success").getAsBoolean();
            if (!ok) {
                onError.accept(response.get("message").getAsString());
                return;
            }

            List<PendingAuctionDTO> list = new ArrayList<>();
            response.getAsJsonArray("data").forEach(el -> {
                JsonObject obj = el.getAsJsonObject();
                list.add(new PendingAuctionDTO(
                        obj.get("id").getAsInt(),
                        obj.get("itemName").getAsString(),
                        obj.get("sellerId").getAsInt(),
                        obj.get("description").getAsString(),
                        obj.get("startingPrice").getAsLong(),
                        obj.get("category").getAsString(),
                        obj.get("startTime").getAsString(),
                        obj.get("endTime").getAsString(),
                        obj.has("imageUrl") ? obj.get("imageUrl").getAsString() : null));
            });
            onSuccess.accept(list);
        });
    }

    public void duyetAuction(int auctionId, Consumer<String> onSuccess, Consumer<String> onError) {
        JsonObject request = buildRequest(ActionType.ADMIN_APPROVE_AUCTION);
        request.addProperty("auctionId", auctionId);
        ClientSocket.getInstance().sendJsonRequest(request, "ADMIN_APPROVE_AUCTION_RESPONSE", response -> {
            boolean ok = response.has("success") && response.get("success").getAsBoolean();
            String msg = response.has("message") ? response.get("message").getAsString() : "";
            if (ok)
                onSuccess.accept(msg);
            else
                onError.accept(msg);
        });
    }

    public void tuChoiAuction(int auctionId, Consumer<String> onSuccess, Consumer<String> onError) {
        JsonObject request = buildRequest(ActionType.ADMIN_REJECT_AUCTION);
        request.addProperty("auctionId", auctionId);
        ClientSocket.getInstance().sendJsonRequest(request, "ADMIN_REJECT_AUCTION_RESPONSE", response -> {
            boolean ok = response.has("success") && response.get("success").getAsBoolean();
            String msg = response.has("message") ? response.get("message").getAsString() : "";
            if (ok)
                onSuccess.accept(msg);
            else
                onError.accept(msg);
        });
    }

    // Model
    // public static class AuctionInfo {
    //     private final int id;
    //     private final String itemName, startTime, endTime, status;

    //     public AuctionInfo(int id, String itemName, String startTime, String endTime, String status) {
    //         this.id = id;
    //         this.itemName = itemName;
    //         this.startTime = startTime;
    //         this.endTime = endTime;
    //         this.status = status;
    //     }

    //     public int getId() {
    //         return id;
    //     }

    //     public String getItemName() {
    //         return itemName;
    //     }

    //     public String getStartTime() {
    //         return startTime;
    //     }

    //     public String getEndTime() {
    //         return endTime;
    //     }

    //     public String getStatus() {
    //         return status;
    //     }
    // }
}