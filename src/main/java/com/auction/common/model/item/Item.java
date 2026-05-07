package com.auction.common.model.item;

import com.auction.common.model.entity.Entity;

public abstract class Item extends Entity {
    protected String name;
    protected String description;
    protected long startingPrice;
    protected int sellerId;
    protected String category;
    protected String imageUrl;

    public Item() {}

    public Item(String name, String description, long startingPrice) {
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
    }

    public abstract String getItemCategory();

    public String getName() { return name; }
    public String getDescription() { return description; }
    public long getStartingPrice() { return startingPrice; }
    public int getSellerId() { return sellerId; }
    public String getCategory() { return category; }
    public String getImageUrl() { return imageUrl; }

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setStartingPrice(long startingPrice) { this.startingPrice = startingPrice; }
    public void setSellerId(int sellerId) { this.sellerId = sellerId; }
    public void setCategory(String category) { this.category = category; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

}