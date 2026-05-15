package com.auction.common.dto;

public class BaseDTOs {
    // Lớp cha cho mọi yêu cầu
    public static class Request {
        public String type; // Để phân loại yêu cầu tại ClientHandler
        public String requestId; // Để theo dõi response tương ứng
    }

    public static class CreateAuctionRequest extends Request {
        public String productName;
        public String category;
        public String description;
        public double startPrice;
        public double increment;
        public String startTime;
        public String endTime;
        public boolean antiSniping;

        public CreateAuctionRequest() {
            this.type = "CREATE_AUCTION";
        }
    }

    // Lớp cha cho mọi phản hồi
    public static class Response {
        public String type; // Để phân loại phản hồi tại NetworkManager
        public boolean success;
        public String message;
        public String requestId; // Để trả về đúng response cho request đã nhận
    }

    // Thông báo lỗi chung
    public static class ErrorResponse {
        public String errorCode;
        public String errorMessage;
    }

    // Thông tin về một lượt đặt giá (dùng trong push cập nhật giá mới)
    public static class BidData {
        public long bidId;
        public long auctionId;
        public long bidderId;
        public String bidderName;
        public double amount;
        public String bidTime;
    }

    // =========================================================
    // SERVER PUSH – Server chủ động gửi về, không kèm requestId
    // =========================================================

    /** Push khi có người vừa đặt giá mới trong phiên */
    public static class AuctionBidUpdatePush extends Response {
        public long auctionId;
        public BidData latestBid; // thông tin lượt đặt giá mới nhất
        public double newHighestBid; // giá cao nhất hiện tại sau khi cập nhật

        // Không cần constructor gán type
        // vì đây là Response từ server, client chỉ nhận, không gửi
    }

    /** Push khi phiên đấu giá kết thúc */
    public static class AuctionResultPush extends Response {
        public long auctionId;
        public long winnerId;
        public String winnerName;
        public double finalPrice;
    }
}