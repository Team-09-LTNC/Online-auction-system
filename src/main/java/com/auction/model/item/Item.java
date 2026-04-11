package com.auction.model.item;

import com.auction.model.entity.Entity;

// thông tin chung của sản phẩm
public abstract class Item extends Entity {
    protected String name;
    protected String description;
    protected double startingPrice; // Giá khởi điểm

    public Item(String name, String description, double startingPrice) {
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
    }

    // Phân loại sản phẩm
    public abstract String getItemCategory();

    public double getStartingPrice() { return startingPrice; }
    public String getName() { return name; }
}