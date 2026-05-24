package com.auction.server.networkserver;

import com.auction.common.enums.ActionType;
import com.auction.server.networkserver.handler.AdminController;
import com.auction.server.networkserver.handler.AuctionController;
import com.auction.server.networkserver.handler.AuthController;
import com.auction.server.networkserver.handler.ProductController;
import com.auction.server.networkserver.handler.RequestHandler;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestDispatcher {
  private static final Logger LOGGER = LoggerFactory.getLogger(RequestDispatcher.class);
  private static volatile RequestDispatcher instance;

  private final Map<String, RequestHandler> danhSachTrinhXuLy = new HashMap<>();
  private final Gson gson = new Gson();

  private RequestDispatcher() {
    AuthController authController = new AuthController();
    AuctionController auctionController = new AuctionController();
    ProductController productController = new ProductController();
    AdminController adminController = new AdminController();

    dangKyNhomAuth(authController);
    dangKyNhomAuction(auctionController, authController);
    dangKyNhomProduct(productController);
    dangKyNhomAdmin(adminController);
  }

  public static RequestDispatcher layInstance() {
    if (instance == null) {
      synchronized (RequestDispatcher.class) {
        if (instance == null) {
          instance = new RequestDispatcher();
        }
      }
    }
    return instance;
  }

  public String dieuPhoi(String loaiYeuCau, JsonObject yeuCau, ClientHandler client) {
    RequestHandler trinhXuLy = danhSachTrinhXuLy.get(loaiYeuCau);
    if (trinhXuLy == null) {
      LOGGER.warn("Khong tim thay controller cho request '{}'.", loaiYeuCau);
      return taoLoiKhongTimThayController();
    }

    LOGGER.trace("Xu ly request '{}' boi controller {}",
        loaiYeuCau, trinhXuLy.getClass().getSimpleName());
    String phanHoi = trinhXuLy.xuLy(yeuCau, client);
    if (phanHoi != null) {
      return phanHoi;
    }

    LOGGER.warn("Controller {} khong tra phan hoi cho request '{}'.",
        trinhXuLy.getClass().getSimpleName(), loaiYeuCau);
    return taoLoiNoiBo(yeuCau);
  }

  private void dangKyNhomAuth(AuthController authController) {
    danhSachTrinhXuLy.put(ActionType.LOGIN, authController);
    danhSachTrinhXuLy.put(ActionType.REGISTER, authController);
    danhSachTrinhXuLy.put(ActionType.LOGOUT, authController);
    danhSachTrinhXuLy.put(ActionType.TOP_UP_MONEY, authController);
    danhSachTrinhXuLy.put(ActionType.WITHDRAW_MONEY, authController);
    danhSachTrinhXuLy.put(ActionType.ADMIN_GET_ALL_BIDDERS, authController);
    danhSachTrinhXuLy.put(ActionType.ADMIN_GET_ALL_SELLERS, authController);
    danhSachTrinhXuLy.put(ActionType.ADMIN_TOGGLE_LOCK_USER, authController);
  }

  private void dangKyNhomAuction(AuctionController auctionController, AuthController authController) {
    danhSachTrinhXuLy.put(ActionType.JOIN_AUCTION, auctionController);
    danhSachTrinhXuLy.put(ActionType.PLACE_BID, auctionController);
    danhSachTrinhXuLy.put(ActionType.CONFIRM_BUY_NOW, auctionController);
    danhSachTrinhXuLy.put(ActionType.SETTLE_BUY_NOW, auctionController);
    danhSachTrinhXuLy.put(ActionType.CREATE_AUCTION, auctionController);
    danhSachTrinhXuLy.put(ActionType.GET_ALL_AUCTIONS, auctionController);
    danhSachTrinhXuLy.put(ActionType.GET_JOINED_AUCTIONS, auctionController);
    danhSachTrinhXuLy.put(ActionType.GET_AUCTION_BY_ID, auctionController);
    danhSachTrinhXuLy.put(ActionType.LEAVE_AUCTION, auctionController);
    danhSachTrinhXuLy.put(ActionType.CLOSE_AUCTION, auctionController);
    danhSachTrinhXuLy.put(ActionType.GET_BID_HISTORY, auctionController);
    danhSachTrinhXuLy.put(ActionType.GET_DASHBOARD_STATS, auctionController);
    danhSachTrinhXuLy.put(ActionType.REGISTER_AUTO_BID, auctionController);
    danhSachTrinhXuLy.put(ActionType.FOLLOW_AUCTION, auctionController);
    danhSachTrinhXuLy.put(ActionType.UNFOLLOW_AUCTION, auctionController);
    danhSachTrinhXuLy.put(ActionType.GET_FOLLOWED_AUCTIONS, auctionController);
    danhSachTrinhXuLy.put(ActionType.SEND_CHAT_MESSAGE, auctionController);
    danhSachTrinhXuLy.put(ActionType.GET_SYSTEM_NOTIFICATIONS, auctionController);
    danhSachTrinhXuLy.put(ActionType.MARK_SYSTEM_NOTIFICATIONS_READ, auctionController);
    danhSachTrinhXuLy.put("GET_WALLET_HISTORY", authController);
  }

  private void dangKyNhomProduct(ProductController productController) {
    danhSachTrinhXuLy.put(ActionType.CREATE_PRODUCT, productController);
    danhSachTrinhXuLy.put(ActionType.GET_ALL_PRODUCTS, productController);
    danhSachTrinhXuLy.put(ActionType.DELETE_PRODUCT, productController);
    danhSachTrinhXuLy.put(ActionType.SEARCH_PRODUCT, productController);
    danhSachTrinhXuLy.put(ActionType.GET_PRODUCT_BY_ID, productController);
    danhSachTrinhXuLy.put(ActionType.UPDATE_PRODUCT, productController);
    danhSachTrinhXuLy.put(ActionType.GET_MY_PRODUCTS, productController);
  }

  private void dangKyNhomAdmin(AdminController adminController) {
    danhSachTrinhXuLy.put(ActionType.ADMIN_GET_ALL_AUCTIONS, adminController);
    danhSachTrinhXuLy.put(ActionType.ADMIN_GET_PENDING_AUCTIONS, adminController);
    danhSachTrinhXuLy.put(ActionType.ADMIN_APPROVE_AUCTION, adminController);
    danhSachTrinhXuLy.put(ActionType.ADMIN_REJECT_AUCTION, adminController);
    danhSachTrinhXuLy.put(ActionType.ADMIN_GET_INVOICES, adminController);
  }

  private String taoLoiNoiBo(JsonObject yeuCau) {
    JsonObject loiNoiBo = new JsonObject();
    loiNoiBo.addProperty("type", "ERROR_RESPONSE");
    loiNoiBo.addProperty("success", false);
    loiNoiBo.addProperty("errorCode", "ERR_HANDLER_NO_RESPONSE");
    loiNoiBo.addProperty("message", "Yeu cau da duoc nhan nhung khong co du lieu phan hoi.");
    if (yeuCau != null && yeuCau.has("requestId") && !yeuCau.get("requestId").isJsonNull()) {
      loiNoiBo.addProperty("requestId", yeuCau.get("requestId").getAsString());
    }
    return gson.toJson(loiNoiBo);
  }

  private String taoLoiKhongTimThayController() {
    JsonObject loi = new JsonObject();
    loi.addProperty("success", false);
    loi.addProperty("errorCode", "ERR_UNKNOWN");
    loi.addProperty("message", "Khong tim thay controller xu ly cho yeu cau nay.");
    return gson.toJson(loi);
  }
}
