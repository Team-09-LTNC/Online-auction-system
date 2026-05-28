package com.auction.common.model.item;
// sản phẩm khác
public class OtherItem extends Item {
    private String customCategoryName; 

    public OtherItem() {}

    public OtherItem(String name, String description, long startingPrice, String customCategoryName) {
        super(name, description, startingPrice);
        this.customCategoryName = customCategoryName;
    }

    public OtherItem(String name, String imageUrl) {
        super(name, "", 0);
        this.imageUrl = imageUrl;
    }
    
    public OtherItem(String name, int sellerId, String description, long startingPrice, String customCategoryName, String imageUrl) {
        super(name, description, startingPrice);
        this.customCategoryName = customCategoryName;
        this.imageUrl = imageUrl;
        this.sellerId = sellerId;
    }

    public String getCustomCategoryName() { return customCategoryName; }
    public void setCustomCategoryName(String customCategoryName) { this.customCategoryName = customCategoryName; }

    @Override
    public String getItemCategory() {
        return "OTHER";
    }
}