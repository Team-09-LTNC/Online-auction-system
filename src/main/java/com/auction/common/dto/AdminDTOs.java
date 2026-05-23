package com.auction.common.dto;

public class AdminDTOs {
    public static class UserSummaryDTO {
        private final String username;
        private final String fullname;
        private final String status;

        public UserSummaryDTO(String username, String fullname, String status) {
            this.username = username;
            this.fullname = fullname;
            this.status = status;
        }

        public String getUsername() {
            return username;
        }

        public String getFullname() {
            return fullname;
        }

        public String getStatus() {
            return status;
        }
    }

    public static class AuctionSummaryDTO {
        private final String itemName;
        private final String startTime;
        private final String endTime;
        private final String status; // OPEN, RUNNING, FINISHED, PAID, CANCELED
        private final String imageUrl;

        public AuctionSummaryDTO(String itemName, String startTime, String endTime, String status, String imageUrl) {
            this.itemName = itemName;
            this.startTime = startTime;
            this.endTime = endTime;
            this.status = status;
            this.imageUrl = imageUrl; // Placeholder, cần sửa lại sau
        }

        public String getItemName() {
            return itemName;
        }

        public String getStartTime() {
            return startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public String getStatus() {
            return status;
        }

        public String getImageUrl() {
            return imageUrl;
        }
    }

    public static class PendingAuctionDTO {
        private final int id;
        private final String itemName;
        private final int sellerId;
        private final String description;
        private final long startingPrice;
        private final String category;
        private final String startTime;
        private final String endTime;
        private final String imageUrl;

        public PendingAuctionDTO(int id, String itemName, int sellerId, String description, long startingPrice,
                String category, String startTime, String endTime, String imageUrl) {
            this.id = id;
            this.itemName = itemName;
            this.sellerId = sellerId;
            this.description = description;
            this.startingPrice = startingPrice;
            this.category = category;
            this.startTime = startTime;
            this.endTime = endTime;
            this.imageUrl = imageUrl; // Placeholder, cần sửa lại sau
        }

        public int getId() {
            return id;
        }

        public String getItemName() {
            return itemName;
        }

        public int getSellerId() {
            return sellerId;
        }

        public String getDescription() {
            return description;
        }

        public long getStartingPrice() {
            return startingPrice;
        }

        public String getCategory() {
            return category;
        }

        public String getStartTime() {
            return startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public String getImageUrl() {
            return imageUrl;
        }
    }

    public static class InvoiceDTO {
        private final int auctionId;
        private final int itemId;
        private final String itemName;
        private final int sellerId;
        private final int winnerId;
        private final long highestBid;

        public InvoiceDTO(int auctionId, int itemId, String itemName,
                int sellerId, int winnerId, long highestBid) {
            this.auctionId = auctionId;
            this.itemId = itemId;
            this.itemName = itemName;
            this.sellerId = sellerId;
            this.winnerId = winnerId;
            this.highestBid = highestBid;
        }

        public int getAuctionId() {
            return auctionId;
        }

        public int getItemId() {
            return itemId;
        }

        public String getItemName() {
            return itemName;
        }

        public int getSellerId() {
            return sellerId;
        }

        public int getWinnerId() {
            return winnerId;
        }

        public long getHighestBid() {
            return highestBid;
        }
    }
}
