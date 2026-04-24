package com.auction.common.model.item;

import com.auction.common.model.entity.Entity;

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

    
   
    public String getName() { 
        return name; 
    }

    public String getDescription() {
        return description;
    }
     public double getStartingPrice() { 
        return startingPrice; 
    }

}