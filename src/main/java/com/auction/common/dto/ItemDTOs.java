package com.auction.common.dto;

public class ItemDTOs {

    public static class CreateItemRequest extends BaseDTOs.Request {
        private final String name;
        private final String description;
        private final long startingPrice;
        private final String category;
        private final int sellerId;

        public CreateItemRequest(String name, String description, long startingPrice, String category, int sellerId) {
            super("CREATE_ITEM_REQUEST");
            this.name = name;
            this.description = description;
            this.startingPrice = startingPrice;
            this.category = category;
            this.sellerId = sellerId;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public long getStartingPrice() { return startingPrice; }
        public String getCategory() { return category; }
        public int getSellerId() { return sellerId; }
    }

    public static class CreateItemResponse extends BaseDTOs.Response {
        private final int itemId;

        public CreateItemResponse(boolean success, String message, int itemId) {
            super(success, message);
            this.itemId = itemId;
        }

        public int getItemId() { return itemId; }
    }
}