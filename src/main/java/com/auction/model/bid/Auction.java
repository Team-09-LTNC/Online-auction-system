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
import com.auction.exception.InvalidBidException;
import com.auction.model.entity.Entity;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.observer.AuctionObserver;
import com.auction.dao.BidTransactionDAO;
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

    // Quan trọng: Đối tượng DAO để làm việc với Database
    private BidTransactionDAO dao;

    // Sử dụng CopyOnWriteArrayList để an toàn khi thêm/xóa observer trong môi trường đa luồng
    private final List<AuctionObserver> observers = new CopyOnWriteArrayList<>();

    // Quản lý luồng đếm ngược thời gian đấu giá
    private transient final ExecutorService executor = Executors.newSingleThreadExecutor();
    private transient Future<String> timerFuture;

    public Auction(Item item) {
        this.item = item;
        this.currentHighestBid = item.getStartingPrice();
        this.status = AuctionStatus.OPEN;
        // Khởi tạo kết nối DAO ngay khi tạo Auction
        this.dao = new BidTransactionDAO(DatabaseConnection.getConnection());
    }

    /**
     * Xử lý đặt giá mới (Thread-safe)
     * Từ khóa synchronized đảm bảo tại một thời điểm chỉ có 1 luồng được cập nhật giá
     */
    public synchronized boolean addValidBid(BidTransaction transaction) throws InvalidBidException {
        // 1. Kiểm tra trạng thái phiên
        if (this.status != AuctionStatus.RUNNING) {
            throw new InvalidBidException("Phiên đấu giá đang không diễn ra!");
        }

        // 2. Kiểm tra giá đặt có cao hơn giá hiện tại không
        if (transaction.getBidAmount() <= this.currentHighestBid) {
            throw new InvalidBidException("Giá đặt phải cao hơn giá hiện tại: " + this.currentHighestBid);
        }

        // 3. LƯU VÀO DATABASE CLOUD TRƯỚC
        // Chúng ta giả định auctionId = 1 cho bản test này
        if (dao != null && dao.saveTransaction(1, transaction)) {
            // 4. Cập nhật dữ liệu trên bộ nhớ (RAM) nếu lưu DB thành công
            this.bidHistory.add(transaction);
            this.currentHighestBid = transaction.getBidAmount();
            this.currentWinner = transaction.getBidder();

            System.out.println("✅ [DB] Đã lưu: " + transaction.getBidder().getUsername() + " -> $" + transaction.getBidAmount());

            // Thông báo cho các Observer (giao diện) cập nhật
            notifyNewBid(transaction);
            return true;
        } else {
            System.err.println("❌ Lỗi: Không thể lưu lượt đặt giá vào Database!");
            return false;
        }
    }

    /**
     * Bắt đầu phiên đấu giá với thời gian đếm ngược
     */
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
                    return "Phiên đấu giá bị gián đoạn.";
                }
                return endAuction();
            }
        });
    }

    private String endAuction() {
        this.status = AuctionStatus.FINISHED;
        System.out.println("\n>>> PHIÊN ĐẤU GIÁ KẾT THÚC!");

        // Giải phóng tài nguyên luồng
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }

        if (this.currentWinner != null) {
            return "Người chiến thắng là: " + this.currentWinner.getFullName() + " với giá $" + this.currentHighestBid;
        }
        return "Không có ai đặt giá cho phiên này.";
    }

    // --- Observer Pattern Methods ---
    public void addObserver(AuctionObserver observer) {
        observers.add(observer);
    }

    private void notifyNewBid(BidTransaction tx) {
        for (AuctionObserver observer : observers) {
            observer.onNewBid(tx);
        }
    }

    // --- Getters and Setters ---
    public AuctionStatus getStatus() { return status; }
    public double getCurrentHighestBid() { return currentHighestBid; }
    public Bidder getCurrentWinner() { return currentWinner; }
    public Item getItem() { return item; }
}