package com.auction.common.model.bid;

import java.time.LocalDateTime;
import com.auction.common.model.entity.Entity;
import com.auction.common.model.user.Bidder;

/**
 * Đại diện cho một bản ghi đặt giá trong hệ thống.
 * Tương ứng với bảng 'bid_history' trong cơ sở dữ liệu.
 */
public class BidTransaction extends Entity {
    private int auctionId;   // xác định lượt đặt giá này của phiên nào
    private Bidder bidder;   // người đặt giá
    private long bidAmount;   //
    private LocalDateTime timestamp;

    public BidTransaction(int auctionId, Bidder bidder, long bidAmount, LocalDateTime now) {
        super(); // id sẽ được set sau khi lấy từ DB hoặc tự tăng
        this.auctionId = auctionId;
        this.bidder = bidder;
        this.bidAmount = bidAmount;
        this.timestamp = LocalDateTime.now();
    }

    // Phương thức lấy giá trị
    public int getAuctionId() { return auctionId; }
    public Bidder getBidder() { return bidder; }
    public long getBidAmount() { return bidAmount; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
