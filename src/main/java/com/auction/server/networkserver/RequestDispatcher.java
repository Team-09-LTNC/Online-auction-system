package com.auction.server.networkserver;

import com.auction.common.enums.ActionType;
import com.auction.server.networkserver.handler.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * RequestDispatcher: Bộ điều phối yêu cầu (Router).
 */
public class RequestDispatcher {
    private static volatile RequestDispatcher instance;
    private final Map<String, RequestHandler> danhSachTrinhXuLy = new HashMap<>();
    private final Gson gson = new Gson();

    private RequestDispatcher() {
        AuthController authController = new AuthController();
        AuctionController auctionController = new AuctionController();
        ProductController productController = new ProductController();

        // Nhóm Auth
        danhSachTrinhXuLy.put(ActionType.LOGIN, authController);
        danhSachTrinhXuLy.put(ActionType.REGISTER, authController);
        danhSachTrinhXuLy.put(ActionType.LOGOUT, authController);

        // Nhóm Auction
        danhSachTrinhXuLy.put(ActionType.JOIN_AUCTION, auctionController);
        danhSachTrinhXuLy.put(ActionType.PLACE_BID, auctionController);
        danhSachTrinhXuLy.put(ActionType.CREATE_AUCTION, auctionController);
        danhSachTrinhXuLy.put(ActionType.GET_ALL_AUCTIONS, auctionController);
        danhSachTrinhXuLy.put(ActionType.GET_AUCTION_BY_ID, auctionController);
        danhSachTrinhXuLy.put(ActionType.LEAVE_AUCTION, auctionController);
        danhSachTrinhXuLy.put(ActionType.CLOSE_AUCTION, auctionController);
        danhSachTrinhXuLy.put(ActionType.GET_BID_HISTORY, auctionController);

        // Nhóm Product
        danhSachTrinhXuLy.put(ActionType.CREATE_PRODUCT, productController);
        danhSachTrinhXuLy.put(ActionType.GET_ALL_PRODUCTS, productController);
        danhSachTrinhXuLy.put(ActionType.DELETE_PRODUCT, productController);
        danhSachTrinhXuLy.put(ActionType.SEARCH_PRODUCT, productController);
        danhSachTrinhXuLy.put(ActionType.UPDATE_PRODUCT, productController);
    }

    public static RequestDispatcher layInstance() {
        if (instance == null) {
            synchronized (RequestDispatcher.class) {
                if (instance == null) instance = new RequestDispatcher();
            }
        }
        return instance;
    }

    public String dieuPhoi(String loaiYeuCau, JsonObject yeuCau, ClientHandler client) {
        RequestHandler trinhXuLy = danhSachTrinhXuLy.get(loaiYeuCau);
        if (trinhXuLy != null) {
            return trinhXuLy.xuLy(yeuCau, client);
        }

        JsonObject loi = new JsonObject();
        loi.addProperty("success", false);
        loi.addProperty("errorCode", "ERR_UNKNOWN");
        loi.addProperty("message", "Không tìm thấy Controller xử lý cho yêu cầu này.");
        return gson.toJson(loi);
    }
}