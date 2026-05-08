package com.auction.common.dto;

import java.util.List;

public class AuctionDTOs {
    // Lấy danh sách phiên
    public static class AuctionListRequest {}

    public static class AuctionListResponse {
        public List<Object> auctions; // Danh sách các phiên đấu giá
    }

    // Đặt giá
    public static class BidRequest {
        public int auctionId;
        public long bidAmount;
        public int bidderId;
    }

    // Cập nhật Real-time (Observer)
    public static class AuctionUpdateDTO {
        public int auctionId;
        public long currentPrice;
        public String highestBidderName;
    }

    // Lịch sử đấu giá (để vẽ biểu đồ)
    public static class BidHistoryResponse {
        public int auctionId;
        public List<Object> historyLines;
    }
}