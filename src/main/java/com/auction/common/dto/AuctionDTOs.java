package com.auction.common.dto;
import com.auction.common.enums.StatusCode;

import java.util.List;

public class AuctionDTOs {

    // --- LẤY DANH SÁCH PHIÊN ---
    public static class AuctionListRequest extends BaseDTOs.Request {
        public AuctionListRequest() {
            super("AUCTION_LIST_REQUEST");
        }
    }

    public static class AuctionSummaryDTO {
        private final int auctionId;
        private final String itemName;
        private final long currentPrice;
        private final String status; // OPEN, RUNNING, FINISHED, PAID, CANCELED
        private final String imageUrl;
        private String startTime;
        private String endTime;
        private String category;
        private String description;
        private long startingPrice;
        private long bidIncrement;
        private Long buyNowPrice;
        private boolean antiSnipingEnabled;
        private int sellerId;

        public AuctionSummaryDTO(int auctionId, String itemName, long currentPrice, String status, String imageUrl, String startTime, String endTime, String category) {
            this(auctionId, itemName, currentPrice, status, imageUrl, startTime, endTime, category, null, 0, 0, null, false, -1);
        }

        public AuctionSummaryDTO(
                int auctionId,
                String itemName,
                long currentPrice,
                String status,
                String imageUrl,
                String startTime,
                String endTime,
                String category,
                String description,
                long startingPrice,
                long bidIncrement,
                Long buyNowPrice,
                boolean antiSnipingEnabled,
                int sellerId
        ) {
            this.auctionId = auctionId;
            this.itemName = itemName;
            this.currentPrice = currentPrice;
            this.status = status;
            this.imageUrl = imageUrl;
            this.startTime = startTime;
            this.endTime = endTime;
            this.category = category;
            this.description = description;
            this.startingPrice = startingPrice;
            this.bidIncrement = bidIncrement;
            this.buyNowPrice = buyNowPrice;
            this.antiSnipingEnabled = antiSnipingEnabled;
            this.sellerId = sellerId;
        }

        public int getAuctionId() { return auctionId; }
        public String getItemName() { return itemName; }
        public long getCurrentPrice() { return currentPrice; }
        public String getStatus() { return status; }
        public String getImageUrl() { return imageUrl; }
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getDescription() { return description; }
        public long getStartingPrice() { return startingPrice; }
        public long getBidIncrement() { return bidIncrement; }
        public Long getBuyNowPrice() { return buyNowPrice; }
        public boolean isAntiSnipingEnabled() { return antiSnipingEnabled; }
        public int getSellerId() { return sellerId; }
    }

    public static class AuctionListResponse extends BaseDTOs.Response {
        private final List<AuctionSummaryDTO> auctions;

        public AuctionListResponse(boolean success, String message, List<AuctionSummaryDTO> auctions) {
            super("AUCTION_LIST_RESPONSE", StatusCode.OK, success, message);
            this.auctions = auctions;
        }

        public List<AuctionSummaryDTO> getAuctions() { return auctions; }
    }

    // --- ĐẶT GIÁ ---
    public static class BidRequest extends BaseDTOs.Request {
        private final int auctionId;
        private final long bidAmount;
        private final int bidderId;

        public BidRequest(int auctionId, long bidAmount, int bidderId) {
            super("BID_REQUEST");
            this.auctionId = auctionId;
            this.bidAmount = bidAmount;
            this.bidderId = bidderId;
        }

        public int getAuctionId() { return auctionId; }
        public long getBidAmount() { return bidAmount; }
        public int getBidderId() { return bidderId; }
    }

    // --- CẬP NHẬT REAL-TIME (OBSERVER) ---
    // Gói tin này Server tự chủ động gửi xuống Client, không cần Request
    public static class AuctionUpdateDTO extends BaseDTOs.Response {
        private final int auctionId;
        private final long currentPrice;
        private final String highestBidderName;

        public AuctionUpdateDTO(int auctionId, long currentPrice, String highestBidderName) {
            super("NEW_BID_UPDATE", StatusCode.OK, true, "Có người đặt giá mới");            this.auctionId = auctionId;
            this.currentPrice = currentPrice;
            this.highestBidderName = highestBidderName;
        }

        public int getAuctionId() { return auctionId; }
        public long getCurrentPrice() { return currentPrice; }
        public String getHighestBidderName() { return highestBidderName; }
    }

    // --- LỊCH SỬ ĐẤU GIÁ (DÀNH CHO BIỂU ĐỒ) ---
    public static class BidPointDTO {
        private final long timestamp;
        private final long price;

        public BidPointDTO(long timestamp, long price) {
            this.timestamp = timestamp;
            this.price = price;
        }

        public long getTimestamp() { return timestamp; }
        public long getPrice() { return price; }
    }

    public static class BidHistoryResponse extends BaseDTOs.Response {
        private final int auctionId;
        private final List<BidPointDTO> historyLines;

        public BidHistoryResponse(boolean success, String message, int auctionId, List<BidPointDTO> historyLines) {
            super("BID_HISTORY_RESPONSE", StatusCode.OK, success, message);
            this.auctionId = auctionId;
            this.historyLines = historyLines;
        }

        public int getAuctionId() { return auctionId; }
        public List<BidPointDTO> getHistoryLines() { return historyLines; }
    }
}
