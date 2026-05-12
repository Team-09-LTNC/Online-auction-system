package com.auction.common.model.item;

public class Electronics extends Item {
    private String brand;
    private int warrantyMonths;

    public Electronics() {}

    public Electronics(String name, String description, long startingPrice, String brand, int warrantyMonths) {
        super(name, description, startingPrice);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public String getItemCategory() {
        return "ELECTRONICS";
    }

    public String getBrand() { return brand; }
    public int getWarrantyMonths() { return warrantyMonths; }

    public void setBrand(String brand) { this.brand = brand; }
    public void setWarrantyMonths(int warrantyMonths) { this.warrantyMonths = warrantyMonths; }
}