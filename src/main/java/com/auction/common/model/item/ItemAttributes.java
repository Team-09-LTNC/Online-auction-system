package com.auction.common.model.item;

// Tạo một class chứa TẤT CẢ các thuộc tính của các sản phẩm, từ đó dễ truy cập vào constructor của mỗi loại sản phẩm hơn
// Về sau nếu có add thêm sản phẩm nào thì chỉ cần thêm thuộc tính vào class này
public class ItemAttributes {
    // Để public cũng được, vì các biến này chỉ đóng vai trò trung gian để ta gọi
    // thông qua constructor
    private String name;
    private String description;
    private double startingPrice;

    private String artist;
    private int yearCreated;
    private String medium;

    private String brand;
    private int warrantyMonths;

    private String make;
    private String model;
    private int year;

    // Getters/Setters

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public String getArtist() {
        return artist;
    }

    public int getYearCreated() {
        return yearCreated;
    }

    public String getMedium() {
        return medium;
    }

    public String getBrand() {
        return brand;
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setYearCreated(int yearCreated) {
        this.yearCreated = yearCreated;
    }

    public void setMedium(String medium) {
        this.medium = medium;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public void setWarrantyMonths(int warrantyMonths) {
        this.warrantyMonths = warrantyMonths;
    }

    public void setMake(String make) {
        this.make = make;
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

}
