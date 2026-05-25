package com.auction.server.networkserver.handler;

import com.auction.common.dto.AuctionDTOs;
import com.auction.common.dto.BaseDTOs;
import com.auction.common.model.bid.Auction;
import com.auction.server.dao.FollowDao;
import com.auction.server.dao.AuctionDao;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

final class AuctionControllerUtil {
    private AuctionControllerUtil() {
    }

    static List<AuctionDTOs.AuctionSummaryDTO> buildAuctionSummaries(List<Auction> danhSachPhien) {
        List<AuctionDTOs.AuctionSummaryDTO> summaries = new ArrayList<>();
        for (Auction auction : danhSachPhien) {
            summaries.add(new AuctionDTOs.AuctionSummaryDTO(
                    auction.getId(),
                    auction.getItem().getName(),
                    auction.getCurrentHighestBid(),
                    auction.getStoredStatus().name(),
                    auction.getItem().getImageUrl(),
                    auction.getItem().getImageThumbUrl(),
                    auction.getStartTime() != null ? auction.getStartTime().toString() : null,
                    auction.getEndTime() != null ? auction.getEndTime().toString() : null,
                    auction.getItem().getCategory(),
                    auction.getItem().getDescription(),
                    auction.getItem().getStartingPrice(),
                    auction.getItem().getBidIncrement(),
                    auction.getBuyNowPrice(),
                    auction.isAntiSnipingEnabled(),
                    auction.getItem().getSellerId()
            ));
        }
        return summaries;
    }

    static JsonArray buildFollowedIdsArray(int userId) {
        JsonArray followedArray = new JsonArray();
        if (userId <= 0) {
            return followedArray;
        }
        List<Integer> followedIds = new FollowDao().getFollowedAuctionIds(userId);
        for (int id : followedIds) {
            followedArray.add(id);
        }
        return followedArray;
    }

    static void copyRequestId(JsonObject yeuCau, JsonObject phanHoi) {
        if (yeuCau != null && yeuCau.has("requestId") && !yeuCau.get("requestId").isJsonNull()) {
            phanHoi.addProperty("requestId", yeuCau.get("requestId").getAsString());
        }
    }

    static int getAuctionIdFromRequest(JsonObject yeuCau) {
        if (yeuCau == null || !yeuCau.has("auctionId") || yeuCau.get("auctionId").isJsonNull()) {
            return -1;
        }
        try {
            return yeuCau.get("auctionId").getAsInt();
        } catch (RuntimeException e) {
            return -1;
        }
    }

    static String buildError(Gson gson, JsonObject yeuCau, int statusCode, String message, String errorCode) {
        JsonObject loi = gson.toJsonTree(new BaseDTOs.ErrorResponse(statusCode, message, errorCode)).getAsJsonObject();
        loi.addProperty("type", "ERROR_RESPONSE");
        copyRequestId(yeuCau, loi);
        return gson.toJson(loi);
    }

    static boolean isAuctionSeller(AuctionDao auctionDao, int auctionId, int userId) {
        Auction phien = auctionDao.getAuctionById(auctionId);
        return phien != null && phien.getItem() != null && phien.getItem().getSellerId() == userId;
    }

    static JsonObject buildSimpleResponse(String type, boolean success, String message) {
        JsonObject phanHoi = new JsonObject();
        phanHoi.addProperty("type", type);
        phanHoi.addProperty("success", success);
        phanHoi.addProperty("message", message);
        return phanHoi;
    }
}
