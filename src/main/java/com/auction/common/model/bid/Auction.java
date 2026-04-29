package com.auction.common.model.bid;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.auction.auction.AuctionStatus;
import com.auction.common.exception.InvalidBidException;
import com.auction.common.model.entity.Entity;
import com.auction.common.model.item.Item;
import com.auction.common.model.user.Bidder;
import com.auction.common.observer.AuctionObserver;
import com.auction.server.dao.BidTransactionDAO;
import com.auction.server.utils.DatabaseConnection;

public class Auction extends Entity {
    private Item item;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private AuctionStatus status;
    private double currentHighestBid;
    private Bidder currentWinner;
    private final List<BidTransaction> bidHistory = new ArrayList<>();

    private BidTransactionDAO dao;
    private final List<AuctionObserver> observers = new CopyOnWriteArrayList<>();
    private transient final ExecutorService executor = Executors.newSingleThreadExecutor();
    private transient Future<String> timerFuture;

    public Auction(Item item) {
        this.item = item;
        this.currentHighestBid = item.getStartingPrice();
        this.status = AuctionStatus.OPEN;
        this.dao = new BidTransactionDAO(DatabaseConnection.getConnection());
    }

    /**
     * REFACTOR: tách lời gọi DB thành protected method.
     * Mục đích: AuctionTest override method này để bypass DB,
     * không cần mock framework, không cần kết nối mạng khi test.
     */
    protected boolean persistBid(int auctionId, BidTransaction tx) {
        if (dao == null) return false;
        return dao.saveTransaction(auctionId, tx);
    }

    /**
     * Xử lý đặt giá mới (Thread-safe).
     * synchronized đảm bảo tại 1 thời điểm chỉ 1 luồng cập nhật giá.
     */
    public synchronized boolean addValidBid(BidTransaction transaction) throws InvalidBidException {
        if (this.status != AuctionStatus.RUNNING) {
            throw new InvalidBidException("Phiên đấu giá đang không diễn ra!");
        }
        if (transaction.getBidAmount() <= this.currentHighestBid) {
            throw new InvalidBidException("Giá đặt phải cao hơn giá hiện tại: " + this.currentHighestBid);
        }

        if (persistBid(1, transaction)) {
            this.bidHistory.add(transaction);
            this.currentHighestBid = transaction.getBidAmount();
            this.currentWinner = transaction.getBidder();
            notifyNewBid(transaction);
            return true;
        } else {
            System.err.println("Lỗi: Không thể lưu lượt đặt giá vào Database!");
            return false;
        }
    }

    public void startAuction(int durationSeconds) {
        this.status = AuctionStatus.RUNNING;
        this.startTime = LocalDateTime.now();
        this.endTime = startTime.plusSeconds(durationSeconds);

        System.out.println(">>> PHIÊN ĐẤU GIÁ BẮT ĐẦU TRONG " + durationSeconds + " GIÂY!");

        timerFuture = executor.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                try {
                    Thread.sleep(durationSeconds * 1000L);
                } catch (InterruptedException e) {
                    return "Phiên bị gián đoạn.";
                }
                return endAuction();
            }
        });
    }

    private String endAuction() {
        this.status = AuctionStatus.FINISHED;
        System.out.println(">>> PHIÊN ĐẤU GIÁ KẾT THÚC!");
        if (executor != null && !executor.isShutdown()) executor.shutdown();
        if (this.currentWinner != null)
            return "Người thắng: " + this.currentWinner.getFullName() + " - $" + this.currentHighestBid;
        return "Không có ai đặt giá.";
    }

    public void addObserver(AuctionObserver observer) {
        observers.add(observer);
    }

    private void notifyNewBid(BidTransaction tx) {
        for (AuctionObserver observer : observers) {
            observer.onNewBid(tx);
        }
    }

    // Getters
    public AuctionStatus getStatus() { return status; }
    public double getCurrentHighestBid() { return currentHighestBid; }
    public Bidder getCurrentWinner() { return currentWinner; }
    public Item getItem() { return item; }
    public List<BidTransaction> getBidHistory() { return bidHistory; }
}
