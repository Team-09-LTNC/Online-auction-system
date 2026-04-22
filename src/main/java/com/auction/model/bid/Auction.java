package com.auction.model.bid;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.auction.auction.AuctionStatus;
import com.auction.exception.AuctionClosedException;
import com.auction.exception.InvalidBidException;
import com.auction.model.entity.Entity;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.observer.AuctionObserver;
import com.auction.repository.BidTransactionDAO;
import com.auction.utils.DatabaseConnection;

public class Auction extends Entity {
    private Item item;
    private Seller seller;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private AuctionStatus status;
    private double currentHighestBid;
    private Bidder currentWinner;
    private List<BidTransaction> bidHistory = new ArrayList<>();

    private final List<AuctionObserver> observers = new CopyOnWriteArrayList<>();

    private transient final ExecutorService executor = Executors.newSingleThreadExecutor();
    private transient Future<String> timerFuture;

    public Auction(Item item, Seller seller2, LocalDateTime localDateTime, LocalDateTime localDateTime2) {
        this.item = item;
        this.seller = seller;
        this.startTime = startTime;
        this.endTime = endTime;

        this.status = AuctionStatus.OPEN;
        this.currentHighestBid = item.getStartingPrice();
        this.bidHistory = new ArrayList<>();
    }

    public Auction(com.auction.model.entity.Item item2) {
        // TODO Auto-generated constructor stub
    }

    public void addObserver(AuctionObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }

    private void notifyNewBid(BidTransaction bid) {
        for (AuctionObserver observer : observers) {
            observer.onNewBid(bid);
        }
    }

    private void notifyStatusChanged(AuctionStatus newStatus) {
        for (AuctionObserver observer : observers) {
            observer.onStatusChanged(newStatus);
        }
    }

    // synchronized: khóa lại không cho nhiều người đặt giá và cùng xử lý chúng một
    // lúc
    public synchronized boolean addValidBid(BidTransaction transaction) throws InvalidBidException {
        if (this.status != AuctionStatus.RUNNING) {
            throw new InvalidBidException("Phiên đấu giá đang không chạy");

        }
        if (transaction.getBidAmount() <= this.currentHighestBid) {
            throw new InvalidBidException("Không thể đặt giá thấp hơn hoặc bằng giá cao nhất hiện tại");
        }
        this.bidHistory.add(transaction);
        this.currentHighestBid = transaction.getBidAmount();
        this.currentWinner = transaction.getBidder();
        dao.saveTransaction(1, transaction);
        System.out.println(
                "✅ [DB] Đã lưu: " + transaction.getBidder().getUsername() + " -> $" + transaction.getBidAmount());
        notifyNewBid(transaction);
        return true;

    }

    public void startAuction(int durationSeconds) {
        this.updateStatus(AuctionStatus.RUNNING);
        Callable<String> timer = () -> {
            try {
                Thread.sleep(durationSeconds * 1000L);
            } catch (InterruptedException e) {
                this.updateStatus(AuctionStatus.CANCELED);
                return "Phiên đấu giá bị hủy giữa chừng bởi ADMIN";
            }
            return this.closeAuction();
        };

        this.timerFuture = executor.submit(timer);
    }

    // synchronized: Đảm bảo không ai đặt giá được ngay lúc luồng timer đang đóng
    // cửa phiên
    public synchronized String closeAuction() throws AuctionClosedException {
        if (this.status == AuctionStatus.FINISHED || this.status == AuctionStatus.CANCELED) {
            throw new AuctionClosedException("Phiên đấu giá đã kết thúc trước đó.");
        }

        this.updateStatus(AuctionStatus.FINISHED);

        if (this.currentWinner != null) {
            return "Người chiến thắng là: " + this.currentWinner.getFullName() +
                    " với giá " + this.currentHighestBid;
        }
        return "Không có ai đặt giá cho phiên này.";
    }

    private BidTransactionDAO dao = new BidTransactionDAO(DatabaseConnection.getConnection());

    public void updateStatus(AuctionStatus newStatus) {
        this.status = newStatus;
        notifyStatusChanged(newStatus);
    }

    public AuctionStatus getStatus() {
        return status;
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
}