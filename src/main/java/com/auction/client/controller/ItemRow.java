package com.auction.client.controller;

/**
 * Model cho từng hàng trong bảng sản phẩm (TableView)
 */
public class ItemRow {
    private int dbId;
    private String name;
    private String category;
    private String startingPrice;
    private String status;

    public ItemRow(int dbId, String name, String category, String startingPrice, String status) {
        this.dbId = dbId;
        this.name = name;
        this.category = category;
        this.startingPrice = startingPrice;
        this.status = status;
    }

    public int getDbId() { return dbId; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getStartingPrice() { return startingPrice; }
    public String getStatus() { return status; }
}
