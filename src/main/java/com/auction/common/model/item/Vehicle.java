package com.auction.common.model.item;

public class Vehicle extends Item {
    private String make;
    private String model;
    private int year;

    public Vehicle() {}

    public Vehicle(String name, String description, long startingPrice, String make, String model, int year) {
        super(name, description, startingPrice);
        this.make = make;
        this.model = model;
        this.year = year;
    }

    public String getMake() { return make; }
    public String getModel() { return model; }
    public int getYear() { return year; }

    public void setMake(String make) { this.make = make; }
    public void setModel(String model) { this.model = model; }
    public void setYear(int year) { this.year = year; }

    @Override
    public String getItemCategory() {
        return "VEHICLE";
    }
}