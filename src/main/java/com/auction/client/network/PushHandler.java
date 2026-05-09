// client/network/PushHandler.java
package com.auction.client.network;

import com.auction.common.dto.BaseDTOs;
import com.auction.common.enums.ActionType;
import com.google.gson.Gson;

/**
 * Xử lý các gói tin server tự gửi về (không kèm requestId)
 * Tách riêng khỏi NetworkManager để dễ mở rộng
 */
public class PushHandler {

    private static final Gson gson = new Gson();

    public static void handle(String type, String rawJson) {
        switch (type) {

            case ActionType.AUCTION_BID_UPDATE -> {
                BaseDTOs.AuctionBidUpdatePush push =
                    gson.fromJson(rawJson, BaseDTOs.AuctionBidUpdatePush.class);
                onBidUpdate(push);
            }

            case ActionType.AUCTION_RESULT -> {
                BaseDTOs.AuctionResultPush push =
                    gson.fromJson(rawJson, BaseDTOs.AuctionResultPush.class);
                onAuctionResult(push);
            }

            default -> System.err.println("[PushHandler] Unknown push type: " + type);
        }
    }

    private static void onBidUpdate(BaseDTOs.AuctionBidUpdatePush push) {
        // TODO: thông báo tới AuctionController/View đang mở
        System.out.println("[Push] Giá mới: " + push.newHighestBid
                         + " bởi " + push.latestBid.bidderName);
    }

    private static void onAuctionResult(BaseDTOs.AuctionResultPush push) {
        // TODO: thông báo kết quả tới View
        System.out.println("[Push] Phiên kết thúc! Người thắng: " + push.winnerName
                         + " – Giá: " + push.finalPrice);
    }
}