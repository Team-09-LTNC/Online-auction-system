package com.auction.server.networkserver.handler;

import com.auction.common.dto.AuctionDTOs;
import com.auction.common.dto.BaseDTOs;
import com.auction.common.enums.ActionType;
import com.auction.common.enums.ErrorCode;
import com.auction.common.enums.StatusCode;
import com.auction.common.model.bid.Auction;
import com.auction.common.model.bid.BidTransaction;
import com.auction.common.model.user.Bidder;
import com.auction.common.model.user.User;
import com.auction.server.manager.AuctionManager;
import com.auction.server.networkserver.ClientHandler;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.time.LocalDateTime;
import java.util.*;

/**
 * AuctionController: Nhóm các chức năng liên quan đến phiên đấu giá (nhóm chức năng của AUCTION trong ActionType)
 */
public class AuctionController implements RequestHandler {
    private final Gson gson = new Gson();

    @Override
    public String xuLy(JsonObject yeuCau, ClientHandler client) {
        String loaiYeuCau = yeuCau.get("type").getAsString();

        switch (loaiYeuCau) {
            case ActionType.JOIN_AUCTION:
                return xuLyThamGiaPhien(yeuCau, client);
            case ActionType.PLACE_BID:
                return xuLyDatGia(yeuCau, client);
            case ActionType.GET_ALL_AUCTIONS:
                 return xuLyLayDanhSachDauGia(yeuCau,client);
            case ActionType.GET_AUCTION_BY_ID:
                return xuLyLayPhienTheoID(yeuCau,client);
            case ActionType.LEAVE_AUCTION:
                return xuLyRoiPhien(yeuCau,client);
            case ActionType.CLOSE_AUCTION:
                return xuLyDongPhien(yeuCau,client);
            case ActionType.GET_BID_HISTORY:
                return xuLyLayLichSuBid(yeuCau,client);
            default:
                return null;
        }
    }
    // JOIN_AUCTION
    private String xuLyThamGiaPhien(JsonObject yeuCau, ClientHandler client) {
        int maPhien = yeuCau.get("auctionId").getAsInt();
        AuctionManager.getInstance().dangKyTheoDoi(maPhien, client);
        return null; // Không cần phản hồi ngay, dữ liệu sẽ được push qua Observer
    }

    // PLACE_BID
    private String xuLyDatGia(JsonObject yeuCau, ClientHandler client) {
        AuctionDTOs.BidRequest request = gson.fromJson(yeuCau, AuctionDTOs.BidRequest.class);
        User nguoiDung = client.layNguoiDungHienTai();

        if (nguoiDung == null || !"BIDDER".equals(nguoiDung.getRoleName())) {
            return gson.toJson(new BaseDTOs.ErrorResponse(StatusCode.FORBIDDEN, "Chỉ người mua (Bidder) mới được đặt giá.", ErrorCode.UNAUTHORIZED));
        }

        try {
            BidTransaction giaoDich = new BidTransaction(request.getAuctionId(), (Bidder) nguoiDung, request.getBidAmount(), LocalDateTime.now());
            if (AuctionManager.getInstance().xuLyDatGia(request.getAuctionId(), giaoDich)) {
                return gson.toJson(new BaseDTOs.Response("BID_RESPONSE", StatusCode.OK, true, "Đặt giá thành công!") {});
            }
            return gson.toJson(new BaseDTOs.ErrorResponse(StatusCode.BAD_REQUEST, "Đã có người trả giá cao hơn.", ErrorCode.CONCURRENT_CONFLICT));
        } catch (Exception e) {
            return gson.toJson(new BaseDTOs.ErrorResponse(StatusCode.SERVER_ERROR, e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR));
        }
    }
    // GET_ALL_AUCTION
    private String xuLyLayDanhSachDauGia(JsonObject yeuCau, ClientHandler client) {
        List<Auction> danhSachPhien = AuctionManager.getInstance().layDanhSachPhienDangChay();
        List<AuctionDTOs.AuctionSummaryDTO> summaries = new ArrayList<>();

        for (Auction a : danhSachPhien) {
            summaries.add(new AuctionDTOs.AuctionSummaryDTO(
                    a.getId(),
                    a.getItem().getName(),
                    a.getCurrentHighestBid(),
                    a.getStatus().name()
            ));
        }

        AuctionDTOs.AuctionListResponse response = new AuctionDTOs.AuctionListResponse(
                true, "Lấy danh sách thành công", summaries);
        return gson.toJson(response);
    }

    // GET_AUCTION_BY_ID
    private String xuLyLayPhienTheoID(JsonObject yeuCau, ClientHandler client) {
        int idPhien = yeuCau.get("auctionId").getAsInt();
        Auction phien = AuctionManager.getInstance().layPhienTheoId(idPhien);

        if (phien != null) {
            JsonObject phanHoi = new JsonObject();
            phanHoi.addProperty("type", ActionType.GET_AUCTION_BY_ID);
            phanHoi.addProperty("success", true);
            phanHoi.add("data", gson.toJsonTree(phien));
            return gson.toJson(phanHoi);
        }
        return gson.toJson(new BaseDTOs.ErrorResponse(
                StatusCode.NOT_FOUND, "Phiên đấu giá không tồn tại hoặc đã kết thúc", ErrorCode.AUCTION_NOT_FOUND));
    }

    //LEAVE_AUCTION
    private String xuLyRoiPhien(JsonObject yeuCau, ClientHandler client) {
        int idPhien = yeuCau.get("auctionId").getAsInt();

        // Hủy đăng ký Observer để Server ngừng push giá về Client này
        AuctionManager.getInstance().huyTheoDoi(idPhien, client);

        return gson.toJson(new BaseDTOs.Response(
                ActionType.LEAVE_AUCTION, StatusCode.OK, true, "Đã rời phòng đấu giá") {});
    }

    // CLOSE_AUCTION
    private String xuLyDongPhien(JsonObject yeuCau, ClientHandler client) {
        User nguoiDung = client.layNguoiDungHienTai();

        // KIỂM TRA BẢO MẬT CHẶT CHẼ
        if (nguoiDung == null || !"ADMIN".equals(nguoiDung.getRoleName())) {
            return gson.toJson(new BaseDTOs.ErrorResponse(
                    StatusCode.FORBIDDEN, "Chỉ Quản trị viên (Admin) mới có quyền đóng phiên!", ErrorCode.FORBIDDEN));
        }

        int idPhien = yeuCau.get("auctionId").getAsInt();
        boolean thanhCong = AuctionManager.getInstance().buocDongPhien(idPhien);

        if (thanhCong) {
            return gson.toJson(new BaseDTOs.Response(
                    ActionType.CLOSE_AUCTION, StatusCode.OK, true, "Đã ép buộc đóng phiên đấu giá!") {});
        } else {
            return gson.toJson(new BaseDTOs.ErrorResponse(
                    StatusCode.NOT_FOUND, "Không tìm thấy phiên đang chạy", ErrorCode.AUCTION_NOT_FOUND));
        }
    }

    // GET_BID_HISTORY
    private String xuLyLayLichSuBid(JsonObject yeuCau, ClientHandler client) {
        int idPhien = yeuCau.get("auctionId").getAsInt();

        // Gọi DAO để lấy lịch sử
        com.auction.server.dao.BidTransactionDao bidDao = new com.auction.server.dao.BidTransactionDao();
        List<com.auction.common.model.bid.BidLine> lichSu = bidDao.layLichSuPhien(idPhien);

        JsonObject phanHoi = new JsonObject();
        phanHoi.addProperty("type", ActionType.GET_BID_HISTORY);
        phanHoi.addProperty("success", true);
        phanHoi.add("data", gson.toJsonTree(lichSu));

        return gson.toJson(phanHoi);
    }
}