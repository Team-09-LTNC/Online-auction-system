package com.auction.shared.model.item;

// Sản phẩm đồ điện tử
public class Electronics extends Item {
    private String brand; // nhãn hàng
    private int warrantyMonths; // hạn sử dụng

    public Electronics(String name, String description, double startingPrice, String brand, int warrantyMonths) {
        super(name, description, startingPrice);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public String getItemCategory() {
        return "ELECTRONICS";
    }
}