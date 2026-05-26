package com.auction.server.networkserver.handler;

import com.auction.common.util.GsonConfig;
import com.auction.server.manager.AuctionManager;
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
    public String handleRequest(JsonObject yeuCau, ClientHandler client) {
        String loaiYeuCau = yeuCau.get("type").getAsString();

        // Trích xuất chung mã requestId từ Client gửi lên
        String reqId = yeuCau.has("requestId") && !yeuCau.get("requestId").isJsonNull()
                ? yeuCau.get("requestId").getAsString()
                : null;

        switch (loaiYeuCau) {
            case ActionType.ADMIN_GET_ALL_AUCTIONS:
                return handleGetAuctions(yeuCau, reqId);
            case ActionType.ADMIN_GET_PENDING_AUCTIONS:
                return handleGetPendingAuctions(reqId);
            case ActionType.ADMIN_APPROVE_AUCTION:
                return handleApproveAuction(yeuCau, reqId);
            case ActionType.ADMIN_REJECT_AUCTION:
                return handleRejectAuction(yeuCau, reqId);
            case ActionType.ADMIN_GET_INVOICES:
                return handleGetInvoices(reqId);
            case ActionType.ADMIN_CHANGE_AUCTION_STATUS:
                return handleChangeAuctionStatus(yeuCau, reqId);
            case ActionType.ADMIN_DELETE_AUCTION:
                return handleDeleteAuction(yeuCau, reqId);
            case ActionType.ADMIN_GET_TRANSACTIONS:
                return handleGetTransactions(reqId);
            default:
                return null;
        }
    }

    private String handleGetAuctions(JsonObject yeuCau, String reqId) {
        List<Auction> auctions = adminDao.getAllAuctions(); // cần thêm method này vào AdminDao
        JsonArray array = new JsonArray();
        for (Auction a : auctions) {
            JsonObject obj = new JsonObject();
            obj.addProperty("auctionId", a.getId());
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

    private String handleGetTransactions(String reqId) {
        List<com.auction.common.dto.AdminDTOs.TransactionDTO> transactions = adminDao.getTransactions();
        JsonArray array = new JsonArray();

        for (com.auction.common.dto.AdminDTOs.TransactionDTO tx : transactions) {
            JsonObject obj = new JsonObject();
            obj.addProperty("auctionId", tx.getAuctionId());
            obj.addProperty("itemId", tx.getItemId());
            obj.addProperty("itemName", tx.getItemName());
            obj.addProperty("startTime", tx.getStartTime());
            obj.addProperty("endTime", tx.getEndTime());
            obj.addProperty("status", tx.getStatus());
            obj.addProperty("winnerId", tx.getWinnerId());
            obj.addProperty("finalPrice", tx.getFinalPrice());
            array.add(obj);
        }

        JsonObject res = new JsonObject();
        res.addProperty("type", "ADMIN_GET_TRANSACTIONS_RESPONSE");
        res.addProperty("success", true);
        res.add("data", array);
        if (reqId != null)
            res.addProperty("requestId", reqId);
        return gson.toJson(res);
    }

    private String handleDeleteAuction(JsonObject yeuCau, String reqId) {
        int auctionId = yeuCau.get("auctionId").getAsInt();
        boolean ok = adminDao.deleteAuction(auctionId);
        if (ok) {
            AuctionManager.getInstance().syncAfterAuctionDeleted(auctionId);
        }

        JsonObject res = new JsonObject();
        res.addProperty("type", "ADMIN_DELETE_AUCTION_RESPONSE");
        res.addProperty("success", ok);
        res.addProperty("message", ok ? "Đã xoá phiên đấu giá!" : "Xoá phiên thất bại!");
        if (reqId != null)
            res.addProperty("requestId", reqId);
        return gson.toJson(res);
    }

    private String handleGetPendingAuctions(String reqId) {
        List<Auction> auctions = adminDao.getPendingAuctions();
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

    private String handleApproveAuction(JsonObject yeuCau, String reqId) {
        int auctionId = yeuCau.get("auctionId").getAsInt();
        //boolean ok = adminDao.approveAuction(auctionId, "OPEN");
        boolean ok = AuctionManager.getInstance().approveAuctionSession(auctionId);

        JsonObject res = new JsonObject();
        res.addProperty("type", "ADMIN_APPROVE_AUCTION_RESPONSE");
        res.addProperty("success", ok);
        res.addProperty("message", ok ? "Đã duyệt phiên đấu giá!" : "Duyệt thất bại!");
        if (reqId != null)
            res.addProperty("requestId", reqId);
        return gson.toJson(res);
    }

    private String handleRejectAuction(JsonObject yeuCau, String reqId) {
        int auctionId = yeuCau.get("auctionId").getAsInt();
        boolean ok = adminDao.approveAuction(auctionId, "REJECTED");

        JsonObject res = new JsonObject();
        res.addProperty("type", "ADMIN_REJECT_AUCTION_RESPONSE");
        res.addProperty("success", ok);
        res.addProperty("message", ok ? "Đã từ chối phiên đấu giá!" : "Từ chối thất bại!");
        if (ok) {
            AuctionManager.getInstance().broadcastAuctionChanged(auctionId, "REJECTED", "REJECTED");
        }
        if (reqId != null)
            res.addProperty("requestId", reqId);
        return gson.toJson(res);
    }

    /*
     * Xử lý yêu cầu lấy danh sách hóa đơn
     */
    private String handleGetInvoices(String reqId) {
        List<com.auction.common.dto.AdminDTOs.InvoiceDTO> invoices = adminDao.getInvoices();
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

    private String handleChangeAuctionStatus(JsonObject yeuCau, String reqId) {
        int auctionId = yeuCau.get("auctionId").getAsInt();
        String newStatus = yeuCau.get("newStatus").getAsString();

        // Lấy trạng thái hiện tại và thời gian của phiên
        JsonObject auctionInfo = adminDao.getAuctionInfo(auctionId);
        if (auctionInfo == null) {
            JsonObject res = new JsonObject();
            res.addProperty("type", "ADMIN_CHANGE_AUCTION_STATUS_RESPONSE");
            res.addProperty("success", false);
            res.addProperty("message", "Không tìm thấy phiên đấu giá!");
            if (reqId != null)
                res.addProperty("requestId", reqId);
            return gson.toJson(res);
        }

        String currentStatus = auctionInfo.get("status").getAsString();
        String startTime = auctionInfo.get("start_time").getAsString();
        String endTime = auctionInfo.get("end_time").getAsString();

        // Validate chuyển trạng thái
        String validationError = validateStatusTransition(currentStatus, newStatus);
        if (validationError != null) {
            JsonObject res = new JsonObject();
            res.addProperty("type", "ADMIN_CHANGE_AUCTION_STATUS_RESPONSE");
            res.addProperty("success", false);
            res.addProperty("message", validationError);
            if (reqId != null)
                res.addProperty("requestId", reqId);
            return gson.toJson(res);
        }

        // Xử lý CANCELED → reopen: tự tính status đúng theo thời gian
        if ("CANCELED".equals(currentStatus) && "REOPEN".equals(newStatus)) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.LocalDateTime start = java.time.LocalDateTime.parse(startTime);
            java.time.LocalDateTime end = java.time.LocalDateTime.parse(endTime);

            if (now.isBefore(start)) {
                newStatus = "OPEN";
            } else if (now.isAfter(end)) {
                newStatus = "FINISHED";
            } else {
                newStatus = "RUNNING";
            }
        }

        boolean ok = adminDao.updateAuctionStatus(auctionId, newStatus);
        if (ok) AuctionManager.getInstance().syncAfterStatusUpdate(auctionId, newStatus);

        JsonObject res = new JsonObject();
        res.addProperty("type", "ADMIN_CHANGE_AUCTION_STATUS_RESPONSE");
        res.addProperty("success", ok);
        res.addProperty("message", ok ? "Đã cập nhật thành " + newStatus + "!" : "Cập nhật thất bại!");
        res.addProperty("newStatus", newStatus); // trả về status thực tế đã set
        if (reqId != null)
            res.addProperty("requestId", reqId);
        return gson.toJson(res);
    }

    private String validateStatusTransition(String current, String requested) {
        switch (current) {
            case "OPEN":
            case "RUNNING":
                if (!"CANCELED".equals(requested))
                    return "Phiên đang " + current + " chỉ có thể chuyển sang CANCELED!";
                break;
            case "CANCELED":
                if (!"REOPEN".equals(requested))
                    return "Phiên CANCELED chỉ có thể mở lại!";
                break;
            case "FINISHED":
                if (!"PAID".equals(requested))
                    return "Phiên FINISHED chỉ có thể chuyển sang PAID!";
                break;
            case "PAID":
                return "Phiên đã PAID không thể thay đổi trạng thái!";
            case "PENDING":
            case "REJECTED":
                return "Trạng thái này được quản lý qua trang duyệt sản phẩm!";
            default:
                return "Trạng thái không hợp lệ!";
        }
        return null; // hợp lệ
    }
}
