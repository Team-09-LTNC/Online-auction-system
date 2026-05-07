package com.auction.common.model.bid;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.auction.common.model.AuctionStatus;
import com.auction.common.exception.InvalidBidException;
import com.auction.common.model.entity.Entity;
import com.auction.common.model.item.Item;
import com.auction.common.model.user.Bidder;
import com.auction.common.observer.AuctionObserver;

/**
 *Lưu trữ trạng thái của một phiên đấu giá và xử lý logic kiểm tra giá hợp lệ.
 */
public class Auction extends Entity {
    private Item item;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private AuctionStatus status;
    private long currentHighestBid;
    private Bidder currentWinner;

    // Lưu trữ lịch sử đấu giá trong bộ nhớ ram của máy chủ
    private final List<BidTransaction> bidHistory = new ArrayList<>();
    private transient final List<AuctionObserver> observers = new CopyOnWriteArrayList<>();

    public Auction(Item item) {
        this.item = item;
        this.currentHighestBid = item.getStartingPrice();
        this.status = AuctionStatus.OPEN;
    }

    /**
     * Xử lý logic đặt giá của người dùng.
     */
    public boolean processBidLogic(BidTransaction transaction) throws InvalidBidException {
        // Biến tạm để cất giao dịch thành công nhằm thông báo sau
        boolean isSuccess = false;

        // Áp dụng 'synchronized' để giải quyết đấu giá đồng thời
        synchronized (this) {
            if (this.status != AuctionStatus.RUNNING) {
                throw new InvalidBidException("Phiên đấu giá đang không diễn ra!");
            }
            if (transaction.getBidAmount() <= this.currentHighestBid) {
                throw new InvalidBidException("Giá đặt phải cao hơn: " + this.currentHighestBid);
            }

            // Nếu logic đúng,cập nhật dữ liệu ngay lập tức trên RAM
            this.bidHistory.add(transaction);
            this.currentHighestBid = transaction.getBidAmount();
            this.currentWinner = transaction.getBidder();
            isSuccess = true;
        }

        if (isSuccess) {
            notifyNewBid(transaction);
        }

        return isSuccess;
    }

    /**
     * Đăng ký một Observer (người quan sát) để nhận thông báo realtime.
     */
    public void addObserver(AuctionObserver observer) {
        observers.add(observer);
    }

    /**
     * Gửi thông báo đến tất cả các Client đang theo dõi phiên đấu giá này.
     */
    private void notifyNewBid(BidTransaction tx) {
        for (AuctionObserver observer : observers) {
            observer.onNewBid(tx);
        }
    }

    //  Setters để AuctionManager sử dụng
    // Manager sẽ dùng các hàm này để thay đổi vòng đời phiên đấu giá theo thời gian thực

    public void setStatus(AuctionStatus status) { this.status = status; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public LocalDateTime getEndTime() { return endTime; }

    // Getters cơ bản để lấy thông tin hiển thị lên Client (JavaFX)

    public AuctionStatus getStatus() { return status; }
    public long getCurrentHighestBid() { return currentHighestBid; }
    public Bidder getCurrentWinner() { return currentWinner; }
    public Item getItem() { return item; }
    public List<BidTransaction> getBidHistory() { return bidHistory; }
}