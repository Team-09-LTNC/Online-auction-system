package com.auction.server.networkserver.handler;

import com.auction.common.dto.BaseDTOs;
import com.auction.common.enums.ActionType;
import com.auction.common.enums.ErrorCode;
import com.auction.common.enums.StatusCode;
import com.auction.common.util.GsonConfig;
import com.auction.server.dao.AuctionDao;
import com.auction.server.networkserver.ClientHandler;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class AuctionController implements RequestHandler {
    private final Gson gson = GsonConfig.getInstance();
    private final AuctionDao auctionDao = new AuctionDao();
    private final AuctionCommandHandler commandHandler = new AuctionCommandHandler(gson, auctionDao);
    private final AuctionQueryHandler queryHandler = new AuctionQueryHandler(gson, auctionDao);

    @Override
    public String handleRequest(JsonObject yeuCau, ClientHandler client) {
        if (yeuCau == null || !yeuCau.has("type") || yeuCau.get("type").isJsonNull()) {
            return AuctionControllerUtil.buildError(
                    gson, yeuCau, StatusCode.BAD_REQUEST, "Thieu truong type.", ErrorCode.BAD_REQUEST
            );
        }
        String loaiYeuCau = yeuCau.get("type").getAsString();
        switch (loaiYeuCau) {
            case ActionType.JOIN_AUCTION:
                return commandHandler.handleJoinAuction(yeuCau, client);
            case ActionType.PLACE_BID:
                return commandHandler.handlePlaceBid(yeuCau, client);
            case ActionType.CONFIRM_BUY_NOW:
                return commandHandler.handleBuyNow(yeuCau, client);
            case ActionType.CREATE_AUCTION:
                return gson.toJson(new BaseDTOs.ErrorResponse(
                        StatusCode.BAD_REQUEST,
                        "Action CREATE_AUCTION khong duoc ho tro truc tiep. Hay dung CREATE_PRODUCT de tao phien.",
                        ErrorCode.BAD_REQUEST
                ));
            case ActionType.SETTLE_BUY_NOW:
                return commandHandler.handleSettleBuyNow(yeuCau, client);
            case ActionType.REGISTER_AUTO_BID:
                return commandHandler.handleRegisterAutoBid(yeuCau, client);
            case ActionType.REMOVE_AUTO_BID:
                return commandHandler.handleRemoveAutoBid(yeuCau, client);
            case ActionType.LEAVE_AUCTION:
                return commandHandler.handleLeaveAuction(yeuCau, client);
            case ActionType.CLOSE_AUCTION:
                return commandHandler.handleCloseAuction(yeuCau, client);
            case ActionType.GET_ALL_AUCTIONS:
                return queryHandler.handleGetAuctionList(yeuCau, client);
            case ActionType.GET_JOINED_AUCTIONS:
                return queryHandler.handleGetJoinedAuctions(yeuCau, client);
            case ActionType.GET_AUCTION_BY_ID:
                return queryHandler.handleGetAuctionById(yeuCau, client);
            case ActionType.GET_BID_HISTORY:
                return queryHandler.handleGetBidHistory(yeuCau);
            case ActionType.GET_DASHBOARD_STATS:
                return queryHandler.handleGetDashboardStats(yeuCau, client);
            case ActionType.GET_FOLLOWED_AUCTIONS:
                return queryHandler.handleGetFollowedAuctions(yeuCau, client);
            case ActionType.FOLLOW_AUCTION:
                return AuctionMiscHandler.handleFollow(gson, yeuCau, client, true);
            case ActionType.UNFOLLOW_AUCTION:
                return AuctionMiscHandler.handleFollow(gson, yeuCau, client, false);
            case ActionType.SEND_CHAT_MESSAGE:
                return AuctionMiscHandler.handleChat(gson, yeuCau, client);
            case ActionType.GET_SYSTEM_NOTIFICATIONS:
                return AuctionMiscHandler.handleGetSystemNotifications(gson, yeuCau, client);
            case ActionType.MARK_SYSTEM_NOTIFICATIONS_READ:
                return AuctionMiscHandler.handleMarkNotificationsRead(gson, yeuCau, client);
            default:
                return AuctionControllerUtil.buildError(
                        gson, yeuCau, StatusCode.BAD_REQUEST, "Action khong duoc ho tro.", ErrorCode.BAD_REQUEST
                );
        }
    }
}
