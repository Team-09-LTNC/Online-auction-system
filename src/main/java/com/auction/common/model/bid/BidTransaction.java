package com.auction.common.model.bid;

import java.time.LocalDateTime;

import com.auction.common.model.entity.Entity;
import com.auction.common.model.user.Bidder;

// lưu lại lịch sử đặt giá
public class BidTransaction extends Entity {
    private Bidder bidder; // người đặt đấu giá
    private double bidAmount; // số tiền đặt giá
    private LocalDateTime timestamp; // thời gian

    public BidTransaction(Bidder bidder, double bidAmount) {
        this.bidder = bidder;
        this.bidAmount = bidAmount;
        this.timestamp = LocalDateTime.now();
    }

    public Bidder getBidder() {
        return bidder;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

}