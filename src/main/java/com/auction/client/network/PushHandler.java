// client/network/PushHandler.java
package com.auction.client.network;

import com.auction.common.dto.BaseDTOs;
import com.auction.common.enums.ActionType;
import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Xử lý các gói tin server tự gửi về (không kèm requestId)
 * Tách riêng khỏi NetworkManager để dễ mở rộng
 */
public class PushHandler {

    private static final Logger logger = LoggerFactory.getLogger(PushHandler.class);

    private static final Gson gson = new Gson();

    public static void handle(String type, String rawJson) {
        // 🔥 Chuyển sang check bằng chuỗi String chuẩn hoặc dùng Enum ActionType tương ứng với kiến trúc mới
        if ("AUCTION_BID_UPDATE".equals(type)) {
            BaseDTOs.AuctionBidUpdatePush push =
                    gson.fromJson(rawJson, BaseDTOs.AuctionBidUpdatePush.class);
            onBidUpdate(push);
        } else if ("AUCTION_RESULT".equals(type)) {
            BaseDTOs.AuctionResultPush push =
                    gson.fromJson(rawJson, BaseDTOs.AuctionResultPush.class);
            onAuctionResult(push);
        } else {
            logger.error("[PushHandler] Unknown push type: {}", type);
        }
    }

    private static void onBidUpdate(BaseDTOs.AuctionBidUpdatePush push) {
        // TODO: thông báo tới AuctionController/View đang mở
        // 🔥 SỬA TẠI ĐÂY: Chuyển sang gọi qua các hàm Getter chuẩn OOP của lớp BaseDTOs mới
        if (push != null && push.getLatestBid() != null) {
            logger.info("[Push] Giá mới: {} bởi {}", push.getNewHighestBid(), push.getLatestBid().getBidderName());
        }
    }

    private static void onAuctionResult(BaseDTOs.AuctionResultPush push) {
        // TODO: thông báo kết quả tới View
        // 🔥 SỬA TẠI ĐÂY: Chuyển sang gọi qua các hàm Getter chuẩn OOP của lớp BaseDTOs mới
        if (push != null) {
            logger.info("[Push] Phiên kết thúc! Người thắng: {} – Giá: {}", push.getWinnerName(), push.getFinalPrice());
        }
    }
}