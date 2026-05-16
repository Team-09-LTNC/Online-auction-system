package com.auction.common.model.bid;

import java.time.LocalDateTime;
import java.util.*;

import com.auction.common.enums.AuctionStatus;
import com.auction.common.model.entity.Entity;
import com.auction.common.model.item.Item;
import com.auction.common.model.user.Bidder;

public class Auction extends Entity {
    private Item item;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private long currentHighestBid;
    private Bidder currentWinner;

    private final List<BidTransaction> bidHistory = new ArrayList<>();
    private final Queue<AutoBidConfig> autoBidders = new PriorityQueue<>();

    public Auction(Item item) {
        this.item = item;
        this.currentHighestBid = item.getStartingPrice();
        this.status = AuctionStatus.OPEN;
    }

    public void updateWinner(BidTransaction transaction) {
        this.bidHistory.add(transaction);
        this.currentHighestBid = transaction.getBidAmount();
        this.currentWinner = transaction.getBidder();
    }

    public boolean isAcceptingBids() {
        return this.status == AuctionStatus.RUNNING &&
                (this.endTime != null && LocalDateTime.now().isBefore(this.endTime));
    }

    public void addAutoBidConfig(AutoBidConfig config) { autoBidders.offer(config); }
    public Queue<AutoBidConfig> getAutoBidders() { return autoBidders; }

    /**
     * [TỐI ƯU KIẾN TRÚC - ANTI-SNIPING]
     * Ngăn chặn hành vi cộng dồn thời gian vô cực.
     * Chỉ gia hạn tính từ thời điểm HIỆN TẠI (LocalDateTime.now()).
     */
    public void extendEndTime(int extraSeconds) {
        LocalDateTime newEnd = LocalDateTime.now().plusSeconds(extraSeconds);
        // Chỉ kéo dài nếu thời gian mới thực sự muộn hơn thời gian kết thúc hiện tại
        if (this.endTime == null || newEnd.isAfter(this.endTime)) {
            this.endTime = newEnd;
        }
    }

    // --- Getters & Setters ---
    public Item getItem() { return item; }

    public long getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentPrice(long price) { this.currentHighestBid = price; }

    public Bidder getCurrentWinner() { return currentWinner; }
    public void setCurrentWinner(Bidder winner) { this.currentWinner = winner; }

    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public List<BidTransaction> getBidHistory() { return bidHistory; }
}