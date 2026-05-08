package com.auction.common.dto;

public class ItemDTOs {
    public static class CreateItemRequest {
        public String name;
        public String description;
        public long startingPrice;
        public String category;
        public int sellerId;
    }

    public static class CreateItemResponse {
        public boolean success;
        public String message;
        public int itemId;
    }
}