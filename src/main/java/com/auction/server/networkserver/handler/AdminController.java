package com.auction.server.networkserver.handler;

import com.auction.common.util.GsonConfig;
import com.auction.server.dao.AuctionDao;
import com.auction.server.dao.AdminDao;
import com.auction.server.networkserver.ClientHandler;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auction.common.enums.ActionType;
import com.auction.common.model.bid.Auction;

public class AdminController implements RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final Gson gson = GsonConfig.getInstance();
    private final AdminDao adminDao = new AdminDao();

    @Override
    public String xuLy(JsonObject yeuCau, ClientHandler client) {
        String loaiYeuCau = yeuCau.get("type").getAsString();

        // Trích xuất chung mã requestId từ Client gửi lên
        String reqId = yeuCau.has("requestId") && !yeuCau.get("requestId").isJsonNull()
                ? yeuCau.get("requestId").getAsString()
                : null;

        switch (loaiYeuCau) {
            case ActionType.ADMIN_GET_ALL_AUCTIONS:
                return xuLyLayDanhSachAuction(yeuCau, reqId);
            case ActionType.ADMIN_GET_PENDING_AUCTIONS:
                return xuLyLayDanhSachChoDuyet(reqId);
            case ActionType.ADMIN_APPROVE_AUCTION:
                return xuLyDuyetAuction(yeuCau, reqId);
            case ActionType.ADMIN_REJECT_AUCTION:
                return xuLyTuChoiAuction(yeuCau, reqId);
            case ActionType.ADMIN_GET_INVOICES:
                return xuLyLayDanhSachHoaDon(reqId);
            default:
                return null;
        }
    }

    private String xuLyLayDanhSachAuction(JsonObject yeuCau, String reqId) {
        List<Auction> auctions = adminDao.layDanhSachTatCaAuctions(); // cần thêm method này vào AdminDao
        JsonArray array = new JsonArray();
        for (Auction a : auctions) {
            JsonObject obj = new JsonObject();
            obj.addProperty("itemname", a.getItem().getName());
            obj.addProperty("starttime", a.getStartTime().toString());
            obj.addProperty("endtime", a.getEndTime().toString());
            obj.addProperty("status", a.getStatus() != null ? a.getStatus().toString() : "OPEN");
            obj.addProperty("imageurl", a.getItem().getImageUrl());
            array.add(obj);
        }
        JsonObject res = new JsonObject();
        res.addProperty("type", "GET_ALL_AUCTIONS_RESPONSE");
        res.addProperty("success", true);
        res.add("data", array);
        if (reqId != null)
            res.addProperty("requestId", reqId);
        return gson.toJson(res);
    }

    private String xuLyLayDanhSachChoDuyet(String reqId) {
        List<Auction> auctions = adminDao.layDanhSachChoDuyet();
        JsonArray array = new JsonArray();
        for (Auction a : auctions) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", a.getId());
            obj.addProperty("itemName", a.getItem().getName());
            obj.addProperty("sellerId", a.getItem().getSellerId());
            obj.addProperty("description", a.getItem().getDescription());
            obj.addProperty("startingPrice", a.getItem().getStartingPrice());
            obj.addProperty("category", a.getItem().getCategory());
            obj.addProperty("startTime", a.getStartTime().toString());
            obj.addProperty("endTime", a.getEndTime().toString());
            obj.addProperty("imageUrl", a.getItem().getImageUrl());
            array.add(obj);
        }
        JsonObject res = new JsonObject();
        res.addProperty("type", "ADMIN_GET_PENDING_AUCTIONS_RESPONSE");
        res.addProperty("success", true);
        res.add("data", array);

        logger.info("Đã lấy danh sách phiên đấu giá chờ duyệt trong AdminController");

        if (reqId != null)
            res.addProperty("requestId", reqId);
        return gson.toJson(res);
    }

    private String xuLyDuyetAuction(JsonObject yeuCau, String reqId) {
        int auctionId = yeuCau.get("auctionId").getAsInt();
        boolean ok = adminDao.duyetAuction(auctionId, "OPEN");

        JsonObject res = new JsonObject();
        res.addProperty("type", "ADMIN_APPROVE_AUCTION_RESPONSE");
        res.addProperty("success", ok);
        res.addProperty("message", ok ? "Đã duyệt phiên đấu giá!" : "Duyệt thất bại!");
        if (reqId != null)
            res.addProperty("requestId", reqId);
        return gson.toJson(res);
    }

    private String xuLyTuChoiAuction(JsonObject yeuCau, String reqId) {
        int auctionId = yeuCau.get("auctionId").getAsInt();
        boolean ok = adminDao.duyetAuction(auctionId, "REJECTED");

        JsonObject res = new JsonObject();
        res.addProperty("type", "ADMIN_REJECT_AUCTION_RESPONSE");
        res.addProperty("success", ok);
        res.addProperty("message", ok ? "Đã từ chối phiên đấu giá!" : "Từ chối thất bại!");
        if (reqId != null)
            res.addProperty("requestId", reqId);
        return gson.toJson(res);
    }

    /*
     * Xử lý yêu cầu lấy danh sách hóa đơn
     */
    private String xuLyLayDanhSachHoaDon(String reqId) {
        List<com.auction.common.dto.AdminDTOs.InvoiceDTO> invoices = adminDao.layDanhSachHoaDon();
        JsonArray array = new JsonArray();
        long tongDoanhThu = 0;

        for (com.auction.common.dto.AdminDTOs.InvoiceDTO inv : invoices) {
            JsonObject obj = new JsonObject();
            obj.addProperty("auctionId", inv.getAuctionId());
            obj.addProperty("itemId", inv.getItemId());
            obj.addProperty("itemName", inv.getItemName());
            obj.addProperty("sellerId", inv.getSellerId());
            obj.addProperty("winnerId", inv.getWinnerId());
            obj.addProperty("highestBid", inv.getHighestBid());
            array.add(obj);
            tongDoanhThu += inv.getHighestBid();
        }

        JsonObject res = new JsonObject();
        res.addProperty("type", "ADMIN_GET_INVOICES_RESPONSE");
        res.addProperty("success", true);
        res.addProperty("tongDoanhThu", tongDoanhThu); // ← tổng doanh thu tính sẵn trên server
        res.add("data", array);
        if (reqId != null)
            res.addProperty("requestId", reqId);
        return gson.toJson(res);
    }
}
