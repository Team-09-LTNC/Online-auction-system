package com.auction.server.networkserver.handler;

import com.auction.common.util.GsonConfig;
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
        if (yeuCau == null || !yeuCau.has("type") || yeuCau.get("type").isJsonNull()) {
            JsonObject loi = new JsonObject();
            loi.addProperty("type", "ERROR_RESPONSE");
            loi.addProperty("success", false);
            loi.addProperty("message", "Thieu truong type.");
            return gson.toJson(loi);
        }
        String loaiYeuCau = yeuCau.get("type").getAsString();

        // Trích xuất chung mã requestId từ Client gửi lên
        String reqId = yeuCau.has("requestId") && !yeuCau.get("requestId").isJsonNull()
                ? yeuCau.get("requestId").getAsString()
                : null;

        switch (loaiYeuCau) {
            case ActionType.ADMIN_GET_ALL_AUCTIONS:
                return xuLyLayDanhSachAuction(yeuCau, reqId);
            default:
                JsonObject loi = new JsonObject();
                loi.addProperty("type", "ERROR_RESPONSE");
                loi.addProperty("success", false);
                loi.addProperty("message", "Action khong duoc ho tro.");
                if (reqId != null) loi.addProperty("requestId", reqId);
                return gson.toJson(loi);
        }
    }

    private String xuLyLayDanhSachAuction(JsonObject yeuCau, String reqId) {
        List<Auction> auctions = adminDao.layDanhSachTatCaAuctions(); // cần thêm method này vào AdminDao
        JsonArray array = new JsonArray();
        for (Auction a : auctions) {
            JsonObject obj = new JsonObject();
            obj.addProperty("itemname", a.getItem().getName());
            obj.addProperty("starttime", a.getStartTime() != null ? a.getStartTime().toString() : null);
            obj.addProperty("endtime", a.getEndTime() != null ? a.getEndTime().toString() : null);
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

}
