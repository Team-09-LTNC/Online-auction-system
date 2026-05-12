package com.auction.common.model.bid; // Nên đưa vào package dto

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * DTO (Data Transfer Object) dùng để gửi dữ liệu vẽ biểu đồ về Client.
 */
public class BidLine implements Serializable {
    private String bidderName; // Tên bidder
    private long bidAmount;     // Số tiền
    private Timestamp bidTime; // Thời điểm

    public BidLine() {
    }

    public BidLine(String bidderName, long bidAmount, Timestamp bidTime) {
        this.bidderName = bidderName;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
    }

    // Getters & Setters (dùng cho GSON)
    public String getBidderName() {
        return bidderName;
    }

    public void setBidderName(String bidderName) {
        this.bidderName = bidderName;
    }

    public long getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(long bidAmount) {
        this.bidAmount = bidAmount;
    }

    public Timestamp getBidTime() {
        return bidTime;
    }

    public void setBidTime(Timestamp bidTime) {
        this.bidTime = bidTime;
    }
}