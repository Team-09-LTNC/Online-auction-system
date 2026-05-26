package com.auction.server.networkserver.handler;

import com.auction.common.dto.AuctionDTOs;
import com.auction.common.dto.BaseDTOs;
import com.auction.common.enums.ActionType;
import com.auction.common.enums.ErrorCode;
import com.auction.common.enums.StatusCode;
import com.auction.common.model.bid.Auction;
import com.auction.common.model.bid.BidLine;
import com.auction.common.model.user.User;
import com.auction.server.dao.AuctionDao;
import com.auction.server.dao.BidTransactionDao;
import com.auction.server.dao.FollowDao;
import com.auction.server.manager.AuctionManager;
import com.auction.server.networkserver.ClientHandler;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

final class AuctionQueryHandler {
    private final Gson gson;
    private final AuctionDao auctionDao;

    AuctionQueryHandler(Gson gson, AuctionDao auctionDao) {
        this.gson = gson;
        this.auctionDao = auctionDao;
    }

    String handleGetAuctionList(JsonObject yeuCau, ClientHandler client) {
        boolean featuredRunning = yeuCau.has("featuredRunning") && yeuCau.get("featuredRunning").getAsBoolean();
        List<Auction> danhSachPhien = featuredRunning
                ? auctionDao.getTopRunningAuctionsByBids()
                : auctionDao.getAllAuctionSessions();
        String categoryFilter = yeuCau.has("category") ? yeuCau.get("category").getAsString() : "ALL";
        if (danhSachPhien == null) {
            danhSachPhien = new ArrayList<>();
        }
        if (!"ALL".equalsIgnoreCase(categoryFilter) && !"Tat ca".equalsIgnoreCase(categoryFilter)) {
            danhSachPhien.removeIf(phien -> {
                String itemCategory = phien.getItem().getCategory();
                return itemCategory == null || !itemCategory.equalsIgnoreCase(categoryFilter);
            });
        }
        List<AuctionDTOs.AuctionSummaryDTO> summaries =
                AuctionControllerUtil.buildAuctionSummaries(danhSachPhien);
        AuctionDTOs.AuctionListResponse response =
                new AuctionDTOs.AuctionListResponse(true, "Lay danh sach thanh cong", summaries);
        JsonObject jsonResponse = gson.toJsonTree(response).getAsJsonObject();
        AuctionControllerUtil.copyRequestId(yeuCau, jsonResponse);
        jsonResponse.addProperty("serverNow", auctionDao.getDatabaseNow().toString());
        User user = client.getCurrentUser();
        jsonResponse.add("followedIds", AuctionControllerUtil.buildFollowedIdsArray(user != null ? user.getId() : -1));
        return gson.toJson(jsonResponse);
    }

    String handleGetJoinedAuctions(JsonObject yeuCau, ClientHandler client) {
        User nguoiDung = client.getCurrentUser();
        if (nguoiDung == null || !"BIDDER".equalsIgnoreCase(nguoiDung.getRoleName())) {
            return gson.toJson(new BaseDTOs.ErrorResponse(
                    StatusCode.UNAUTHORIZED, "Chua dang nhap!", ErrorCode.UNAUTHORIZED
            ));
        }
        List<Auction> danhSachPhien = auctionDao.getJoinedAuctions(nguoiDung.getId(), nguoiDung.getRoleName());
        List<AuctionDTOs.AuctionSummaryDTO> summaries =
                AuctionControllerUtil.buildAuctionSummaries(danhSachPhien);
        AuctionDTOs.AuctionListResponse response = new AuctionDTOs.AuctionListResponse(
                true, "Lay danh sach phien da tham gia thanh cong", summaries
        );
        JsonObject jsonResponse = gson.toJsonTree(response).getAsJsonObject();
        jsonResponse.addProperty("type", "JOINED_AUCTIONS_RESPONSE");
        AuctionControllerUtil.copyRequestId(yeuCau, jsonResponse);
        jsonResponse.addProperty("serverNow", auctionDao.getDatabaseNow().toString());
        jsonResponse.add("followedIds", AuctionControllerUtil.buildFollowedIdsArray(nguoiDung.getId()));
        return gson.toJson(jsonResponse);
    }

    String handleGetFollowedAuctions(JsonObject yeuCau, ClientHandler client) {
        User user = client.getCurrentUser();
        if (user == null) {
            return gson.toJson(new BaseDTOs.ErrorResponse(
                    StatusCode.UNAUTHORIZED, "Chua dang nhap", ErrorCode.UNAUTHORIZED
            ));
        }
        List<Integer> followedIds = new FollowDao().getFollowedAuctionIds(user.getId());
        List<Auction> danhSachPhien = auctionDao.getAllAuctionSessions();
        List<AuctionDTOs.AuctionSummaryDTO> summaries = new ArrayList<>();
        for (Auction auction : danhSachPhien) {
            if (followedIds.contains(auction.getId())) {
                summaries.addAll(AuctionControllerUtil.buildAuctionSummaries(List.of(auction)));
            }
        }
        AuctionDTOs.AuctionListResponse response =
                new AuctionDTOs.AuctionListResponse(true, "Thanh cong", summaries);
        JsonObject jsonResponse = gson.toJsonTree(response).getAsJsonObject();
        jsonResponse.addProperty("type", "FOLLOWED_AUCTIONS_RESPONSE");
        AuctionControllerUtil.copyRequestId(yeuCau, jsonResponse);
        jsonResponse.addProperty("serverNow", auctionDao.getDatabaseNow().toString());
        return gson.toJson(jsonResponse);
    }

    String handleGetAuctionById(JsonObject yeuCau, ClientHandler client) {
        int idPhien = AuctionControllerUtil.getAuctionIdFromRequest(yeuCau);
        if (idPhien <= 0) {
            return AuctionControllerUtil.buildError(
                    gson, yeuCau, StatusCode.BAD_REQUEST, "Thieu auctionId hop le.", ErrorCode.BAD_REQUEST
            );
        }
        Auction phien = AuctionManager.getInstance().getAuctionById(idPhien);
        if (phien == null) {
            phien = auctionDao.getAuctionById(idPhien);
        }
        JsonObject phanHoi = new JsonObject();
        AuctionControllerUtil.copyRequestId(yeuCau, phanHoi);
        if (phien != null) {
            phanHoi.addProperty("type", ActionType.GET_AUCTION_BY_ID);
            phanHoi.addProperty("success", true);
            JsonObject dataObj = gson.toJsonTree(phien).getAsJsonObject();
            if (phien.getCurrentWinner() != null) {
                JsonObject winnerObj = gson.toJsonTree(phien.getCurrentWinner()).getAsJsonObject();
                dataObj.add("currentWinner", winnerObj.has("bidder") ? winnerObj.get("bidder") : winnerObj);
            }
            dataObj.addProperty("currentHighestBid", phien.getCurrentHighestBid());
            dataObj.addProperty("currentPrice", phien.getCurrentHighestBid());
            dataObj.addProperty("antiSnipingEnabled", phien.isAntiSnipingEnabled());
            phanHoi.addProperty("serverNow", auctionDao.getDatabaseNow().toString());
            addUserAutoBidState(client, idPhien, dataObj);
            phanHoi.add("data", dataObj);
            return gson.toJson(phanHoi);
        }
        phanHoi.addProperty("type", "ERROR_RESPONSE");
        phanHoi.addProperty("success", false);
        phanHoi.addProperty("message", "Phien dau gia khong ton tai trong Database!");
        return gson.toJson(phanHoi);
    }

    String handleGetBidHistory(JsonObject yeuCau) {
        int idPhien = AuctionControllerUtil.getAuctionIdFromRequest(yeuCau);
        if (idPhien <= 0) {
            return AuctionControllerUtil.buildError(
                    gson, yeuCau, StatusCode.BAD_REQUEST, "Thieu auctionId hop le.", ErrorCode.BAD_REQUEST
            );
        }
        List<BidLine> lichSu = new BidTransactionDao().getAuctionBidHistory(idPhien);
        JsonObject phanHoi = new JsonObject();
        phanHoi.addProperty("type", ActionType.GET_BID_HISTORY);
        AuctionControllerUtil.copyRequestId(yeuCau, phanHoi);
        phanHoi.addProperty("success", true);
        phanHoi.add("data", gson.toJsonTree(lichSu));
        return gson.toJson(phanHoi);
    }

    String handleGetDashboardStats(JsonObject yeuCau, ClientHandler client) {
        int activeCount = auctionDao.countRunningAuctions();
        int joinedActiveCount = 0;
        int followedCount = 0;
        int myBidsCount = 0;
        User user = client.getCurrentUser();
        if (user != null && "BIDDER".equalsIgnoreCase(user.getRoleName())) {
            followedCount = new FollowDao().countFollowedAuctions(user.getId());
            myBidsCount = auctionDao.countJoinedAuctions(user.getId());
            joinedActiveCount = auctionDao.countActiveJoinedAuctions(user.getId());
        }
        JsonObject data = new JsonObject();
        data.addProperty("activeCount", activeCount);
        data.addProperty("joinedActiveCount", joinedActiveCount);
        data.addProperty("endingSoonCount", joinedActiveCount);
        data.addProperty("followedCount", followedCount);
        data.addProperty("myBidsCount", myBidsCount);
        JsonObject phanHoi = new JsonObject();
        phanHoi.addProperty("type", "DASHBOARD_STATS_RESPONSE");
        phanHoi.addProperty("success", true);
        AuctionControllerUtil.copyRequestId(yeuCau, phanHoi);
        phanHoi.add("data", data);
        return gson.toJson(phanHoi);
    }

    private void addUserAutoBidState(ClientHandler client, int auctionId, JsonObject dataObj) {
        User user = client.getCurrentUser();
        if (user == null) {
            return;
        }
        long userMaxAutoBid = auctionDao.getMaxAutoBid(auctionId, user.getId());
        if (userMaxAutoBid <= 0) {
            return;
        }
        dataObj.addProperty("userMaxAutoBid", userMaxAutoBid);
        long userAutoBidStep = auctionDao.getAutoBidStep(auctionId, user.getId());
        if (userAutoBidStep > 0) {
            dataObj.addProperty("userAutoBidStep", userAutoBidStep);
        }
    }
}
