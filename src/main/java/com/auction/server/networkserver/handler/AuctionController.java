package com.auction.server.networkserver.handler;

import com.auction.common.dto.*;
import com.auction.common.enums.*;
import com.auction.common.model.bid.*;
import com.auction.common.model.user.*;
import com.auction.server.dao.AuctionDao;
import com.auction.server.manager.AuctionManager;
import com.auction.server.networkserver.ClientHandler;
import com.auction.common.util.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.time.LocalDateTime;
import java.util.*;

public class AuctionController implements RequestHandler {
    private final Gson gson = GsonConfig.getInstance();
    private final AuctionDao auctionDao = new AuctionDao();

    @Override
    public String xuLy(JsonObject yeuCau, ClientHandler client) {
        if (yeuCau == null || !yeuCau.has("type") || yeuCau.get("type").isJsonNull()) {
            return taoLoi(yeuCau, StatusCode.BAD_REQUEST, "Thieu truong type.", ErrorCode.BAD_REQUEST);
        }
        String loaiYeuCau = yeuCau.get("type").getAsString();

        switch (loaiYeuCau) {
            case ActionType.JOIN_AUCTION:
                return xuLyThamGiaPhien(yeuCau, client);
            case ActionType.PLACE_BID:
                return xuLyDatGia(yeuCau, client);
            case ActionType.CONFIRM_BUY_NOW:
                return xuLyMuaDut(yeuCau, client);
            case ActionType.CREATE_AUCTION:
                return gson.toJson(new BaseDTOs.ErrorResponse(
                        StatusCode.BAD_REQUEST,
                        "Action CREATE_AUCTION khong duoc ho tro truc tiep. Hay dung CREATE_PRODUCT de tao phien.",
                        ErrorCode.BAD_REQUEST
                ));
            case ActionType.SETTLE_BUY_NOW:
                return xuLyQuyetToanMuaDut(yeuCau, client);
            case ActionType.REGISTER_AUTO_BID:
                return xuLyDangKyAutoBid(yeuCau, client);
            case ActionType.GET_ALL_AUCTIONS:
                return xuLyLayDanhSachDauGia(yeuCau,client);
            case ActionType.GET_JOINED_AUCTIONS:
                return xuLyLayDanhSachPhienThamGia(yeuCau, client);
            case ActionType.GET_AUCTION_BY_ID:
                return xuLyLayPhienTheoID(yeuCau,client);
            case ActionType.LEAVE_AUCTION:
                return xuLyRoiPhien(yeuCau,client);
            case ActionType.CLOSE_AUCTION:
                return xuLyDongPhien(yeuCau,client);
            case ActionType.GET_BID_HISTORY:
                return xuLyLayLichSuBid(yeuCau,client);
            case ActionType.GET_DASHBOARD_STATS:
                return xuLyLayThongKeDashboard(yeuCau, client);
            case ActionType.FOLLOW_AUCTION:
                return xuLyTheoDoi(yeuCau, client, true);
            case ActionType.UNFOLLOW_AUCTION:
                return xuLyTheoDoi(yeuCau, client, false);
            case ActionType.GET_FOLLOWED_AUCTIONS:
                return xuLyLayDanhSachTheoDoi(yeuCau, client);
            case ActionType.SEND_CHAT_MESSAGE:
                return xuLyChat(yeuCau, client);
            case ActionType.GET_SYSTEM_NOTIFICATIONS:
                return xuLyLayThongBaoHeThong(yeuCau, client);
            default:
                return taoLoi(yeuCau, StatusCode.BAD_REQUEST, "Action khong duoc ho tro.", ErrorCode.BAD_REQUEST);
        }
    }

    private String xuLyThamGiaPhien(JsonObject yeuCau, ClientHandler client) {
        int maPhien = layAuctionId(yeuCau);
        if (maPhien <= 0) {
            return taoLoi(yeuCau, StatusCode.BAD_REQUEST, "Thieu auctionId hop le.", ErrorCode.BAD_REQUEST);
        }
        AuctionManager.getInstance().dangKyTheoDoi(maPhien, client);
        JsonObject phanHoi = new JsonObject();
        phanHoi.addProperty("type", "JOIN_AUCTION_RESPONSE");
        phanHoi.addProperty("success", true);
        copyRequestId(yeuCau, phanHoi);
        return gson.toJson(phanHoi);
    }

    private String xuLyDatGia(JsonObject yeuCau, ClientHandler client) {
        AuctionDTOs.BidRequest request = gson.fromJson(yeuCau, AuctionDTOs.BidRequest.class);
        User nguoiDung = client.layNguoiDungHienTai();

        // Lấy lại mã định danh requestId từ client để gửi phản hồi khớp luồng callback
        String requestId = yeuCau.has("requestId") ? yeuCau.get("requestId").getAsString() : null;

        if (nguoiDung == null || !"BIDDER".equals(nguoiDung.getRoleName())) {
            JsonObject errorRes = gson.toJsonTree(new BaseDTOs.ErrorResponse(StatusCode.FORBIDDEN, "Chỉ người mua (Bidder) mới được đặt giá.", ErrorCode.UNAUTHORIZED)).getAsJsonObject();
            errorRes.addProperty("type", "BID_RESPONSE");
            if (requestId != null) errorRes.addProperty("requestId", requestId);
            return gson.toJson(errorRes);
        }

        try {
            BidTransaction giaoDich = new BidTransaction(request.getAuctionId(), (Bidder) nguoiDung, request.getBidAmount(), LocalDateTime.now());
            if (AuctionManager.getInstance().xuLyDatGia(request.getAuctionId(), giaoDich)) {
                JsonObject phanHoi = new JsonObject();
                phanHoi.addProperty("type", "BID_RESPONSE");
                if (requestId != null) phanHoi.addProperty("requestId", requestId);
                phanHoi.addProperty("statusCode", StatusCode.OK);
                phanHoi.addProperty("success", true);
                phanHoi.addProperty("message", "Đặt giá thành công!");
                return gson.toJson(phanHoi);
            }

            JsonObject failRes = gson.toJsonTree(new BaseDTOs.ErrorResponse(StatusCode.BAD_REQUEST, "Đã có người trả giá cao hơn.", ErrorCode.CONCURRENT_CONFLICT)).getAsJsonObject();
            failRes.addProperty("type", "BID_RESPONSE");
            if (requestId != null) failRes.addProperty("requestId", requestId);
            return gson.toJson(failRes);
        } catch (Exception e) {
            JsonObject errRes = gson.toJsonTree(new BaseDTOs.ErrorResponse(StatusCode.SERVER_ERROR, e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR)).getAsJsonObject();
            errRes.addProperty("type", "BID_RESPONSE");
            if (requestId != null) errRes.addProperty("requestId", requestId);
            return gson.toJson(errRes);
        }
    }

    private String xuLyDangKyAutoBid(JsonObject yeuCau, ClientHandler client) {
        User nguoiDung = client.layNguoiDungHienTai();
        if (nguoiDung == null || !"BIDDER".equals(nguoiDung.getRoleName())) {
            return gson.toJson(new BaseDTOs.ErrorResponse(StatusCode.FORBIDDEN, "Chỉ người mua (Bidder) mới được cài auto-bid.", ErrorCode.UNAUTHORIZED));
        }

        int maPhien = yeuCau.has("auctionId") ? yeuCau.get("auctionId").getAsInt() : -1;
        long maxBid = yeuCau.has("maxBid") ? yeuCau.get("maxBid").getAsLong() : -1;

        if (maPhien == -1 || maxBid <= 0) {
            return gson.toJson(new BaseDTOs.ErrorResponse(StatusCode.BAD_REQUEST, "Thiếu thông tin maxBid hoặc auctionId", ErrorCode.BAD_REQUEST));
        }

        try {
            AuctionManager.getInstance().dangKyAutoBid(maPhien, nguoiDung, maxBid);
            JsonObject phanHoi = new JsonObject();
            phanHoi.addProperty("type", "AUTO_BID_RESPONSE");
            phanHoi.addProperty("success", true);
            phanHoi.addProperty("message", "Đã cài đặt Auto-Bid thành công!");
            return gson.toJson(phanHoi);
        } catch (Exception e) {
            return gson.toJson(new BaseDTOs.ErrorResponse(StatusCode.SERVER_ERROR, e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR));
        }
    }

    private String xuLyLayDanhSachDauGia(JsonObject yeuCau, ClientHandler client) {
        boolean featuredRunning = yeuCau.has("featuredRunning")
                && yeuCau.get("featuredRunning").getAsBoolean();
        List<Auction> danhSachPhien = featuredRunning
                ? auctionDao.laySauPhienDangChayNhieuBidNhat()
                : auctionDao.layDanhSachTatCaPhien();
        String categoryFilter = yeuCau.has("category")
                ? yeuCau.get("category").getAsString()
                : "ALL";

        if (danhSachPhien == null) {
            danhSachPhien = new ArrayList<>();
        }

        if (!"ALL".equalsIgnoreCase(categoryFilter) && !"Tất cả".equalsIgnoreCase(categoryFilter)) {

            danhSachPhien.removeIf(phien -> {
                String itemCategory = phien.getItem().getCategory();

                return itemCategory == null
                        || !itemCategory.equalsIgnoreCase(categoryFilter);
            });
        }

        List<AuctionDTOs.AuctionSummaryDTO> summaries = new ArrayList<>();
        for (Auction a : danhSachPhien) {
            summaries.add(new AuctionDTOs.AuctionSummaryDTO(
                    a.getId(),
                    a.getItem().getName(),
                    a.getCurrentHighestBid(),
                    a.getStoredStatus().name(),
                    a.getItem().getImageUrl(),
                    a.getStartTime() != null ? a.getStartTime().toString() : null,
                    a.getEndTime() != null ? a.getEndTime().toString() : null,
                    a.getItem().getCategory()
            ));
        }

        AuctionDTOs.AuctionListResponse response = new AuctionDTOs.AuctionListResponse(
                true, "Lấy danh sách thành công", summaries);
        JsonObject jsonResponse = gson.toJsonTree(response).getAsJsonObject();
        if (yeuCau.has("requestId") && !yeuCau.get("requestId").isJsonNull()) {
            jsonResponse.addProperty("requestId", yeuCau.get("requestId").getAsString());
        }
        jsonResponse.addProperty("serverNow", LocalDateTime.now().toString());

        User user = client.layNguoiDungHienTai();
        com.google.gson.JsonArray followedArray = new com.google.gson.JsonArray();
        if (user != null) {
            List<Integer> followedIds = new com.auction.server.dao.FollowDao().getFollowedAuctionIds(user.getId());
            for (int id : followedIds) followedArray.add(id);
        }
        jsonResponse.add("followedIds", followedArray);

        return gson.toJson(jsonResponse);
    }

    private String xuLyLayDanhSachPhienThamGia(JsonObject yeuCau, ClientHandler client) {
        User nguoiDung = client.layNguoiDungHienTai();
        if (nguoiDung == null || !"BIDDER".equalsIgnoreCase(nguoiDung.getRoleName())) {
            return gson.toJson(new BaseDTOs.ErrorResponse(StatusCode.UNAUTHORIZED, "Chưa đăng nhập!", ErrorCode.UNAUTHORIZED));
        }

        List<Auction> danhSachPhien = auctionDao.layDanhSachPhienThamGia(nguoiDung.getId(), nguoiDung.getRoleName());
        List<AuctionDTOs.AuctionSummaryDTO> summaries = new ArrayList<>();

        for (Auction a : danhSachPhien) {
            summaries.add(new AuctionDTOs.AuctionSummaryDTO(
                    a.getId(),
                    a.getItem().getName(),
                    a.getCurrentHighestBid(),
                    a.getStoredStatus().name(),
                    a.getItem().getImageUrl(),
                    a.getStartTime() != null ? a.getStartTime().toString() : null,
                    a.getEndTime() != null ? a.getEndTime().toString() : null,
                    a.getItem().getCategory()
            ));
        }

        AuctionDTOs.AuctionListResponse response = new AuctionDTOs.AuctionListResponse(
                true, "Lấy danh sách phiên đã tham gia thành công", summaries);
        JsonObject jsonResponse = gson.toJsonTree(response).getAsJsonObject();
        jsonResponse.addProperty("type", "JOINED_AUCTIONS_RESPONSE");
        if (yeuCau.has("requestId") && !yeuCau.get("requestId").isJsonNull()) {
            jsonResponse.addProperty("requestId", yeuCau.get("requestId").getAsString());
        }
        jsonResponse.addProperty("serverNow", LocalDateTime.now().toString());

        com.google.gson.JsonArray followedArray = new com.google.gson.JsonArray();
        List<Integer> followedIds = new com.auction.server.dao.FollowDao().getFollowedAuctionIds(nguoiDung.getId());
        for (int id : followedIds) {
            followedArray.add(id);
        }
        jsonResponse.add("followedIds", followedArray);
        return gson.toJson(jsonResponse);
    }

    private String xuLyMuaDut(JsonObject yeuCau, ClientHandler client) {
        User nguoiDung = client.layNguoiDungHienTai();
        JsonObject phanHoi = new JsonObject();
        phanHoi.addProperty("type", "BUY_NOW_RESPONSE");
        copyRequestId(yeuCau, phanHoi);

        if (nguoiDung == null || !"BIDDER".equals(nguoiDung.getRoleName())) {
            phanHoi.addProperty("success", false);
            phanHoi.addProperty("message", "Chỉ bidder mới được mua đứt.");
            return gson.toJson(phanHoi);
        }

        int auctionId = yeuCau.has("auctionId") ? yeuCau.get("auctionId").getAsInt() : -1;
        if (auctionId == -1) {
            phanHoi.addProperty("success", false);
            phanHoi.addProperty("message", "Thiếu auctionId.");
            return gson.toJson(phanHoi);
        }

        try {
            BidTransaction giaoDich = AuctionManager.getInstance()
                    .xuLyMuaDut(auctionId, (Bidder) nguoiDung);
            Auction phienDaChot = auctionDao.layPhienTheoId(auctionId);
            String tenPhien = phienDaChot != null ? phienDaChot.getItem().getName() : "sản phẩm";

            phanHoi.addProperty("success", true);
            phanHoi.addProperty("auctionId", auctionId);
            phanHoi.addProperty("buyNowPrice", giaoDich.getBidAmount());
            phanHoi.addProperty("message", "Đã chốt phiên ở giá mua đứt.");

            com.auction.server.manager.SystemNotificationManager.getInstance().guiThongBaoRieng(
                    auctionId,
                    nguoiDung.getId(),
                    taoNoiDungThongBaoThanhToan(tenPhien),
                    true
            );

            if (phienDaChot != null) {
                com.auction.server.manager.SystemNotificationManager.getInstance().guiThongBaoRieng(
                        auctionId,
                        phienDaChot.getItem().getSellerId(),
                        taoNoiDungThongBaoSeller(tenPhien, auctionId, nguoiDung.getFullName()),
                        false
                );
            }
            return gson.toJson(phanHoi);
        } catch (Exception e) {
            phanHoi.addProperty("success", false);
            phanHoi.addProperty("message", e.getMessage());
            return gson.toJson(phanHoi);
        }
    }

    private String xuLyQuyetToanMuaDut(JsonObject yeuCau, ClientHandler client) {
        User nguoiDung = client.layNguoiDungHienTai();
        JsonObject phanHoi = new JsonObject();
        phanHoi.addProperty("type", "BUY_NOW_SETTLEMENT_RESPONSE");
        copyRequestId(yeuCau, phanHoi);

        if (nguoiDung == null || !"BIDDER".equals(nguoiDung.getRoleName())) {
            phanHoi.addProperty("success", false);
            phanHoi.addProperty("message", "Chỉ bidder thắng phiên mới được quyết toán.");
            return gson.toJson(phanHoi);
        }

        int auctionId = yeuCau.has("auctionId") ? yeuCau.get("auctionId").getAsInt() : -1;
        String decision = yeuCau.has("decision") ? yeuCau.get("decision").getAsString() : "";
        if (auctionId == -1 || (!"CONFIRM".equalsIgnoreCase(decision) && !"CANCEL".equalsIgnoreCase(decision))) {
            phanHoi.addProperty("success", false);
            phanHoi.addProperty("message", "Thiếu quyết định thanh toán hợp lệ.");
            return gson.toJson(phanHoi);
        }

        boolean thanhToan = "CONFIRM".equalsIgnoreCase(decision);
        com.auction.server.dao.BidderMoneySellerDao.PaymentResult ketQua =
                new com.auction.server.dao.BidderMoneySellerDao()
                        .quyetToanMuaDut(auctionId, nguoiDung.getId(), thanhToan);

        phanHoi.addProperty("success", ketQua.success);
        phanHoi.addProperty("message", ketQua.message);
        phanHoi.addProperty("amount", ketQua.amount);
        if (ketQua.auctionStatus != null) {
            phanHoi.addProperty("auctionStatus", ketQua.auctionStatus);
        }
        if (ketQua.success) {
            AuctionManager.getInstance().capNhatTrangThaiSauThanhToan(
                    auctionId,
                    AuctionStatus.valueOf(ketQua.auctionStatus)
            );
        }
        return gson.toJson(phanHoi);
    }

    private String taoNoiDungThongBaoThanhToan(String tenPhien) {
        return "Chúc mừng bạn đã chiến thắng phiên đấu giá " + tenPhien + ".\n"
                + "Xác nhận thanh toán để chính thức sở hữu sản phẩm.\n\n"
                + "Nếu hủy thanh toán, bạn sẽ chịu phạt 10% tiền đặt giá.";
    }

    private String taoNoiDungThongBaoSeller(String tenPhien, int auctionId, String tenNguoiThang) {
        return "Chúc mừng sản phẩm " + tenPhien + " phiên " + auctionId
                + " đã được bán thành công, người chiến thắng là " + tenNguoiThang + ".";
    }

    private String xuLyLayDanhSachTheoDoi(JsonObject yeuCau, ClientHandler client) {
        User user = client.layNguoiDungHienTai();
        if (user == null) return gson.toJson(new BaseDTOs.ErrorResponse(StatusCode.UNAUTHORIZED, "Chưa đăng nhập", ErrorCode.UNAUTHORIZED));

        List<Integer> followedIds = new com.auction.server.dao.FollowDao().getFollowedAuctionIds(user.getId());
        List<Auction> danhSachPhien = auctionDao.layDanhSachTatCaPhien();

        List<AuctionDTOs.AuctionSummaryDTO> summaries = new ArrayList<>();
        for (Auction a : danhSachPhien) {
            if (followedIds.contains(a.getId())) {
                summaries.add(
                        new AuctionDTOs.AuctionSummaryDTO(
                        a.getId(),
                        a.getItem().getName(),
                        a.getCurrentHighestBid(),
                        a.getStoredStatus().name(),
                        a.getItem().getImageUrl(),
                        a.getStartTime() != null ? a.getStartTime().toString() : null,
                        a.getEndTime() != null ? a.getEndTime().toString() : null,
                        a.getItem().getCategory()
                ));
            }
        }
        AuctionDTOs.AuctionListResponse response = new AuctionDTOs.AuctionListResponse(true, "Thành công", summaries);
        JsonObject jsonResponse = gson.toJsonTree(response).getAsJsonObject();
        jsonResponse.addProperty("type", "FOLLOWED_AUCTIONS_RESPONSE");
        copyRequestId(yeuCau, jsonResponse);
        jsonResponse.addProperty("serverNow", LocalDateTime.now().toString());
        return gson.toJson(jsonResponse);
    }

    private String xuLyLayPhienTheoID(JsonObject yeuCau, ClientHandler client) {
        int idPhien = layAuctionId(yeuCau);
        if (idPhien <= 0) {
            return taoLoi(yeuCau, StatusCode.BAD_REQUEST, "Thieu auctionId hop le.", ErrorCode.BAD_REQUEST);
        }

        // 1. Cố gắng lấy từ RAM trước
        Auction phien = AuctionManager.getInstance().layPhienTheoId(idPhien);

        if (phien == null) {
            phien = auctionDao.layPhienTheoId(idPhien);
        }

        JsonObject phanHoi = new JsonObject();
        if (yeuCau.has("requestId")) {
            phanHoi.addProperty("requestId", yeuCau.get("requestId").getAsString());
        }

        if (phien != null) {
            phanHoi.addProperty("type", ActionType.GET_AUCTION_BY_ID);
            phanHoi.addProperty("success", true);

            JsonObject dataObj = gson.toJsonTree(phien).getAsJsonObject();

            if (phien.getCurrentWinner() != null) {
                JsonObject winnerObj = gson.toJsonTree(phien.getCurrentWinner()).getAsJsonObject();
                if (winnerObj.has("bidder")) {
                    dataObj.add("currentWinner", winnerObj.get("bidder"));
                } else {
                    dataObj.add("currentWinner", winnerObj);
                }
            }

            dataObj.addProperty("currentHighestBid", phien.getCurrentHighestBid());
            dataObj.addProperty("currentPrice", phien.getCurrentHighestBid());
            phanHoi.addProperty("serverNow", LocalDateTime.now().toString());

            User user = client.layNguoiDungHienTai();
            if (user != null) {
                long userMaxAutoBid = auctionDao.layGiaTranAutoBid(idPhien, user.getId());
                if (userMaxAutoBid > 0) {
                    dataObj.addProperty("userMaxAutoBid", userMaxAutoBid);
                }
            }
            phanHoi.add("data", dataObj);
            return gson.toJson(phanHoi);
        }

        phanHoi.addProperty("type", "ERROR_RESPONSE");
        phanHoi.addProperty("success", false);
        phanHoi.addProperty("message", "Phiên đấu giá không tồn tại trong Database!");
        return gson.toJson(phanHoi);
    }

    private String xuLyRoiPhien(JsonObject yeuCau, ClientHandler client) {
        int idPhien = layAuctionId(yeuCau);
        if (idPhien <= 0) {
            return taoLoi(yeuCau, StatusCode.BAD_REQUEST, "Thieu auctionId hop le.", ErrorCode.BAD_REQUEST);
        }
        AuctionManager.getInstance().huyTheoDoi(idPhien, client);

        JsonObject phanHoi = new JsonObject();
        phanHoi.addProperty("type", ActionType.LEAVE_AUCTION);
        phanHoi.addProperty("statusCode", StatusCode.OK);
        phanHoi.addProperty("success", true);
        phanHoi.addProperty("message", "Đã rời phòng đấu giá");
        return gson.toJson(phanHoi);
    }

    private String xuLyDongPhien(JsonObject yeuCau, ClientHandler client) {
        User nguoiDung = client.layNguoiDungHienTai();
        if (nguoiDung == null || !"ADMIN".equals(nguoiDung.getRoleName())) {
            return gson.toJson(new BaseDTOs.ErrorResponse(StatusCode.FORBIDDEN, "Chỉ Quản trị viên (Admin) mới có quyền đóng phiên!", ErrorCode.FORBIDDEN));
        }

        int idPhien = layAuctionId(yeuCau);
        if (idPhien <= 0) {
            return taoLoi(yeuCau, StatusCode.BAD_REQUEST, "Thieu auctionId hop le.", ErrorCode.BAD_REQUEST);
        }
        boolean thanhCong = AuctionManager.getInstance().buocDongPhien(idPhien);

        if (thanhCong) {
            JsonObject phanHoi = new JsonObject();
            phanHoi.addProperty("type", ActionType.CLOSE_AUCTION);
            phanHoi.addProperty("statusCode", StatusCode.OK);
            phanHoi.addProperty("success", true);
            phanHoi.addProperty("message", "Đã ép buộc đóng phiên đấu giá!");
            return gson.toJson(phanHoi);
        } else {
            return gson.toJson(new BaseDTOs.ErrorResponse(StatusCode.NOT_FOUND, "Không tìm thấy phiên đang chạy", ErrorCode.AUCTION_NOT_FOUND));
        }
    }

    private String xuLyLayLichSuBid(JsonObject yeuCau, ClientHandler client) {
        int idPhien = layAuctionId(yeuCau);
        if (idPhien <= 0) {
            return taoLoi(yeuCau, StatusCode.BAD_REQUEST, "Thieu auctionId hop le.", ErrorCode.BAD_REQUEST);
        }
        com.auction.server.dao.BidTransactionDao bidDao = new com.auction.server.dao.BidTransactionDao();
        List<com.auction.common.model.bid.BidLine> lichSu = bidDao.layLichSuPhien(idPhien);

        JsonObject phanHoi = new JsonObject();
        phanHoi.addProperty("type", ActionType.GET_BID_HISTORY);

        // Trả ngược lại requestId để tránh lỗi mất Callback ở Client
        if (yeuCau.has("requestId")) {
            phanHoi.addProperty("requestId", yeuCau.get("requestId").getAsString());
        }

        phanHoi.addProperty("success", true);
        phanHoi.add("data", gson.toJsonTree(lichSu));
        return gson.toJson(phanHoi);
    }

    private String xuLyLayThongKeDashboard(JsonObject yeuCau, ClientHandler client) {
        int activeCount = auctionDao.demPhienDangChay();
        int joinedActiveCount = 0;
        int followedCount = 0;
        int myBidsCount = 0;
        User user = client.layNguoiDungHienTai();
        if (user != null && "BIDDER".equalsIgnoreCase(user.getRoleName())) {
            followedCount = new com.auction.server.dao.FollowDao().countFollowedAuctions(user.getId());
            myBidsCount = auctionDao.demPhienBidderDaThamGia(user.getId());
            joinedActiveCount = auctionDao.demPhienBidderDangThamGia(user.getId());
        }

        JsonObject data = new JsonObject();
        data.addProperty("activeCount", activeCount);
        data.addProperty("joinedActiveCount", joinedActiveCount);
        data.addProperty("endingSoonCount", joinedActiveCount);
        data.addProperty("followedCount", followedCount);
        data.addProperty("myBidsCount", myBidsCount);

        JsonObject phanHoi = new JsonObject();
        phanHoi.addProperty("type", "DASHBOARD_STATS_RESPONSE");
        phanHoi.addProperty("success", true);
        copyRequestId(yeuCau, phanHoi);
        phanHoi.add("data", data);

        return gson.toJson(phanHoi);
    }

    private String xuLyTheoDoi(JsonObject yeuCau, ClientHandler client, boolean isFollow) {
        User nguoiDung = client.layNguoiDungHienTai();
        if (nguoiDung == null) {
            JsonObject loi = gson.toJsonTree(
                    new BaseDTOs.ErrorResponse(StatusCode.UNAUTHORIZED, "Chưa đăng nhập!", ErrorCode.UNAUTHORIZED)
            ).getAsJsonObject();
            copyRequestId(yeuCau, loi);
            return gson.toJson(loi);
        }

        com.auction.server.dao.FollowDao followDao = new com.auction.server.dao.FollowDao();
        int auctionId = yeuCau.has("auctionId") ? yeuCau.get("auctionId").getAsInt() : -1;

        if (auctionId == -1) {
            JsonObject loi = gson.toJsonTree(
                    new BaseDTOs.ErrorResponse(StatusCode.BAD_REQUEST, "Thiếu auctionId.", ErrorCode.BAD_REQUEST)
            ).getAsJsonObject();
            copyRequestId(yeuCau, loi);
            return gson.toJson(loi);
        }

        if (isFollow) followDao.follow(nguoiDung.getId(), auctionId);
        else followDao.unfollow(nguoiDung.getId(), auctionId);

        boolean actualFollowed = followDao.isFollowing(nguoiDung.getId(), auctionId);
        JsonObject phanHoi = new JsonObject();
        phanHoi.addProperty("type", isFollow ? "FOLLOW_RESPONSE" : "UNFOLLOW_RESPONSE");
        phanHoi.addProperty("success", actualFollowed == isFollow);
        phanHoi.addProperty("isFollowed", actualFollowed);
        phanHoi.addProperty("message", actualFollowed == isFollow
                ? (isFollow ? "Đã theo dõi phiên đấu giá" : "Đã hủy theo dõi")
                : "Không cập nhật được trạng thái theo dõi.");
        copyRequestId(yeuCau, phanHoi);
        return gson.toJson(phanHoi);
    }

    private void copyRequestId(JsonObject yeuCau, JsonObject phanHoi) {
        if (yeuCau != null && yeuCau.has("requestId") && !yeuCau.get("requestId").isJsonNull()) {
            phanHoi.addProperty("requestId", yeuCau.get("requestId").getAsString());
        }
    }

    private String xuLyLayThongBaoHeThong(JsonObject yeuCau, ClientHandler client) {
        User nguoiDung = client.layNguoiDungHienTai();
        JsonObject phanHoi = new JsonObject();
        phanHoi.addProperty("type", "SYSTEM_NOTIFICATIONS_RESPONSE");
        copyRequestId(yeuCau, phanHoi);

        if (nguoiDung == null) {
            phanHoi.addProperty("success", false);
            phanHoi.addProperty("message", "Chưa đăng nhập.");
            return gson.toJson(phanHoi);
        }

        phanHoi.addProperty("success", true);
        phanHoi.add("data", new com.auction.server.dao.SystemNotificationDao()
                .layThongBaoCuaNguoiNhan(nguoiDung.getId()));
        return gson.toJson(phanHoi);
    }

    private String xuLyChat(JsonObject yeuCau, ClientHandler client) {
        User nguoiDung = client.layNguoiDungHienTai();
        if (nguoiDung == null) {
            return gson.toJson(new BaseDTOs.ErrorResponse(StatusCode.UNAUTHORIZED, "Chưa đăng nhập!", ErrorCode.UNAUTHORIZED));
        }

        String message = yeuCau.has("message") ? yeuCau.get("message").getAsString() : "";
        int auctionId = yeuCau.has("auctionId") ? yeuCau.get("auctionId").getAsInt() : -1;

        if (message.isEmpty() || auctionId == -1) {
            return gson.toJson(new BaseDTOs.ErrorResponse(StatusCode.BAD_REQUEST, "Thiếu nội dung chat hoặc auctionId", ErrorCode.BAD_REQUEST));
        }

        AuctionManager.getInstance().broadcastChatMessage(auctionId, nguoiDung.getFullName(), message, false);

        JsonObject phanHoi = new JsonObject();
        phanHoi.addProperty("type", "CHAT_SEND_RESPONSE");
        phanHoi.addProperty("success", true);
        phanHoi.addProperty("message", "Đã gửi tin nhắn");
        return gson.toJson(phanHoi);
    }

    private int layAuctionId(JsonObject yeuCau) {
        if (yeuCau == null || !yeuCau.has("auctionId") || yeuCau.get("auctionId").isJsonNull()) {
            return -1;
        }
        try {
            return yeuCau.get("auctionId").getAsInt();
        } catch (RuntimeException e) {
            return -1;
        }
    }

    private String taoLoi(JsonObject yeuCau, int statusCode, String message, String errorCode) {
        JsonObject loi = gson.toJsonTree(new BaseDTOs.ErrorResponse(statusCode, message, errorCode)).getAsJsonObject();
        loi.addProperty("type", "ERROR_RESPONSE");
        copyRequestId(yeuCau, loi);
        return gson.toJson(loi);
    }
}
