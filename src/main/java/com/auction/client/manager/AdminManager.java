package com.auction.client.manager;

import com.auction.client.networkclient.ClientSocket;
import com.auction.common.enums.ActionType;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

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

    public static class UserInfo {
        private final String username;
        private final String fullname;
        private String status;

        public UserInfo(String username, String fullname, String status) {
            this.username = username;
            this.fullname = fullname;
            this.status = status;
        }

        public String getUsername() {
            return username;
        }

        public String getFullname() {
            return fullname;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    // ── API cho Controller gọi ────────────────────────────────

    /**
     * Lấy danh sách Bidder từ server.
     * Controller chỉ cần truyền callback nhận List<UserInfo>.
     */
    public void layDanhSachBidder(Consumer<List<UserInfo>> onSuccess, Consumer<String> onError) {
        JsonObject request = buildRequest(ActionType.ADMIN_GET_ALL_BIDDERS);

        ClientSocket.getInstance().sendJsonRequest(request, "GET_ALL_BIDDERS_RESPONSE", response -> {
            xuLyDanhSachResponse(response, "GET_ALL_BIDDERS_RESPONSE", onSuccess, onError);
        });
    }

    /**
     * Lấy danh sách Seller từ server.
     */
    public void layDanhSachSeller(Consumer<List<UserInfo>> onSuccess, Consumer<String> onError) {
        JsonObject request = buildRequest(ActionType.ADMIN_GET_ALL_SELLERS);

        ClientSocket.getInstance().sendJsonRequest(request, "GET_ALL_SELLERS_RESPONSE", response -> {
            xuLyDanhSachResponse(response, "GET_ALL_SELLERS_RESPONSE", onSuccess, onError);
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
     * Lấy tổng số người đấu giá, người bán, phiên đấu giá để hiển thị trên Dashboard.
     * Controller chỉ cần gọi API này và nhận về số lượng, không cần quan tâm cách thức lấy dữ liệu.
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

    // ── Helpers ───────────────────────────────────────────────

    private JsonObject buildRequest(String type) {
        JsonObject request = new JsonObject();
        request.addProperty("type", type);
        request.addProperty("requestId", UUID.randomUUID().toString());
        return request;
    }

    private void xuLyDanhSachResponse(JsonObject response, String expectedType,
            Consumer<List<UserInfo>> onSuccess, Consumer<String> onError) {
        boolean ok = response.has("success") && response.get("success").getAsBoolean();
        if (!ok) {
            String msg = response.has("message") ? response.get("message").getAsString() : "Lỗi không xác định";
            onError.accept(msg);
            return;
        }

        List<UserInfo> list = new ArrayList<>();
        JsonArray data = response.getAsJsonArray("data");
        if (data != null) {
            data.forEach(el -> {
                JsonObject obj = el.getAsJsonObject();
                list.add(new UserInfo(
                        obj.get("username").getAsString(),
                        obj.get("fullname").getAsString(),
                        obj.has("status") ? obj.get("status").getAsString() : "ACTIVE"));
            });
        }
        onSuccess.accept(list);
    }
}