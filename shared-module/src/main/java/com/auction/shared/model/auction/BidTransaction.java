package com.auction.shared.model.auction;

import com.auction.shared.model.Entity;
import com.auction.shared.model.user.Bidder;
import java.time.LocalDateTime;

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

    public Bidder getBidder() { return bidder; }
    public double getBidAmount() { return bidAmount; }
    public LocalDateTime getTimestamp() { return timestamp; }
}