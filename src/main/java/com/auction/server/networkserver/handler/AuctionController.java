package com.auction.server.networkserver.handler;

import com.auction.common.dto.AuctionDTOs;
import com.auction.common.dto.BaseDTOs;
import com.auction.common.enums.ActionType;
import com.auction.common.enums.AuctionStatus;
import com.auction.common.enums.ErrorCode;
import com.auction.common.enums.StatusCode;
import com.auction.common.model.bid.Auction;
import com.auction.common.model.bid.BidLine;
import com.auction.common.model.bid.BidTransaction;
import com.auction.common.model.user.Bidder;
import com.auction.common.model.user.User;
import com.auction.common.util.GsonConfig;
import com.auction.server.dao.AuctionDao;
import com.auction.server.dao.BidTransactionDao;
import com.auction.server.dao.BidderMoneySellerDao;
import com.auction.server.dao.BidderPenaltyDao;
import com.auction.server.dao.FollowDao;
import com.auction.server.dao.SystemNotificationDao;
import com.auction.server.dao.UserDao;
import com.auction.server.manager.AuctionManager;
import com.auction.server.networkserver.ClientHandler;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionController implements RequestHandler {
    private static final String SELLER_SELF_BID_MESSAGE = "Khong duoc tu bid san pham cua chinh minh.";
    private static final String LOCKED_BIDDER_MESSAGE = "Tai khoan cua ban dang bi khoa, khong the tham gia dau gia.";

    private final Gson gson = GsonConfig.getInstance();
    private final AuctionDao auctionDao = new AuctionDao();
    private final UserDao userDao = new UserDao();

    @Override
    public String xuLy(JsonObject yeuCau, ClientHandler client) {
        if (yeuCau == null || !yeuCau.has("type") || yeuCau.get("type").isJsonNull()) {
            return AuctionControllerUtil.taoLoi(gson, yeuCau, StatusCode.BAD_REQUEST, "Thieu truong type.", ErrorCode.BAD_REQUEST);
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
            case ActionType.REMOVE_AUTO_BID:
                return xuLyXoaAutoBid(yeuCau, client);
            case ActionType.GET_ALL_AUCTIONS:
                return xuLyLayDanhSachDauGia(yeuCau, client);
            case ActionType.GET_JOINED_AUCTIONS:
                return xuLyLayDanhSachPhienThamGia(yeuCau, client);
            case ActionType.GET_AUCTION_BY_ID:
                return xuLyLayPhienTheoID(yeuCau, client);
            case ActionType.LEAVE_AUCTION:
                return xuLyRoiPhien(yeuCau, client);
            case ActionType.CLOSE_AUCTION:
                return xuLyDongPhien(yeuCau, client);
            case ActionType.GET_BID_HISTORY:
                return xuLyLayLichSuBid(yeuCau, client);
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
            case ActionType.MARK_SYSTEM_NOTIFICATIONS_READ:
                return xuLyDanhDauThongBaoDaDoc(yeuCau, client);
            default:
                return AuctionControllerUtil.taoLoi(gson, yeuCau, StatusCode.BAD_REQUEST, "Action khong duoc ho tro.", ErrorCode.BAD_REQUEST);
        }
    }

    private String xuLyThamGiaPhien(JsonObject yeuCau, ClientHandler client) {
        int maPhien = AuctionControllerUtil.layAuctionId(yeuCau);
        if (maPhien <= 0) {
            return AuctionControllerUtil.taoLoi(gson, yeuCau, StatusCode.BAD_REQUEST, "Thieu auctionId hop le.", ErrorCode.BAD_REQUEST);
        }
        AuctionManager.getInstance().dangKyTheoDoi(maPhien, client);
        JsonObject phanHoi = new JsonObject();
        phanHoi.addProperty("type", "JOIN_AUCTION_RESPONSE");
        phanHoi.addProperty("success", true);
        AuctionControllerUtil.copyRequestId(yeuCau, phanHoi);
        return gson.toJson(phanHoi);
    }

    private String xuLyDatGia(JsonObject yeuCau, ClientHandler client) {
        AuctionDTOs.BidRequest request = gson.fromJson(yeuCau, AuctionDTOs.BidRequest.class);
        User nguoiDung = client.layNguoiDungHienTai();
        String requestId = yeuCau.has("requestId") ? yeuCau.get("requestId").getAsString() : null;

        if (nguoiDung == null || !"BIDDER".equals(nguoiDung.getRoleName())) {
            if (nguoiDung != null && "SELLER".equalsIgnoreCase(nguoiDung.getRoleName())
                    && laSellerCuaPhien(request.getAuctionId(), nguoiDung.getId())) {
                JsonObject errorRes = AuctionControllerUtil.taoPhanHoiDonGian("BID_RESPONSE", false, SELLER_SELF_BID_MESSAGE);
                if (requestId != null) {
                    errorRes.addProperty("requestId", requestId);
                }
                return gson.toJson(errorRes);
            }
            JsonObject errorRes = gson.toJsonTree(new BaseDTOs.ErrorResponse(
                    StatusCode.FORBIDDEN, "Chi nguoi mua (Bidder) moi duoc dat gia.", ErrorCode.UNAUTHORIZED
            )).getAsJsonObject();
            errorRes.addProperty("type", "BID_RESPONSE");
            if (requestId != null) {
                errorRes.addProperty("requestId", requestId);
            }
            return gson.toJson(errorRes);
        }
        if (laTaiKhoanBiKhoa(nguoiDung)) {
            JsonObject errorRes = gson.toJsonTree(new BaseDTOs.ErrorResponse(
                    StatusCode.FORBIDDEN, LOCKED_BIDDER_MESSAGE, ErrorCode.FORBIDDEN
            )).getAsJsonObject();
            errorRes.addProperty("type", "BID_RESPONSE");
            if (requestId != null) {
                errorRes.addProperty("requestId", requestId);
            }
            return gson.toJson(errorRes);
        }

        try {
            BidTransaction giaoDich = new BidTransaction(
                    request.getAuctionId(), (Bidder) nguoiDung, request.getBidAmount(), LocalDateTime.now()
            );
            if (AuctionManager.getInstance().xuLyDatGia(request.getAuctionId(), giaoDich)) {
                JsonObject phanHoi = new JsonObject();
                phanHoi.addProperty("type", "BID_RESPONSE");
                if (requestId != null) {
                    phanHoi.addProperty("requestId", requestId);
                }
                phanHoi.addProperty("statusCode", StatusCode.OK);
                phanHoi.addProperty("success", true);
                phanHoi.addProperty("message", "Dat gia thanh cong!");
                return gson.toJson(phanHoi);
            }
            JsonObject failRes = gson.toJsonTree(new BaseDTOs.ErrorResponse(
                    StatusCode.BAD_REQUEST, "Da co nguoi tra gia cao hon.", ErrorCode.CONCURRENT_CONFLICT
            )).getAsJsonObject();
            failRes.addProperty("type", "BID_RESPONSE");
            if (requestId != null) {
                failRes.addProperty("requestId", requestId);
            }
            return gson.toJson(failRes);
        } catch (Exception e) {
            JsonObject errRes = gson.toJsonTree(new BaseDTOs.ErrorResponse(
                    StatusCode.SERVER_ERROR, e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR
            )).getAsJsonObject();
            errRes.addProperty("type", "BID_RESPONSE");
            if (requestId != null) {
                errRes.addProperty("requestId", requestId);
            }
            return gson.toJson(errRes);
        }
    }

    private String xuLyDangKyAutoBid(JsonObject yeuCau, ClientHandler client) {
        String requestId = yeuCau.has("requestId") && !yeuCau.get("requestId").isJsonNull()
                ? yeuCau.get("requestId").getAsString()
                : null;
        User nguoiDung = client.layNguoiDungHienTai();
        int maPhien = yeuCau.has("auctionId") ? yeuCau.get("auctionId").getAsInt() : -1;
        long maxBid = yeuCau.has("maxBid") ? yeuCau.get("maxBid").getAsLong() : -1;
        long bidStep = yeuCau.has("bidStep") ? yeuCau.get("bidStep").getAsLong() : -1;
        if (nguoiDung == null || !"BIDDER".equals(nguoiDung.getRoleName())) {
            if (nguoiDung != null && "SELLER".equalsIgnoreCase(nguoiDung.getRoleName())
                    && laSellerCuaPhien(maPhien, nguoiDung.getId())) {
                JsonObject phanHoi = AuctionControllerUtil.taoPhanHoiDonGian("AUTO_BID_RESPONSE", false, SELLER_SELF_BID_MESSAGE);
                if (requestId != null) {
                    phanHoi.addProperty("requestId", requestId);
                }
                return gson.toJson(phanHoi);
            }
            JsonObject phanHoi = AuctionControllerUtil.taoPhanHoiDonGian("AUTO_BID_RESPONSE", false, "Chi nguoi mua (Bidder) moi duoc cai auto-bid.");
            if (requestId != null) {
                phanHoi.addProperty("requestId", requestId);
            }
            return gson.toJson(phanHoi);
        }
        if (maPhien == -1 || maxBid <= 0 || bidStep <= 0) {
            JsonObject phanHoi = AuctionControllerUtil.taoPhanHoiDonGian(
                    "AUTO_BID_RESPONSE",
                    false,
                    "Thieu thong tin maxBid, bidStep hoac auctionId"
            );
            if (requestId != null) {
                phanHoi.addProperty("requestId", requestId);
            }
            return gson.toJson(phanHoi);
        }
        if (laTaiKhoanBiKhoa(nguoiDung)) {
            JsonObject phanHoi = AuctionControllerUtil.taoPhanHoiDonGian(
                    "AUTO_BID_RESPONSE", false, LOCKED_BIDDER_MESSAGE
            );
            if (requestId != null) {
                phanHoi.addProperty("requestId", requestId);
            }
            return gson.toJson(phanHoi);
        }
        try {
            AuctionManager.getInstance().dangKyAutoBid(maPhien, nguoiDung, maxBid, bidStep);
            JsonObject phanHoi = new JsonObject();
            phanHoi.addProperty("type", "AUTO_BID_RESPONSE");
            if (requestId != null) {
                phanHoi.addProperty("requestId", requestId);
            }
            phanHoi.addProperty("success", true);
            phanHoi.addProperty("message", "Da cai dat Auto-Bid thanh cong!");
            return gson.toJson(phanHoi);
        } catch (Exception e) {
            JsonObject phanHoi = AuctionControllerUtil.taoPhanHoiDonGian("AUTO_BID_RESPONSE", false, e.getMessage());
            if (requestId != null) {
                phanHoi.addProperty("requestId", requestId);
            }
            return gson.toJson(phanHoi);
        }
    }

    private String xuLyXoaAutoBid(JsonObject yeuCau, ClientHandler client) {
        String requestId = yeuCau.has("requestId") && !yeuCau.get("requestId").isJsonNull()
                ? yeuCau.get("requestId").getAsString()
                : null;
        User nguoiDung = client.layNguoiDungHienTai();
        int maPhien = yeuCau.has("auctionId") ? yeuCau.get("auctionId").getAsInt() : -1;
        if (nguoiDung == null || !"BIDDER".equals(nguoiDung.getRoleName())) {
            JsonObject phanHoi = AuctionControllerUtil.taoPhanHoiDonGian("AUTO_BID_RESPONSE", false, "Chi bidder moi duoc xoa auto-bid.");
            if (requestId != null) {
                phanHoi.addProperty("requestId", requestId);
            }
            return gson.toJson(phanHoi);
        }
        if (maPhien <= 0) {
            JsonObject phanHoi = AuctionControllerUtil.taoPhanHoiDonGian("AUTO_BID_RESPONSE", false, "Thieu auctionId.");
            if (requestId != null) {
                phanHoi.addProperty("requestId", requestId);
            }
            return gson.toJson(phanHoi);
        }
        try {
            AuctionManager.getInstance().xoaAutoBid(maPhien, nguoiDung);
            JsonObject phanHoi = new JsonObject();
            phanHoi.addProperty("type", "AUTO_BID_RESPONSE");
            if (requestId != null) {
                phanHoi.addProperty("requestId", requestId);
            }
            phanHoi.addProperty("success", true);
            phanHoi.addProperty("message", "Da xoa Auto-Bid thanh cong.");
            return gson.toJson(phanHoi);
        } catch (Exception e) {
            JsonObject phanHoi = AuctionControllerUtil.taoPhanHoiDonGian("AUTO_BID_RESPONSE", false, e.getMessage());
            if (requestId != null) {
                phanHoi.addProperty("requestId", requestId);
            }
            return gson.toJson(phanHoi);
        }
    }

    private String xuLyLayDanhSachDauGia(JsonObject yeuCau, ClientHandler client) {
        boolean featuredRunning = yeuCau.has("featuredRunning") && yeuCau.get("featuredRunning").getAsBoolean();
        List<Auction> danhSachPhien = featuredRunning ? auctionDao.laySauPhienDangChayNhieuBidNhat() : auctionDao.layDanhSachTatCaPhien();
        String categoryFilter = yeuCau.has("category") ? yeuCau.get("category").getAsString() : "ALL";
        if (danhSachPhien == null) {
            danhSachPhien = new ArrayList<>();
        }
        if (!"ALL".equalsIgnoreCase(categoryFilter) && !"Tat ca".equalsIgnoreCase(categoryFilter)) {
            danhSachPhien.removeIf(phien -> {
                String itemCategory = phien.getItem().getCategory();
                return itemCategory == null || !itemCategory.equalsIgnoreCase(categoryFilter);
            });
        }
        List<AuctionDTOs.AuctionSummaryDTO> summaries = AuctionControllerUtil.taoAuctionSummaries(danhSachPhien);
        AuctionDTOs.AuctionListResponse response = new AuctionDTOs.AuctionListResponse(true, "Lay danh sach thanh cong", summaries);
        JsonObject jsonResponse = gson.toJsonTree(response).getAsJsonObject();
        if (yeuCau.has("requestId") && !yeuCau.get("requestId").isJsonNull()) {
            jsonResponse.addProperty("requestId", yeuCau.get("requestId").getAsString());
        }
        jsonResponse.addProperty("serverNow", LocalDateTime.now().toString());
        User user = client.layNguoiDungHienTai();
        jsonResponse.add("followedIds", AuctionControllerUtil.taoFollowedIdsArray(user != null ? user.getId() : -1));
        return gson.toJson(jsonResponse);
    }

    private String xuLyLayDanhSachPhienThamGia(JsonObject yeuCau, ClientHandler client) {
        User nguoiDung = client.layNguoiDungHienTai();
        if (nguoiDung == null || !"BIDDER".equalsIgnoreCase(nguoiDung.getRoleName())) {
            return gson.toJson(new BaseDTOs.ErrorResponse(StatusCode.UNAUTHORIZED, "Chua dang nhap!", ErrorCode.UNAUTHORIZED));
        }
        List<Auction> danhSachPhien = auctionDao.layDanhSachPhienThamGia(nguoiDung.getId(), nguoiDung.getRoleName());
        List<AuctionDTOs.AuctionSummaryDTO> summaries = AuctionControllerUtil.taoAuctionSummaries(danhSachPhien);
        AuctionDTOs.AuctionListResponse response = new AuctionDTOs.AuctionListResponse(
                true, "Lay danh sach phien da tham gia thanh cong", summaries
        );
        JsonObject jsonResponse = gson.toJsonTree(response).getAsJsonObject();
        jsonResponse.addProperty("type", "JOINED_AUCTIONS_RESPONSE");
        if (yeuCau.has("requestId") && !yeuCau.get("requestId").isJsonNull()) {
            jsonResponse.addProperty("requestId", yeuCau.get("requestId").getAsString());
        }
        jsonResponse.addProperty("serverNow", LocalDateTime.now().toString());
        jsonResponse.add("followedIds", AuctionControllerUtil.taoFollowedIdsArray(nguoiDung.getId()));
        return gson.toJson(jsonResponse);
    }

    private String xuLyMuaDut(JsonObject yeuCau, ClientHandler client) {
        User nguoiDung = client.layNguoiDungHienTai();
        JsonObject phanHoi = new JsonObject();
        phanHoi.addProperty("type", "BUY_NOW_RESPONSE");
        AuctionControllerUtil.copyRequestId(yeuCau, phanHoi);
        int auctionId = yeuCau.has("auctionId") ? yeuCau.get("auctionId").getAsInt() : -1;
        if (nguoiDung == null || !"BIDDER".equals(nguoiDung.getRoleName())) {
            if (nguoiDung != null && "SELLER".equalsIgnoreCase(nguoiDung.getRoleName()) && laSellerCuaPhien(auctionId, nguoiDung.getId())) {
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
        if (laTaiKhoanBiKhoa(nguoiDung)) {
            phanHoi.addProperty("success", false);
            phanHoi.addProperty("message", LOCKED_BIDDER_MESSAGE);
            return gson.toJson(phanHoi);
        }
        try {
            BidTransaction giaoDich = AuctionManager.getInstance().xuLyMuaDut(auctionId, (Bidder) nguoiDung);
            Auction phienDaChot = auctionDao.layPhienTheoId(auctionId);
            String tenPhien = phienDaChot != null ? phienDaChot.getItem().getName() : "san pham";
            phanHoi.addProperty("success", true);
            phanHoi.addProperty("auctionId", auctionId);
            phanHoi.addProperty("buyNowPrice", giaoDich.getBidAmount());
            phanHoi.addProperty("message", "Da chot phien o gia mua dut.");
            AuctionNotificationService.guiThongBaoMuaDut(
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

    private String xuLyQuyetToanMuaDut(JsonObject yeuCau, ClientHandler client) {
        User nguoiDung = client.layNguoiDungHienTai();
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
        BidderMoneySellerDao.PaymentResult ketQua = new BidderMoneySellerDao().quyetToanMuaDut(auctionId, nguoiDung.getId(), thanhToan);
        phanHoi.addProperty("success", ketQua.success);
        phanHoi.addProperty("message", ketQua.message);
        phanHoi.addProperty("amount", ketQua.amount);
        if (ketQua.auctionStatus != null) {
            phanHoi.addProperty("auctionStatus", ketQua.auctionStatus);
        }
        if (ketQua.success) {
            AuctionManager.getInstance().capNhatTrangThaiSauThanhToan(auctionId, AuctionStatus.valueOf(ketQua.auctionStatus));
            Auction phien = auctionDao.layPhienTheoId(auctionId);
            int sellerId = phien != null ? phien.getItem().getSellerId() : -1;
            String tenPhien = phien != null ? phien.getItem().getName() : ("phien #" + auctionId);
            AuctionNotificationService.guiThongBaoSauQuyetToan(
                    auctionId, nguoiDung.getId(), sellerId, tenPhien, ketQua.amount, thanhToan
            );
        }
        return gson.toJson(phanHoi);
    }

    private String xuLyLayDanhSachTheoDoi(JsonObject yeuCau, ClientHandler client) {
        User user = client.layNguoiDungHienTai();
        if (user == null) {
            return gson.toJson(new BaseDTOs.ErrorResponse(StatusCode.UNAUTHORIZED, "Chua dang nhap", ErrorCode.UNAUTHORIZED));
        }
        List<Integer> followedIds = new FollowDao().getFollowedAuctionIds(user.getId());
        List<Auction> danhSachPhien = auctionDao.layDanhSachTatCaPhien();
        List<AuctionDTOs.AuctionSummaryDTO> summaries = new ArrayList<>();
        for (Auction a : danhSachPhien) {
            if (followedIds.contains(a.getId())) {
                summaries.addAll(AuctionControllerUtil.taoAuctionSummaries(List.of(a)));
            }
        }
        AuctionDTOs.AuctionListResponse response = new AuctionDTOs.AuctionListResponse(true, "Thanh cong", summaries);
        JsonObject jsonResponse = gson.toJsonTree(response).getAsJsonObject();
        jsonResponse.addProperty("type", "FOLLOWED_AUCTIONS_RESPONSE");
        AuctionControllerUtil.copyRequestId(yeuCau, jsonResponse);
        jsonResponse.addProperty("serverNow", LocalDateTime.now().toString());
        return gson.toJson(jsonResponse);
    }

    private String xuLyLayPhienTheoID(JsonObject yeuCau, ClientHandler client) {
        int idPhien = AuctionControllerUtil.layAuctionId(yeuCau);
        if (idPhien <= 0) {
            return AuctionControllerUtil.taoLoi(gson, yeuCau, StatusCode.BAD_REQUEST, "Thieu auctionId hop le.", ErrorCode.BAD_REQUEST);
        }
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
                dataObj.add("currentWinner", winnerObj.has("bidder") ? winnerObj.get("bidder") : winnerObj);
            }
            dataObj.addProperty("currentHighestBid", phien.getCurrentHighestBid());
            dataObj.addProperty("currentPrice", phien.getCurrentHighestBid());
            dataObj.addProperty("antiSnipingEnabled", phien.isAntiSnipingEnabled());
            phanHoi.addProperty("serverNow", LocalDateTime.now().toString());
            User user = client.layNguoiDungHienTai();
            if (user != null) {
                long userMaxAutoBid = auctionDao.layGiaTranAutoBid(idPhien, user.getId());
                if (userMaxAutoBid > 0) {
                    dataObj.addProperty("userMaxAutoBid", userMaxAutoBid);
                    long userAutoBidStep = auctionDao.layBuocGiaAutoBid(idPhien, user.getId());
                    if (userAutoBidStep > 0) {
                        dataObj.addProperty("userAutoBidStep", userAutoBidStep);
                    }
                }
            }
            phanHoi.add("data", dataObj);
            return gson.toJson(phanHoi);
        }
        phanHoi.addProperty("type", "ERROR_RESPONSE");
        phanHoi.addProperty("success", false);
        phanHoi.addProperty("message", "Phien dau gia khong ton tai trong Database!");
        return gson.toJson(phanHoi);
    }

    private String xuLyRoiPhien(JsonObject yeuCau, ClientHandler client) {
        int idPhien = AuctionControllerUtil.layAuctionId(yeuCau);
        if (idPhien <= 0) {
            return AuctionControllerUtil.taoLoi(gson, yeuCau, StatusCode.BAD_REQUEST, "Thieu auctionId hop le.", ErrorCode.BAD_REQUEST);
        }
        AuctionManager.getInstance().huyTheoDoi(idPhien, client);
        JsonObject phanHoi = new JsonObject();
        phanHoi.addProperty("type", ActionType.LEAVE_AUCTION);
        phanHoi.addProperty("statusCode", StatusCode.OK);
        phanHoi.addProperty("success", true);
        phanHoi.addProperty("message", "Da roi phong dau gia");
        return gson.toJson(phanHoi);
    }

    private String xuLyDongPhien(JsonObject yeuCau, ClientHandler client) {
        User nguoiDung = client.layNguoiDungHienTai();
        if (nguoiDung == null || !"ADMIN".equals(nguoiDung.getRoleName())) {
            return gson.toJson(new BaseDTOs.ErrorResponse(
                    StatusCode.FORBIDDEN, "Chi Quan tri vien (Admin) moi co quyen dong phien!", ErrorCode.FORBIDDEN
            ));
        }
        int idPhien = AuctionControllerUtil.layAuctionId(yeuCau);
        if (idPhien <= 0) {
            return AuctionControllerUtil.taoLoi(gson, yeuCau, StatusCode.BAD_REQUEST, "Thieu auctionId hop le.", ErrorCode.BAD_REQUEST);
        }
        boolean thanhCong = AuctionManager.getInstance().buocDongPhien(idPhien);
        if (thanhCong) {
            JsonObject phanHoi = new JsonObject();
            phanHoi.addProperty("type", ActionType.CLOSE_AUCTION);
            phanHoi.addProperty("statusCode", StatusCode.OK);
            phanHoi.addProperty("success", true);
            phanHoi.addProperty("message", "Da ep buoc dong phien dau gia!");
            return gson.toJson(phanHoi);
        }
        return gson.toJson(new BaseDTOs.ErrorResponse(StatusCode.NOT_FOUND, "Khong tim thay phien dang chay", ErrorCode.AUCTION_NOT_FOUND));
    }

    private String xuLyLayLichSuBid(JsonObject yeuCau, ClientHandler client) {
        int idPhien = AuctionControllerUtil.layAuctionId(yeuCau);
        if (idPhien <= 0) {
            return AuctionControllerUtil.taoLoi(gson, yeuCau, StatusCode.BAD_REQUEST, "Thieu auctionId hop le.", ErrorCode.BAD_REQUEST);
        }
        List<BidLine> lichSu = new BidTransactionDao().layLichSuPhien(idPhien);
        JsonObject phanHoi = new JsonObject();
        phanHoi.addProperty("type", ActionType.GET_BID_HISTORY);
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
            followedCount = new FollowDao().countFollowedAuctions(user.getId());
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
        AuctionControllerUtil.copyRequestId(yeuCau, phanHoi);
        phanHoi.add("data", data);
        return gson.toJson(phanHoi);
    }

    private String xuLyTheoDoi(JsonObject yeuCau, ClientHandler client, boolean isFollow) {
        return AuctionMiscHandler.xuLyTheoDoi(gson, yeuCau, client, isFollow);
    }

    private String xuLyLayThongBaoHeThong(JsonObject yeuCau, ClientHandler client) {
        return AuctionMiscHandler.xuLyLayThongBaoHeThong(gson, yeuCau, client);
    }

    private String xuLyDanhDauThongBaoDaDoc(JsonObject yeuCau, ClientHandler client) {
        return AuctionMiscHandler.xuLyDanhDauThongBaoDaDoc(gson, yeuCau, client);
    }

    private String xuLyChat(JsonObject yeuCau, ClientHandler client) {
        return AuctionMiscHandler.xuLyChat(gson, yeuCau, client);
    }

    private boolean laSellerCuaPhien(int auctionId, int userId) {
        return AuctionControllerUtil.laSellerCuaPhien(auctionDao, auctionId, userId);
    }

    private boolean laTaiKhoanBiKhoa(User nguoiDung) {
        if (nguoiDung == null) {
            return false;
        }
        if ("LOCKED".equalsIgnoreCase(nguoiDung.getStatus())) {
            return true;
        }
        BidderPenaltyDao.LockInfo lockInfo = new BidderPenaltyDao().layTrangThaiTamKhoa(nguoiDung.getId());
        if (lockInfo.locked) {
            return true;
        }
        return userDao.timTheoTenDangNhap(nguoiDung.getUsername())
                .map(user -> "LOCKED".equalsIgnoreCase(user.getStatus()))
                .orElse(false);
    }
}
