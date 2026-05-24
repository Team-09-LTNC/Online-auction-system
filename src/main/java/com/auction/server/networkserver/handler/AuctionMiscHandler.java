package com.auction.server.networkserver.handler;

import com.auction.common.dto.BaseDTOs;
import com.auction.common.enums.ErrorCode;
import com.auction.common.enums.StatusCode;
import com.auction.common.model.user.User;
import com.auction.server.dao.FollowDao;
import com.auction.server.dao.SystemNotificationDao;
import com.auction.server.manager.AuctionManager;
import com.auction.server.networkserver.ClientHandler;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

final class AuctionMiscHandler {
    private AuctionMiscHandler() {
    }

    static String xuLyTheoDoi(Gson gson, JsonObject yeuCau, ClientHandler client, boolean isFollow) {
        User nguoiDung = client.layNguoiDungHienTai();
        if (nguoiDung == null) {
            JsonObject loi = gson.toJsonTree(new BaseDTOs.ErrorResponse(
                    StatusCode.UNAUTHORIZED, "Chua dang nhap!", ErrorCode.UNAUTHORIZED
            )).getAsJsonObject();
            AuctionControllerUtil.copyRequestId(yeuCau, loi);
            return gson.toJson(loi);
        }

        int auctionId = yeuCau.has("auctionId") ? yeuCau.get("auctionId").getAsInt() : -1;
        if (auctionId == -1) {
            JsonObject loi = gson.toJsonTree(new BaseDTOs.ErrorResponse(
                    StatusCode.BAD_REQUEST, "Thieu auctionId.", ErrorCode.BAD_REQUEST
            )).getAsJsonObject();
            AuctionControllerUtil.copyRequestId(yeuCau, loi);
            return gson.toJson(loi);
        }

        FollowDao followDao = new FollowDao();
        if (isFollow) {
            followDao.follow(nguoiDung.getId(), auctionId);
        } else {
            followDao.unfollow(nguoiDung.getId(), auctionId);
        }

        boolean actualFollowed = followDao.isFollowing(nguoiDung.getId(), auctionId);
        JsonObject phanHoi = new JsonObject();
        phanHoi.addProperty("type", isFollow ? "FOLLOW_RESPONSE" : "UNFOLLOW_RESPONSE");
        phanHoi.addProperty("success", actualFollowed == isFollow);
        phanHoi.addProperty("isFollowed", actualFollowed);
        phanHoi.addProperty("message", actualFollowed == isFollow
                ? (isFollow ? "Da theo doi phien dau gia" : "Da huy theo doi")
                : "Khong cap nhat duoc trang thai theo doi.");
        AuctionControllerUtil.copyRequestId(yeuCau, phanHoi);
        return gson.toJson(phanHoi);
    }

    static String xuLyLayThongBaoHeThong(Gson gson, JsonObject yeuCau, ClientHandler client) {
        User nguoiDung = client.layNguoiDungHienTai();
        JsonObject phanHoi = new JsonObject();
        phanHoi.addProperty("type", "SYSTEM_NOTIFICATIONS_RESPONSE");
        AuctionControllerUtil.copyRequestId(yeuCau, phanHoi);
        if (nguoiDung == null) {
            phanHoi.addProperty("success", false);
            phanHoi.addProperty("message", "Chua dang nhap.");
            return gson.toJson(phanHoi);
        }
        SystemNotificationDao dao = new SystemNotificationDao();
        phanHoi.addProperty("success", true);
        phanHoi.add("data", dao.layThongBaoCuaNguoiNhan(nguoiDung.getId()));
        phanHoi.addProperty("unreadCount", dao.demThongBaoChuaDoc(nguoiDung.getId()));
        return gson.toJson(phanHoi);
    }

    static String xuLyDanhDauThongBaoDaDoc(Gson gson, JsonObject yeuCau, ClientHandler client) {
        User nguoiDung = client.layNguoiDungHienTai();
        JsonObject phanHoi = new JsonObject();
        phanHoi.addProperty("type", "MARK_NOTIFICATIONS_READ_RESPONSE");
        AuctionControllerUtil.copyRequestId(yeuCau, phanHoi);
        if (nguoiDung == null) {
            phanHoi.addProperty("success", false);
            phanHoi.addProperty("message", "Chua dang nhap.");
            return gson.toJson(phanHoi);
        }
        boolean ok = new SystemNotificationDao().danhDauDaDoc(nguoiDung.getId());
        phanHoi.addProperty("success", ok);
        return gson.toJson(phanHoi);
    }

    static String xuLyChat(Gson gson, JsonObject yeuCau, ClientHandler client) {
        User nguoiDung = client.layNguoiDungHienTai();
        if (nguoiDung == null) {
            return gson.toJson(new BaseDTOs.ErrorResponse(
                    StatusCode.UNAUTHORIZED, "Chua dang nhap!", ErrorCode.UNAUTHORIZED
            ));
        }
        String message = yeuCau.has("message") ? yeuCau.get("message").getAsString() : "";
        int auctionId = yeuCau.has("auctionId") ? yeuCau.get("auctionId").getAsInt() : -1;
        if (message.isEmpty() || auctionId == -1) {
            return gson.toJson(new BaseDTOs.ErrorResponse(
                    StatusCode.BAD_REQUEST, "Thieu noi dung chat hoac auctionId", ErrorCode.BAD_REQUEST
            ));
        }
        AuctionManager.getInstance().broadcastChatMessage(auctionId, nguoiDung.getFullName(), message, false);
        JsonObject phanHoi = new JsonObject();
        phanHoi.addProperty("type", "CHAT_SEND_RESPONSE");
        phanHoi.addProperty("success", true);
        phanHoi.addProperty("message", "Da gui tin nhan");
        return gson.toJson(phanHoi);
    }
}
