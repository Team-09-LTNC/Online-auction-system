package com.auction.common.dto;
import com.auction.common.enums.StatusCode;

public class ItemDTOs {

    public static class CreateItemRequest extends BaseDTOs.Request {
        private final String name;
        private final String description;
        private final long startingPrice;
        private final String category;
        private final int sellerId;
        private final String imageUrl;
        private final String imageThumbUrl;
        private final String startTime;
        private final String endTime;

        public CreateItemRequest(String name, String description, long startingPrice, String category, int sellerId, String imageUrl, String startTime, String endTime) {
            this(name, description, startingPrice, category, sellerId, imageUrl, null, startTime, endTime);
        }

        public CreateItemRequest(String name, String description, long startingPrice, String category, int sellerId, String imageUrl, String imageThumbUrl, String startTime, String endTime) {
            super("CREATE_ITEM_REQUEST");
            this.name = name;
            this.description = description;
            this.startingPrice = startingPrice;
            this.category = category;
            this.sellerId = sellerId;
            this.imageUrl = imageUrl;
            this.imageThumbUrl = imageThumbUrl;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public long getStartingPrice() { return startingPrice; }
        public String getCategory() { return category; }
        public int getSellerId() { return sellerId; }
        public String getImageUrl() { return imageUrl; }
        public String getImageThumbUrl() { return imageThumbUrl; }
        public String getStartTime() { return startTime; }
        public String getEndTime() { return endTime; }
    }

    public static class CreateItemResponse extends BaseDTOs.Response {
        private final int itemId;

        public CreateItemResponse(boolean success, String message, int itemId) {
            super("CREATE_ITEM_RESPONSE", StatusCode.CREATED, success, message);
            this.itemId = itemId;
        }

        public int getItemId() { return itemId; }
    }
}
