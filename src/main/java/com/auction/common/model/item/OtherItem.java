package com.auction.common.model.item;
// sản phẩm (người dùng tự nhập)
public class OtherItem extends Item {
    private String customCategoryName; // Tên loại do người dùng tự nhập (VD: "Thời trang")

    public OtherItem() {}

    public OtherItem(String name, String description, long startingPrice, String customCategoryName) {
        super(name, description, startingPrice);
        this.customCategoryName = customCategoryName;
    }

    public OtherItem(String name, String imageUrl) {
        super(name, "", 0);
        this.imageUrl = imageUrl;
    }       

    public String getCustomCategoryName() { return customCategoryName; }
    public void setCustomCategoryName(String customCategoryName) { this.customCategoryName = customCategoryName; }

    @Override
    public String getItemCategory() {
        return "OTHER";
    }
}