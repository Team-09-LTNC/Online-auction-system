package com.auction.common.model.item;

import com.auction.common.model.entity.Entity;

public abstract class Item extends Entity {
    protected String name;
    protected String description;
    protected long startingPrice;
    protected long bidIncrement;
    protected int sellerId;
    protected String category;
    protected String imageUrl;
    protected String imageThumbUrl;
    private String startTime;
    private String endTime;


    public Item() {}

    public Item(String name, String description, long startingPrice) {
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
    }

    /**
     * Hàm khởi tạo item để hiển thị lên trang duyệt của admin.
     */
    public Item(String name, int sellerId, String description, long startingPrice) {
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.sellerId = sellerId;
    }

    public abstract String getItemCategory();

    public String getName() { return name; }
    public String getDescription() { return description; }
    public long getStartingPrice() { return startingPrice; }
    public long getBidIncrement() { return bidIncrement; }
    public int getSellerId() { return sellerId; }
    public String getCategory() { return category; }
    public String getImageUrl() { return imageUrl; }
    public String getImageThumbUrl() { return imageThumbUrl; }
    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setStartingPrice(long startingPrice) { this.startingPrice = startingPrice; }
    public void setBidIncrement(long bidIncrement) { this.bidIncrement = bidIncrement; }
    public void setSellerId(int sellerId) { this.sellerId = sellerId; }
    public void setCategory(String category) { this.category = category; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setImageThumbUrl(String imageThumbUrl) { this.imageThumbUrl = imageThumbUrl; }

}
