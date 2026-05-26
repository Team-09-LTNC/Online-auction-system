package com.auction.server.networkserver.handler;

import com.auction.common.dto.AuctionDTOs;
import com.auction.common.dto.BaseDTOs;
import com.auction.common.enums.ActionType;
import com.auction.common.enums.AuctionStatus;
import com.auction.common.enums.ErrorCode;
import com.auction.common.enums.StatusCode;
import com.auction.common.model.bid.Auction;
import com.auction.common.model.bid.BidTransaction;
import com.auction.common.model.user.Bidder;
import com.auction.common.model.user.User;
import com.auction.server.dao.AuctionDao;
import com.auction.server.dao.BidderMoneySellerDao;
import com.auction.server.manager.AuctionManager;
import com.auction.server.networkserver.ClientHandler;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.time.LocalDateTime;

final class AuctionCommandHandler {
    private static final String SELLER_SELF_BID_MESSAGE = "Khong duoc tu bid san pham cua chinh minh.";
    private static final String LOCKED_BIDDER_MESSAGE =
            "Tai khoan cua ban dang bi khoa, khong the tham gia dau gia.";

    private final Gson gson;
    private final AuctionDao auctionDao;
    private final AuctionAccountGuard accountGuard;

    AuctionCommandHandler(Gson gson, AuctionDao auctionDao) {
        this.gson = gson;
        this.auctionDao = auctionDao;
        this.accountGuard = new AuctionAccountGuard(auctionDao);
    }

    String handleJoinAuction(JsonObject yeuCau, ClientHandler client) {
        int maPhien = AuctionControllerUtil.getAuctionIdFromRequest(yeuCau);
        if (maPhien <= 0) {
            return AuctionControllerUtil.buildError(
                    gson, yeuCau, StatusCode.BAD_REQUEST, "Thieu auctionId hop le.", ErrorCode.BAD_REQUEST
            );
        }
        AuctionManager.getInstance().subscribe(maPhien, client);
        JsonObject phanHoi = new JsonObject();
        phanHoi.addProperty("type", "JOIN_AUCTION_RESPONSE");
        phanHoi.addProperty("success", true);
        AuctionControllerUtil.copyRequestId(yeuCau, phanHoi);
        return gson.toJson(phanHoi);
    }

    String handlePlaceBid(JsonObject yeuCau, ClientHandler client) {
        AuctionDTOs.BidRequest request = gson.fromJson(yeuCau, AuctionDTOs.BidRequest.class);
        User nguoiDung = client.getCurrentUser();
        String requestId = getRequestId(yeuCau);

        if (nguoiDung == null || !"BIDDER".equals(nguoiDung.getRoleName())) {
            if (nguoiDung != null && "SELLER".equalsIgnoreCase(nguoiDung.getRoleName())
                    && accountGuard.isAuctionSeller(request.getAuctionId(), nguoiDung.getId())) {
                return simpleResponse("BID_RESPONSE", false, SELLER_SELF_BID_MESSAGE, requestId);
            }
            JsonObject errorRes = gson.toJsonTree(new BaseDTOs.ErrorResponse(
                    StatusCode.FORBIDDEN, "Chi nguoi mua (Bidder) moi duoc dat gia.", ErrorCode.UNAUTHORIZED
            )).getAsJsonObject();
            errorRes.addProperty("type", "BID_RESPONSE");
            addRequestId(errorRes, requestId);
            return gson.toJson(errorRes);
        }
        if (accountGuard.isAccountLocked(nguoiDung)) {
            JsonObject errorRes = gson.toJsonTree(new BaseDTOs.ErrorResponse(
                    StatusCode.FORBIDDEN, LOCKED_BIDDER_MESSAGE, ErrorCode.FORBIDDEN
            )).getAsJsonObject();
            errorRes.addProperty("type", "BID_RESPONSE");
            addRequestId(errorRes, requestId);
            return gson.toJson(errorRes);
        }

        try {
            BidTransaction giaoDich = new BidTransaction(
                    request.getAuctionId(), (Bidder) nguoiDung, request.getBidAmount(), LocalDateTime.now()
            );
            if (AuctionManager.getInstance().handlePlaceBid(request.getAuctionId(), giaoDich)) {
                JsonObject phanHoi = new JsonObject();
                phanHoi.addProperty("type", "BID_RESPONSE");
                addRequestId(phanHoi, requestId);
                phanHoi.addProperty("statusCode", StatusCode.OK);
                phanHoi.addProperty("success", true);
                phanHoi.addProperty("message", "Dat gia thanh cong!");
                return gson.toJson(phanHoi);
            }
            JsonObject failRes = gson.toJsonTree(new BaseDTOs.ErrorResponse(
                    StatusCode.BAD_REQUEST, "Da co nguoi tra gia cao hon.", ErrorCode.CONCURRENT_CONFLICT
            )).getAsJsonObject();
            failRes.addProperty("type", "BID_RESPONSE");
            addRequestId(failRes, requestId);
            return gson.toJson(failRes);
        } catch (Exception e) {
            JsonObject errRes = gson.toJsonTree(new BaseDTOs.ErrorResponse(
                    StatusCode.SERVER_ERROR, e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR
            )).getAsJsonObject();
            errRes.addProperty("type", "BID_RESPONSE");
            addRequestId(errRes, requestId);
            return gson.toJson(errRes);
        }
    }

    String handleRegisterAutoBid(JsonObject yeuCau, ClientHandler client) {
        String requestId = getRequestId(yeuCau);
        User nguoiDung = client.getCurrentUser();
        int maPhien = yeuCau.has("auctionId") ? yeuCau.get("auctionId").getAsInt() : -1;
        long maxBid = yeuCau.has("maxBid") ? yeuCau.get("maxBid").getAsLong() : -1;
        long bidStep = yeuCau.has("bidStep") ? yeuCau.get("bidStep").getAsLong() : -1;
        if (nguoiDung == null || !"BIDDER".equals(nguoiDung.getRoleName())) {
            if (nguoiDung != null && "SELLER".equalsIgnoreCase(nguoiDung.getRoleName())
                    && accountGuard.isAuctionSeller(maPhien, nguoiDung.getId())) {
                return simpleResponse("AUTO_BID_RESPONSE", false, SELLER_SELF_BID_MESSAGE, requestId);
            }
            return simpleResponse(
                    "AUTO_BID_RESPONSE", false, "Chi nguoi mua (Bidder) moi duoc cai auto-bid.", requestId
            );
        }
        if (maPhien == -1 || maxBid <= 0 || bidStep <= 0) {
            return simpleResponse(
                    "AUTO_BID_RESPONSE", false, "Thieu thong tin maxBid, bidStep hoac auctionId", requestId
            );
        }
        if (accountGuard.isAccountLocked(nguoiDung)) {
            return simpleResponse("AUTO_BID_RESPONSE", false, LOCKED_BIDDER_MESSAGE, requestId);
        }
        try {
            AuctionManager.getInstance().registerAutoBid(maPhien, nguoiDung, maxBid, bidStep);
            JsonObject phanHoi = new JsonObject();
            phanHoi.addProperty("type", "AUTO_BID_RESPONSE");
            addRequestId(phanHoi, requestId);
            phanHoi.addProperty("success", true);
            phanHoi.addProperty("message", "Da cai dat Auto-Bid thanh cong!");
            return gson.toJson(phanHoi);
        } catch (Exception e) {
            return simpleResponse("AUTO_BID_RESPONSE", false, e.getMessage(), requestId);
        }
    }

    String handleRemoveAutoBid(JsonObject yeuCau, ClientHandler client) {
        String requestId = getRequestId(yeuCau);
        User nguoiDung = client.getCurrentUser();
        int maPhien = yeuCau.has("auctionId") ? yeuCau.get("auctionId").getAsInt() : -1;
        if (nguoiDung == null || !"BIDDER".equals(nguoiDung.getRoleName())) {
            return simpleResponse("AUTO_BID_RESPONSE", false, "Chi bidder moi duoc xoa auto-bid.", requestId);
        }
        if (maPhien <= 0) {
            return simpleResponse("AUTO_BID_RESPONSE", false, "Thieu auctionId.", requestId);
        }
        try {
            AuctionManager.getInstance().removeAutoBid(maPhien, nguoiDung);
            JsonObject phanHoi = new JsonObject();
            phanHoi.addProperty("type", "AUTO_BID_RESPONSE");
            addRequestId(phanHoi, requestId);
            phanHoi.addProperty("success", true);
            phanHoi.addProperty("message", "Da xoa Auto-Bid thanh cong.");
            return gson.toJson(phanHoi);
        } catch (Exception e) {
            return simpleResponse("AUTO_BID_RESPONSE", false, e.getMessage(), requestId);
        }
    }

    String handleBuyNow(JsonObject yeuCau, ClientHandler client) {
        User nguoiDung = client.getCurrentUser();
        JsonObject phanHoi = new JsonObject();
        phanHoi.addProperty("type", "BUY_NOW_RESPONSE");
        AuctionControllerUtil.copyRequestId(yeuCau, phanHoi);
        int auctionId = yeuCau.has("auctionId") ? yeuCau.get("auctionId").getAsInt() : -1;
        if (nguoiDung == null || !"BIDDER".equals(nguoiDung.getRoleName())) {
            if (nguoiDung != null && "SELLER".equalsIgnoreCase(nguoiDung.getRoleName())
                    && accountGuard.isAuctionSeller(auctionId, nguoiDung.getId())) {
                phanHoi.addProperty("success", false);
                phanHoi.addProperty("message", SELLER_SELF_BID_MESSAGE);
                return gson.toJson(phanHoi);
            }
            phanHoi.addProperty("success", false);
            phanHoi.addProperty("message", "Chi bidder moi duoc mua dut.");
            return gson.toJson(phanHoi);
        }
        if (auctionId == -1) {
            phanHoi.addProperty("success", false);
            phanHoi.addProperty("message", "Thieu auctionId.");
            return gson.toJson(phanHoi);
        }
        if (accountGuard.isAccountLocked(nguoiDung)) {
            phanHoi.addProperty("success", false);
            phanHoi.addProperty("message", LOCKED_BIDDER_MESSAGE);
            return gson.toJson(phanHoi);
        }
        try {
            BidTransaction giaoDich = AuctionManager.getInstance().handleBuyNow(auctionId, (Bidder) nguoiDung);
            Auction phienDaChot = auctionDao.getAuctionById(auctionId);
            String tenPhien = phienDaChot != null ? phienDaChot.getItem().getName() : "san pham";
            phanHoi.addProperty("success", true);
            phanHoi.addProperty("auctionId", auctionId);
            phanHoi.addProperty("buyNowPrice", giaoDich.getBidAmount());
            phanHoi.addProperty("message", "Da chot phien o gia mua dut.");
            AuctionNotificationService.sendBuyNowNotification(
                    auctionId,
                    nguoiDung.getId(),
                    phienDaChot != null ? phienDaChot.getItem().getSellerId() : -1,
                    tenPhien,
                    nguoiDung.getFullName()
            );
            return gson.toJson(phanHoi);
        } catch (Exception e) {
            phanHoi.addProperty("success", false);
            phanHoi.addProperty("message", e.getMessage());
            return gson.toJson(phanHoi);
        }
    }

    String handleSettleBuyNow(JsonObject yeuCau, ClientHandler client) {
        User nguoiDung = client.getCurrentUser();
        JsonObject phanHoi = new JsonObject();
        phanHoi.addProperty("type", "BUY_NOW_SETTLEMENT_RESPONSE");
        AuctionControllerUtil.copyRequestId(yeuCau, phanHoi);
        if (nguoiDung == null || !"BIDDER".equals(nguoiDung.getRoleName())) {
            phanHoi.addProperty("success", false);
            phanHoi.addProperty("message", "Chi bidder thang phien moi duoc quyet toan.");
            return gson.toJson(phanHoi);
        }
        int auctionId = yeuCau.has("auctionId") ? yeuCau.get("auctionId").getAsInt() : -1;
        String decision = yeuCau.has("decision") ? yeuCau.get("decision").getAsString() : "";
        if (auctionId == -1 || (!"CONFIRM".equalsIgnoreCase(decision) && !"CANCEL".equalsIgnoreCase(decision))) {
            phanHoi.addProperty("success", false);
            phanHoi.addProperty("message", "Thieu quyet dinh thanh toan hop le.");
            return gson.toJson(phanHoi);
        }
        boolean thanhToan = "CONFIRM".equalsIgnoreCase(decision);
        BidderMoneySellerDao.PaymentResult ketQua =
                new BidderMoneySellerDao().settleBuyNow(auctionId, nguoiDung.getId(), thanhToan);
        phanHoi.addProperty("success", ketQua.success);
        phanHoi.addProperty("message", ketQua.message);
        phanHoi.addProperty("amount", ketQua.amount);
        if (ketQua.auctionStatus != null) {
            phanHoi.addProperty("auctionStatus", ketQua.auctionStatus);
        }
        if (ketQua.success) {
            AuctionManager.getInstance().updateStatusAfterPayment(
                    auctionId, AuctionStatus.valueOf(ketQua.auctionStatus)
            );
            Auction phien = auctionDao.getAuctionById(auctionId);
            int sellerId = phien != null ? phien.getItem().getSellerId() : -1;
            String tenPhien = phien != null ? phien.getItem().getName() : ("phien #" + auctionId);
            AuctionNotificationService.sendPostSettlementNotification(
                    auctionId, nguoiDung.getId(), sellerId, tenPhien, ketQua.amount, thanhToan
            );
        }
        return gson.toJson(phanHoi);
    }

    String handleLeaveAuction(JsonObject yeuCau, ClientHandler client) {
        int idPhien = AuctionControllerUtil.getAuctionIdFromRequest(yeuCau);
        if (idPhien <= 0) {
            return AuctionControllerUtil.buildError(
                    gson, yeuCau, StatusCode.BAD_REQUEST, "Thieu auctionId hop le.", ErrorCode.BAD_REQUEST
            );
        }
        AuctionManager.getInstance().unsubscribe(idPhien, client);
        JsonObject phanHoi = new JsonObject();
        phanHoi.addProperty("type", ActionType.LEAVE_AUCTION);
        phanHoi.addProperty("statusCode", StatusCode.OK);
        phanHoi.addProperty("success", true);
        phanHoi.addProperty("message", "Da roi phong dau gia");
        return gson.toJson(phanHoi);
    }

    String handleCloseAuction(JsonObject yeuCau, ClientHandler client) {
        User nguoiDung = client.getCurrentUser();
        if (nguoiDung == null || !"ADMIN".equals(nguoiDung.getRoleName())) {
            return gson.toJson(new BaseDTOs.ErrorResponse(
                    StatusCode.FORBIDDEN, "Chi Quan tri vien (Admin) moi co quyen dong phien!", ErrorCode.FORBIDDEN
            ));
        }
        int idPhien = AuctionControllerUtil.getAuctionIdFromRequest(yeuCau);
        if (idPhien <= 0) {
            return AuctionControllerUtil.buildError(
                    gson, yeuCau, StatusCode.BAD_REQUEST, "Thieu auctionId hop le.", ErrorCode.BAD_REQUEST
            );
        }
        boolean thanhCong = AuctionManager.getInstance().forceCloseAuction(idPhien);
        if (thanhCong) {
            JsonObject phanHoi = new JsonObject();
            phanHoi.addProperty("type", ActionType.CLOSE_AUCTION);
            phanHoi.addProperty("statusCode", StatusCode.OK);
            phanHoi.addProperty("success", true);
            phanHoi.addProperty("message", "Da ep buoc dong phien dau gia!");
            return gson.toJson(phanHoi);
        }
        return gson.toJson(new BaseDTOs.ErrorResponse(
                StatusCode.NOT_FOUND, "Khong tim thay phien dang chay", ErrorCode.AUCTION_NOT_FOUND
        ));
    }

    private String getRequestId(JsonObject request) {
        return request.has("requestId") && !request.get("requestId").isJsonNull()
                ? request.get("requestId").getAsString()
                : null;
    }

    private String simpleResponse(String type, boolean success, String message, String requestId) {
        JsonObject response = AuctionControllerUtil.buildSimpleResponse(type, success, message);
        addRequestId(response, requestId);
        return gson.toJson(response);
    }

    private void addRequestId(JsonObject response, String requestId) {
        if (requestId != null) {
            response.addProperty("requestId", requestId);
        }
    }

}
