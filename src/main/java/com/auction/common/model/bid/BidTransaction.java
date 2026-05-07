package com.auction.common.model.bid;

import java.time.LocalDateTime;
import com.auction.common.model.entity.Entity;
import com.auction.common.model.user.Bidder;

/**
 * Đại diện cho một bản ghi đặt giá trong hệ thống.
 * Tương ứng với bảng 'bid_history' trong Database.
 */
public class BidTransaction extends Entity {
    private int auctionId;   // xác định bid này của phiên nào
    private Bidder bidder;   // người đặt giá
    private long bidAmount;   //
    private LocalDateTime timestamp;

    public BidTransaction(int auctionId, Bidder bidder, long bidAmount) {
        super(); // id sẽ được set sau khi lấy từ DB hoặc tự tăng
        this.auctionId = auctionId;
        this.bidder = bidder;
        this.bidAmount = bidAmount;
        this.timestamp = LocalDateTime.now();
    }

    // Getters
    public int getAuctionId() { return auctionId; }
    public Bidder getBidder() { return bidder; }
    public long getBidAmount() { return bidAmount; }
    public LocalDateTime getTimestamp() { return timestamp; }
}