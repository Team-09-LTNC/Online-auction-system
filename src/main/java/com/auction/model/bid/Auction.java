package com.auction.model.bid;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.auction.auction.AuctionStatus;
import com.auction.model.entity.Entity;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;

// Dữ liệu của phòng đấu giá
public class Auction extends Entity {
    // thông tin của phiên đấu giá( sản phẩm, người bán, thời gian đấu)
    private Item item;
    private Seller seller;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // cập nhật dữ liệu khi có thay đổi
    private AuctionStatus status;
    private double currentHighestBid; // giá cao nhất hiện tại
    private Bidder currentWinner; // người đang trả cao nhất
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
    public synchronized boolean addValidBid(BidTransaction transaction) {
        if (this.status != AuctionStatus.RUNNING) {
            System.out.println("Phiên đấu giá không hoạt động");
            return false;
        }
        if (this.currentHighestBid < transaction.getBidAmount()) {
            this.bidHistory.add(transaction);
            this.currentHighestBid = transaction.getBidAmount();
            this.currentWinner = transaction.getBidder();
            System.out.println("Đặt giá thành công!");
            return true;
        }
        return false;
    }

    // SingleThread vì chỉ cần 1 luồng đếm giờ cho mỗi phiên
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Tờ biên lai (Future) để sau này lôi ra kiểm tra hoặc hủy phiên đấu giá giữa
    // chừng
    private Future<String> timerFuture;

    public synchronized String closeAuction() {
        if (this.status == AuctionStatus.FINISHED) {
            return "Phiên đấu giá đã kết thúc";
        }

        if (this.currentWinner != null) {
            return "Người chiến thắng là: " + this.currentWinner + "\n Với giá là: " + this.currentHighestBid;
        }
        return "Không có ai đặt giá cho phiên này";
    }

    // Bắt đầu phiên đấu giá
    public void startAuction(int durationSeconds) { // Thời gian phiên đấu giá
        this.status = AuctionStatus.RUNNING; // Khởi tạo trạng thái cho phiên đấu giá
        Callable<String> timer = () -> {
            try {
                Thread.sleep(durationSeconds * 1000L); // Cho luồng nghỉ trong thời gian đấu giá

            } catch (InterruptedException e) {
                return "Phiên đấu giá bị hủy bởi ADMIN";
            }

            return this.closeAuction(); // Khi luồng "dậy" thì đóng phiên đấu giá lại

        };

        this.timerFuture = executor.submit(timer); // Thêm task vào Future
        executor.shutdown();
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public double getCurrentHighestBid() {
        return currentHighestBid;
    }

    public Bidder getCurrentWinner() {
        return currentWinner;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Seller getSeller() {
        return seller;
    }

    public void setSeller(Seller seller) {
        this.seller = seller;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public List<BidTransaction> getBidHistory() {
        return bidHistory;
    }

    public Future<String> getTimerFuture() {
        return timerFuture;
    }

    public void setTimerFuture(Future<String> timerFuture) {
        this.timerFuture = timerFuture;
    }

}