package com.auction.model.item;

public class Art extends Item {
    private String artist; // Tác giả
    private int yearCreated; // Năm sáng tác
    private String medium; // Chất liệu (VD: Sơn dầu, Điêu khắc)

    public Art(String name, String description, double startingPrice, String artist, int yearCreated, String medium) {
        super(name, description, startingPrice);
        this.artist = artist;
        this.yearCreated = yearCreated;
        this.medium = medium;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public int getYearCreated() {
        return yearCreated;
    }

    public void setYearCreated(int yearCreated) {
        this.yearCreated = yearCreated;
    }

    public String getMedium() {
        return medium;
    }

    public void setMedium(String medium) {
        this.medium = medium;
    }

    @Override
    public String getItemCategory() {
        return "ART";
    }
}