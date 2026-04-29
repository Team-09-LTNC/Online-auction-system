package com.auction.common.model.item;

public class Vehicle extends Item {
    private String make; // Hãng xe (VD: Toyota, Ford)
    private String model; // Mẫu xe (VD: Camry, Mustang)
    private int year; // Năm sản xuất

    public Vehicle(String name, String description, double startingPrice, String make, String model, int year) {
        super(name, description, startingPrice);
        this.make = make;
        this.model = model;
        this.year = year;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    @Override
    public String getItemCategory() {
        return "VEHICLE";
    }
}