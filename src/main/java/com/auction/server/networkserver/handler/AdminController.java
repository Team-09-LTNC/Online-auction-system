package com.auction.server.networkserver.handler;

import com.auction.common.util.GsonConfig;
import com.auction.server.dao.AuctionDao;
import com.auction.server.networkserver.ClientHandler;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.List;

import com.auction.common.enums.ActionType;
import com.auction.common.model.user.User;
import com.auction.common.model.bid.Auction;

//thêm implements RequestHandler.

public class AdminController{

    // private final Gson gson = GsonConfig.getInstance();
    // private final AuctionDao auctionDao = new AuctionDao();

    // @Override
    // public String xuLy(JsonObject yeuCau, ClientHandler client) {
    //     String loaiYeuCau = yeuCau.get("type").getAsString();

    //     // Trích xuất chung mã requestId từ Client gửi lên
    //     String reqId = yeuCau.has("requestId") && !yeuCau.get("requestId").isJsonNull()
    //             ? yeuCau.get("requestId").getAsString()
    //             : null;

        // switch (loaiYeuCau) {
            // case ActionType.ADMIN_GET_ALL_AUCTIONS:
            //     return xuLyLayDanhSachAuction(yeuCau, reqId);
            // default:
                // return null;
        // }
    // }

    // private String xuLyLayDanhSachAuction(JsonObject yeuCau, String reqId) {
    //     List<Auction> auctions = AuctionDao.layTatCaAuction(); // cần thêm method này vào AuctionDao
    //     JsonArray array = new JsonArray();
    //     for (Auction u : auctions) {
    //         JsonObject obj = new JsonObject();
    //         obj.addProperty("username", u.getUsername());
    //         obj.addProperty("fullname", u.getFullName());
    //         obj.addProperty("status", u.getStatus() != null ? u.getStatus() : "ACTIVE");
    //         array.add(obj);
    //     }
    //     JsonObject res = new JsonObject();
    //     res.addProperty("type", "GET_ALL_AUCTIONS_RESPONSE");
    //     res.addProperty("success", true);
    //     res.add("data", array);
    //     if (reqId != null)
    //         res.addProperty("requestId", reqId);
    //     return gson.toJson(res);
    // }
}
