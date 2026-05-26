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

    registerAuthHandlers(authController);
    registerAuctionHandlers(auctionController, authController);
    registerProductHandlers(productController);
    registerAdminHandlers(adminController);
  }

  public static RequestDispatcher getInstance() {
    if (instance == null) {
      synchronized (RequestDispatcher.class) {
        if (instance == null) {
          instance = new RequestDispatcher();
        }
      }
    }
    return instance;
  }

  public String dispatch(String loaiYeuCau, JsonObject yeuCau, ClientHandler client) {
    RequestHandler trinhXuLy = danhSachTrinhXuLy.get(loaiYeuCau);
    if (trinhXuLy == null) {
      LOGGER.warn("Không tìm thấy controller cho request '{}'.", loaiYeuCau);
      return buildControllerNotFoundError();
    }

    LOGGER.trace("Xử lý request '{}' bởi controller {}",
        loaiYeuCau, trinhXuLy.getClass().getSimpleName());
    String phanHoi = trinhXuLy.handleRequest(yeuCau, client);
    if (phanHoi != null) {
      return phanHoi;
    }

    LOGGER.warn("Controller {} không trả phản hồi cho request '{}'.",
        trinhXuLy.getClass().getSimpleName(), loaiYeuCau);
    return buildInternalError(yeuCau);
  }

  private void registerAuthHandlers(AuthController authController) {
    danhSachTrinhXuLy.put(ActionType.LOGIN, authController);
    danhSachTrinhXuLy.put(ActionType.REGISTER, authController);
    danhSachTrinhXuLy.put(ActionType.LOGOUT, authController);
    danhSachTrinhXuLy.put(ActionType.TOP_UP_MONEY, authController);
    danhSachTrinhXuLy.put(ActionType.WITHDRAW_MONEY, authController);
    danhSachTrinhXuLy.put(ActionType.ADMIN_GET_ALL_BIDDERS, authController);
    danhSachTrinhXuLy.put(ActionType.ADMIN_GET_ALL_SELLERS, authController);
    danhSachTrinhXuLy.put(ActionType.ADMIN_TOGGLE_LOCK_USER, authController);
  }

  private void registerAuctionHandlers(AuctionController auctionController, AuthController authController) {
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
    danhSachTrinhXuLy.put(ActionType.REMOVE_AUTO_BID, auctionController);
    danhSachTrinhXuLy.put(ActionType.FOLLOW_AUCTION, auctionController);
    danhSachTrinhXuLy.put(ActionType.UNFOLLOW_AUCTION, auctionController);
    danhSachTrinhXuLy.put(ActionType.GET_FOLLOWED_AUCTIONS, auctionController);
    danhSachTrinhXuLy.put(ActionType.SEND_CHAT_MESSAGE, auctionController);
    danhSachTrinhXuLy.put(ActionType.GET_SYSTEM_NOTIFICATIONS, auctionController);
    danhSachTrinhXuLy.put(ActionType.MARK_SYSTEM_NOTIFICATIONS_READ, auctionController);
    danhSachTrinhXuLy.put("GET_WALLET_HISTORY", authController);
  }

  private void registerProductHandlers(ProductController productController) {
    danhSachTrinhXuLy.put(ActionType.CREATE_PRODUCT, productController);
    danhSachTrinhXuLy.put(ActionType.GET_ALL_PRODUCTS, productController);
    danhSachTrinhXuLy.put(ActionType.DELETE_PRODUCT, productController);
    danhSachTrinhXuLy.put(ActionType.SEARCH_PRODUCT, productController);
    danhSachTrinhXuLy.put(ActionType.GET_PRODUCT_BY_ID, productController);
    danhSachTrinhXuLy.put(ActionType.UPDATE_PRODUCT, productController);
    danhSachTrinhXuLy.put(ActionType.GET_MY_PRODUCTS, productController);
  }

  private void registerAdminHandlers(AdminController adminController) {
    danhSachTrinhXuLy.put(ActionType.ADMIN_GET_ALL_AUCTIONS, adminController);
    danhSachTrinhXuLy.put(ActionType.ADMIN_GET_PENDING_AUCTIONS, adminController);
    danhSachTrinhXuLy.put(ActionType.ADMIN_APPROVE_AUCTION, adminController);
    danhSachTrinhXuLy.put(ActionType.ADMIN_REJECT_AUCTION, adminController);
    danhSachTrinhXuLy.put(ActionType.ADMIN_GET_INVOICES, adminController);
    danhSachTrinhXuLy.put(ActionType.ADMIN_CHANGE_AUCTION_STATUS, adminController);
    danhSachTrinhXuLy.put(ActionType.ADMIN_DELETE_AUCTION, adminController);
    danhSachTrinhXuLy.put(ActionType.ADMIN_GET_TRANSACTIONS, adminController);
  }

  private String buildInternalError(JsonObject yeuCau) {
    JsonObject loiNoiBo = new JsonObject();
    loiNoiBo.addProperty("type", "ERROR_RESPONSE");
    loiNoiBo.addProperty("success", false);
    loiNoiBo.addProperty("errorCode", "ERR_HANDLER_NO_RESPONSE");
    loiNoiBo.addProperty("message", "Yêu cầu đã được nhận nhưng không có dữ liệu phản hồi.");
    if (yeuCau != null && yeuCau.has("requestId") && !yeuCau.get("requestId").isJsonNull()) {
      loiNoiBo.addProperty("requestId", yeuCau.get("requestId").getAsString());
    }
    return gson.toJson(loiNoiBo);
  }

  private String buildControllerNotFoundError() {
    JsonObject loi = new JsonObject();
    loi.addProperty("success", false);
    loi.addProperty("errorCode", "ERR_UNKNOWN");
    loi.addProperty("message", "Không tìm thấy controller xử lý cho yêu cầu này.");
    return gson.toJson(loi);
  }
}
