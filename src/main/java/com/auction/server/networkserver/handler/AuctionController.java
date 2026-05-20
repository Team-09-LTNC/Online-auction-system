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
        String loaiYeuCau = yeuCau.get("type").getAsString();

        switch (loaiYeuCau) {
            case ActionType.JOIN_AUCTION:
                return xuLyThamGiaPhien(yeuCau, client);
            case ActionType.PLACE_BID:
                return xuLyDatGia(yeuCau, client);
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
            default:
                return null;
        }
    }

    private String xuLyThamGiaPhien(JsonObject yeuCau, ClientHandler client) {
        int maPhien = yeuCau.get("auctionId").getAsInt();
        AuctionManager.getInstance().dangKyTheoDoi(maPhien, client);
        return null;
    }

    private String xuLyDatGia(JsonObject yeuCau, ClientHandler client) {
        AuctionDTOs.BidRequest request = gson.fromJson(yeuCau, AuctionDTOs.BidRequest.class);
        User nguoiDung = client.layNguoiDungHienTai();

        if (nguoiDung == null || !"BIDDER".equals(nguoiDung.getRoleName())) {
            return gson.toJson(new BaseDTOs.ErrorResponse(StatusCode.FORBIDDEN, "Chỉ người mua (Bidder) mới được đặt giá.", ErrorCode.UNAUTHORIZED));
        }

        try {
            BidTransaction giaoDich = new BidTransaction(request.getAuctionId(), (Bidder) nguoiDung, request.getBidAmount(), LocalDateTime.now());
            if (AuctionManager.getInstance().xuLyDatGia(request.getAuctionId(), giaoDich)) {
                JsonObject phanHoi = new JsonObject();
                phanHoi.addProperty("type", "BID_RESPONSE");
                phanHoi.addProperty("statusCode", StatusCode.OK);
                phanHoi.addProperty("success", true);
                phanHoi.addProperty("message", "Đặt giá thành công!");
                return gson.toJson(phanHoi);
            }
            return gson.toJson(new BaseDTOs.ErrorResponse(StatusCode.BAD_REQUEST, "Đã có người trả giá cao hơn.", ErrorCode.CONCURRENT_CONFLICT));
        } catch (Exception e) {
            return gson.toJson(new BaseDTOs.ErrorResponse(StatusCode.SERVER_ERROR, e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR));
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
        // 1. Móc toàn bộ các phiên đang chạy dưới DB lên RAM
        List<Auction> danhSachPhien = auctionDao.layDanhSachPhienDangChay();
        if (danhSachPhien == null) {
            danhSachPhien = new ArrayList<>();
        }

        List<Auction> danhSachChoMo = auctionDao.layDanhSachPhienChoMo();
        if (danhSachChoMo != null) {
            for (Auction openAuction : danhSachChoMo) {
                // Chống trùng lặp: Nếu RAM chưa chứa ID này thì mới add vào danh sách tổng
                boolean daTonTai = false;
                for (Auction activeAuction : danhSachPhien) {
                    if (activeAuction.getId() == openAuction.getId()) {
                        daTonTai = true;
                        break;
                    }
                }
                if (!daTonTai) {
                    danhSachPhien.add(openAuction);
                }
            }
        }

        // 3. Đọc yêu cầu lọc từ Client (Giữ nguyên logic OOP của Kiên)
        String categoryFilter = yeuCau.has("category") ? yeuCau.get("category").getAsString() : "Tất cả";
        if (!"Tất cả".equals(categoryFilter)) {
            danhSachPhien.removeIf(phien -> {
                com.auction.common.model.item.Item item = phien.getItem();
                if ("Điện tử".equalsIgnoreCase(categoryFilter) && !(item instanceof com.auction.common.model.item.Electronics)) return true;
                if ("Xe cộ".equalsIgnoreCase(categoryFilter) && !(item instanceof com.auction.common.model.item.Vehicle)) return true;
                if ("Nghệ thuật".equalsIgnoreCase(categoryFilter) && !(item instanceof com.auction.common.model.item.Art)) return true;
                if ("Khác".equalsIgnoreCase(categoryFilter)) {
                    if (item instanceof com.auction.common.model.item.Electronics ||
                            item instanceof com.auction.common.model.item.Vehicle ||
                            item instanceof com.auction.common.model.item.Art) {
                        return true;
                    }
                }
                return false;
            });
        }

        // 4. Đóng gói kết quả gửi về Client thông qua danh sách tóm tắt duy nhất
        List<AuctionDTOs.AuctionSummaryDTO> summaries = new ArrayList<>();
        for (Auction a : danhSachPhien) {
            summaries.add(new AuctionDTOs.AuctionSummaryDTO(
                    a.getId(),
                    a.getItem().getName(),
                    a.getCurrentHighestBid(),
                    a.getStatus().name(), // Trả về chuỗi nguyên bản "RUNNING" hoặc "OPEN"
                    a.getItem().getImageUrl()
            ));
        }

        AuctionDTOs.AuctionListResponse response = new AuctionDTOs.AuctionListResponse(
                true, "Lấy danh sách thành công", summaries);
        JsonObject jsonResponse = gson.toJsonTree(response).getAsJsonObject();

        // 5. Kèm trạng thái Tim đỏ (Followed)
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
        if (nguoiDung == null) {
            return gson.toJson(new BaseDTOs.ErrorResponse(StatusCode.UNAUTHORIZED, "Chưa đăng nhập!", ErrorCode.UNAUTHORIZED));
        }

        List<Auction> danhSachPhien = auctionDao.layDanhSachPhienThamGia(nguoiDung.getId(), nguoiDung.getRoleName());
        List<AuctionDTOs.AuctionSummaryDTO> summaries = new ArrayList<>();

        for (Auction a : danhSachPhien) {
            summaries.add(new AuctionDTOs.AuctionSummaryDTO(
                    a.getId(),
                    a.getItem().getName(),
                    a.getCurrentHighestBid(),
                    a.getStatus().name(),
                    a.getItem().getImageUrl()
            ));
        }

        AuctionDTOs.AuctionListResponse response = new AuctionDTOs.AuctionListResponse(
                true, "Lấy danh sách phiên đã tham gia thành công", summaries);
        JsonObject jsonResponse = gson.toJsonTree(response).getAsJsonObject();
        jsonResponse.addProperty("type", "JOINED_AUCTIONS_RESPONSE");
        return gson.toJson(jsonResponse);
    }

    private String xuLyLayDanhSachTheoDoi(JsonObject yeuCau, ClientHandler client) {
        User user = client.layNguoiDungHienTai();
        if (user == null) return gson.toJson(new BaseDTOs.ErrorResponse(StatusCode.UNAUTHORIZED, "Chưa đăng nhập", ErrorCode.UNAUTHORIZED));

        List<Integer> followedIds = new com.auction.server.dao.FollowDao().getFollowedAuctionIds(user.getId());
        List<Auction> danhSachPhien = auctionDao.layDanhSachPhienDangChay();

        // Cứu hộ nốt danh sách follow: Nhét thêm cả những phiên OPEN đã lên lịch mở
        List<Auction> danhSachChoMo = auctionDao.layDanhSachPhienChoMo();
        if (danhSachChoMo != null) {
            danhSachPhien.addAll(danhSachChoMo);
        }

        List<AuctionDTOs.AuctionSummaryDTO> summaries = new ArrayList<>();
        for (Auction a : danhSachPhien) {
            if (followedIds.contains(a.getId())) {
                summaries.add(new AuctionDTOs.AuctionSummaryDTO(
                        a.getId(), a.getItem().getName(), a.getCurrentHighestBid(), a.getStatus().name(), a.getItem().getImageUrl()
                ));
            }
        }
        AuctionDTOs.AuctionListResponse response = new AuctionDTOs.AuctionListResponse(true, "Thành công", summaries);
        JsonObject jsonResponse = gson.toJsonTree(response).getAsJsonObject();
        jsonResponse.addProperty("type", "FOLLOWED_AUCTIONS_RESPONSE");
        return gson.toJson(jsonResponse);
    }

    // Tìm kiếm phòng đấu giá chi tiết theo ID kể cả khi chưa hiển thị trên RAM
    private String xuLyLayPhienTheoID(JsonObject yeuCau, ClientHandler client) {
        int idPhien = yeuCau.get("auctionId").getAsInt();
        Auction phien = AuctionManager.getInstance().layPhienTheoId(idPhien);

        if (phien == null) {
            List<Auction> tatCaPhienChoMo = auctionDao.layDanhSachPhienChoMo();
            if (tatCaPhienChoMo != null) {
                for (Auction a : tatCaPhienChoMo) {
                    if (a.getId() == idPhien) {
                        phien = a;
                        break;
                    }
                }
            }
        }

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

    private String xuLyRoiPhien(JsonObject yeuCau, ClientHandler client) {
        int idPhien = yeuCau.get("auctionId").getAsInt();
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

        int idPhien = yeuCau.get("auctionId").getAsInt();
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
        int idPhien = yeuCau.get("auctionId").getAsInt();
        com.auction.server.dao.BidTransactionDao bidDao = new com.auction.server.dao.BidTransactionDao();
        List<com.auction.common.model.bid.BidLine> lichSu = bidDao.layLichSuPhien(idPhien);

        JsonObject phanHoi = new JsonObject();
        phanHoi.addProperty("type", ActionType.GET_BID_HISTORY);
        phanHoi.addProperty("success", true);
        phanHoi.add("data", gson.toJsonTree(lichSu));
        return gson.toJson(phanHoi);
    }

    private String xuLyLayThongKeDashboard(JsonObject yeuCau, ClientHandler client) {
        List<Auction> danhSachPhien = auctionDao.layDanhSachPhienDangChay();
        int activeCount = danhSachPhien.size();

        int endingSoonCount = 0;
        LocalDateTime now = LocalDateTime.now();
        for (Auction a : danhSachPhien) {
            if (a.getEndTime() != null && a.getEndTime().minusHours(1).isBefore(now)) {
                endingSoonCount++;
            }
        }

        int followedCount = 0;
        int myBidsCount = 0;

        JsonObject data = new JsonObject();
        data.addProperty("activeCount", activeCount);
        data.addProperty("endingSoonCount", endingSoonCount);
        data.addProperty("followedCount", followedCount);
        data.addProperty("myBidsCount", myBidsCount);

        JsonObject phanHoi = new JsonObject();
        phanHoi.addProperty("type", "DASHBOARD_STATS_RESPONSE");
        phanHoi.addProperty("success", true);
        phanHoi.add("data", data);

        return gson.toJson(phanHoi);
    }

    private String xuLyTheoDoi(JsonObject yeuCau, ClientHandler client, boolean isFollow) {
        User nguoiDung = client.layNguoiDungHienTai();
        if (nguoiDung == null) {
            return gson.toJson(new BaseDTOs.ErrorResponse(StatusCode.UNAUTHORIZED, "Chưa đăng nhập!", ErrorCode.UNAUTHORIZED));
        }

        com.auction.server.dao.FollowDao followDao = new com.auction.server.dao.FollowDao();
        int auctionId = yeuCau.has("auctionId") ? yeuCau.get("auctionId").getAsInt() : -1;

        if (auctionId != -1) {
            if (isFollow) followDao.follow(nguoiDung.getId(), auctionId);
            else followDao.unfollow(nguoiDung.getId(), auctionId);
        }

        JsonObject phanHoi = new JsonObject();
        phanHoi.addProperty("type", isFollow ? "FOLLOW_RESPONSE" : "UNFOLLOW_RESPONSE");
        phanHoi.addProperty("success", true);
        phanHoi.addProperty("message", isFollow ? "Đã theo dõi phiên đấu giá" : "Đã hủy theo dõi");
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
}