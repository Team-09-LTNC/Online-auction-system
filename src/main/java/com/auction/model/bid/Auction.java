package com.auction.model.bid;

import com.auction.model.entity.Entity;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.auction.AuctionStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Dữ liệu của phòng đấu giá
public class Auction extends Entity {
    // thông tin của phiên đấu giá( sản phẩm, người bán, thời gian đấu)
    private Item item;
    private Seller seller;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // cập nhật dữ liệu khi có thay đổi
    private AuctionStatus status;
    private double currentHighestBid;   // giá cao nhất hiện tại
    private Bidder currentWinner;       // người đang trả cao nhất
    private List<BidTransaction> bidHistory; // lịch sử trả giá

    public Auction(Item item, Seller seller, LocalDateTime startTime, LocalDateTime endTime) {
        this.item = item;
        this.seller = seller;
        this.startTime = startTime;
        this.endTime = endTime;

        this.status = AuctionStatus.OPEN;
        this.currentHighestBid = item.getStartingPrice();
        this.bidHistory = new ArrayList<>();
    }

    // câp nhật người đặt giá cao nhất ( chiến thắng tạm thời)
    public void addValidBid(BidTransaction transaction) {
        this.bidHistory.add(transaction);
        this.currentHighestBid = transaction.getBidAmount();
        this.currentWinner = transaction.getBidder();
    }

    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }

    public double getCurrentHighestBid() { return currentHighestBid; }
    public Bidder getCurrentWinner() { return currentWinner; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}