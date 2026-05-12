package com.auction.common.model.item;

public class Art extends Item {
    private String artist;
    private int yearCreated;
    private String medium;

    public Art() {}

    public Art(String name, String description, long startingPrice, String artist, int yearCreated, String medium) {
        super(name, description, startingPrice);
        this.artist = artist;
        this.yearCreated = yearCreated;
        this.medium = medium;
    }

    public String getArtist() { return artist; }
    public int getYearCreated() { return yearCreated; }
    public String getMedium() { return medium; }

    public void setArtist(String artist) { this.artist = artist; }
    public void setYearCreated(int yearCreated) { this.yearCreated = yearCreated; }
    public void setMedium(String medium) { this.medium = medium; }

    @Override
    public String getItemCategory() {
        return "ART";
    }
}