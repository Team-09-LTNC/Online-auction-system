package com.auction.server.manager;

import com.auction.common.exception.AuctionClosedException;
import com.auction.common.exception.InvalidBidException;
import com.auction.common.enums.AuctionStatus;
import com.auction.common.model.bid.Auction;
import com.auction.common.model.bid.AutoBidConfig;
import com.auction.common.model.bid.BidTransaction;
import com.auction.common.model.user.Bidder;
import com.auction.common.model.user.User;
import com.auction.common.observer.AuctionObserver;
import com.auction.server.dao.AuctionDao;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;

public class AuctionManager {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AuctionManager.class);
    private static volatile AuctionManager instance;
    private static final ZoneId SERVER_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final Map<Integer, Auction> dsPhienDangChay = new ConcurrentHashMap<>();
    private final Map<Integer, List<AuctionObserver>> dsNguoiTheoDoi = new ConcurrentHashMap<>();
    private final Map<Integer, ScheduledFuture<?>> tasksDongPhien = new ConcurrentHashMap<>();
    private final Map<Integer, ScheduledFuture<?>> tasksMoPhien = new ConcurrentHashMap<>();

    private final AuctionDao auctionDao = new AuctionDao();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final ExecutorService notifierPool = Executors.newFixedThreadPool(50);
    private final AuctionRealtimeNotifier realtimeNotifier =
            new AuctionRealtimeNotifier(dsNguoiTheoDoi, dsPhienDangChay, notifierPool, SERVER_ZONE);
    private final AuctionPaymentTimeoutService paymentTimeoutService =
            new AuctionPaymentTimeoutService(scheduler, auctionDao);
    private final AuctionLifecycleService lifecycleService = new AuctionLifecycleService(
            auctionDao,
            dsPhienDangChay,
            tasksDongPhien,
            tasksMoPhien,
            scheduler,
            paymentTimeoutService,
            realtimeNotifier,
            SERVER_ZONE
    );
    private final AuctionAutoBidService autoBidService = new AuctionAutoBidService(
            auctionDao,
            this::extendIfLateBid,
            this::notifyNewBid,
            this::finishAuctionAfterAutoBuyNow
    );
    private final AuctionBidCommandService bidCommandService = new AuctionBidCommandService(
            dsPhienDangChay,
            auctionDao,
            lifecycleService,
            paymentTimeoutService,
            autoBidService,
            realtimeNotifier
    );
    private final AuctionAdminSyncService adminSyncService = new AuctionAdminSyncService(
            auctionDao,
            dsPhienDangChay,
            SERVER_ZONE,
            this::loadAutoBidsIntoAuction,
            this::scheduleAuctionStart,
            this::scheduleAuctionClose,
            this::cancelAuctionStartSchedule,
            this::cancelAuctionCloseSchedule,
            realtimeNotifier
    );

    private void loadAutoBidsIntoAuction(Auction auction) {
        if (auction == null) {
            return;
        }
        List<AutoBidConfig> bots = auctionDao.getAuctionAutoBids(auction.getId());
        for (AutoBidConfig bot : bots) {
            auction.addAutoBidConfig(bot);
        }
    }

    // Tải phiên từ DB và lên lịch khi khởi động
    private AuctionManager() {
        // Khôi phục các phiên đang chạy
        for (Auction a : auctionDao.getRunningAuctions()) {
            loadAutoBidsIntoAuction(a);

            dsPhienDangChay.put(a.getId(), a);
            lifecycleService.scheduleAuctionClose(a);
        }
        // Lên lịch mở các phiên đang chờ
        for (Auction a : auctionDao.getPendingAuctionSessions()) {
            loadAutoBidsIntoAuction(a);

            lifecycleService.scheduleAuctionStart(a);
        }
        paymentTimeoutService.startOverduePaymentSweep();
    }

    // Khóa kiểm tra hai lần: đảm bảo an toàn luồng cho Singleton
    public static AuctionManager getInstance() {
        if (instance == null) {
            synchronized (AuctionManager.class) {
                if (instance == null)
                    instance = new AuctionManager();
            }
        }
        return instance;
    }

    // Lên lịch mở phiên vào thời điểm bắt đầu
    public void scheduleAuctionStart(Auction phien) {
        lifecycleService.scheduleAuctionStart(phien);
    }

    // Lên lịch đóng phiên, hủy tác vụ cũ nếu có (dùng cho chống đặt giá phút chót khi gia hạn)
    public void scheduleAuctionClose(Auction phien) {
        lifecycleService.scheduleAuctionClose(phien);
    }

    // Đóng phiên: xác định trạng thái kết thúc/bị hủy, dọn dẹp tài nguyên
    private void closeAuction(int idPhien) {
        lifecycleService.closeAuction(idPhien);
    }

    // Xử lý đặt giá: kiểm tra điều kiện, chống đặt giá phút chót, lưu DB và kích hoạt
    // đặt giá tự động
    public boolean handlePlaceBid(int idPhien, BidTransaction giaoDich) throws InvalidBidException, AuctionClosedException {
        Auction phien = dsPhienDangChay.get(idPhien);
        if (phien == null) {
            Auction tuDb = auctionDao.getAuctionById(idPhien);
            if (tuDb == null) {
                throw new InvalidBidException("Phiên đấu giá không tồn tại.");
            }
            AuctionStatus status = tuDb.getStoredStatus();
            if (status == AuctionStatus.OPEN) {
                throw new InvalidBidException("Phiên chưa mở, chưa thể đặt giá.");
            }
            if (status == AuctionStatus.RUNNING) {
                loadAutoBidsIntoAuction(tuDb);
                dsPhienDangChay.put(tuDb.getId(), tuDb);
                scheduleAuctionClose(tuDb);
                phien = tuDb;
            }
            if (status == AuctionStatus.CANCELED) {
                throw new AuctionClosedException("Phiên đã bị hủy.");
            }
            if (status == AuctionStatus.FINISHED || status == AuctionStatus.PAID) {
                throw new AuctionClosedException("Phiên đã kết thúc.");
            }
        }

        synchronized (phien) { // Đồng bộ trên phiên để tránh tranh chấp luồng
            if (!phien.isAcceptingBids()) {
                closeAuction(idPhien);
                throw new AuctionClosedException("Phiên đã kết thúc!");
            }

            // Chặn người bán tự đặt giá sản phẩm của mình
            if (giaoDich.getBidder().getId() == phien.getItem().getSellerId())
                throw new InvalidBidException("Không được tự bid sản phẩm của mình!");

            long giaHienTai = phien.getCurrentHighestBid();
            long buocGia = phien.getItem().getBidIncrement();
            boolean chuaCoAiDatGia = phien.getCurrentWinner() == null;

            if (isBuyNowBid(phien, giaoDich.getBidAmount())) {
                throw new InvalidBidException("Mức giá này đạt giá mua đứt. Hãy xác nhận mua ngay.");
            }

            long giaToiThieu = chuaCoAiDatGia ? giaHienTai : giaHienTai + buocGia;

            if (giaoDich.getBidAmount() < giaToiThieu) {
                throw new InvalidBidException("Giá đặt tối thiểu: " + giaToiThieu);
            }

            // Lưu giao dịch vào DB và cập nhật người thắng hiện tại
            if (auctionDao.executeBidTransaction(idPhien, giaoDich)) {
                phien.updateWinner(giaoDich);
                extendIfLateBid(phien);
                notifyNewBid(idPhien, giaoDich);
                autoBidService.triggerAutoBid(phien, giaoDich.getBidder().getId()); // Kích hoạt đặt giá tự động để đáp trả nếu cần
                return true;
            }
            return false;
        }
    }

    /** Mua đứt luôn chốt ở giá mua đứt trong DB, không lấy số tiền từ client. */
    public BidTransaction handleBuyNow(int idPhien, Bidder bidder)
            throws InvalidBidException, AuctionClosedException {
        return bidCommandService.handleBuyNow(idPhien, bidder);
    }

    // Đăng ký đặt giá tự động: đặt giá tự động đến mức tối đa cho phép
    public void registerAutoBid(int idPhien, User bidder, long maxBid, long bidStep) throws Exception {
        bidCommandService.registerAutoBid(idPhien, bidder, maxBid, bidStep);
    }

    private void finishAuctionAfterAutoBuyNow(Auction phien) {
        phien.setStatus(AuctionStatus.FINISHED);
        auctionDao.updateStatus(phien.getId(), AuctionStatus.FINISHED.name());
        lifecycleService.cancelAuctionCloseSchedule(phien.getId());
        notifyStatusChange(phien.getId(), AuctionStatus.FINISHED);
        AuctionDao.AuctionNotificationTargets targets =
                auctionDao.getAuctionEndNotificationTargets(phien.getId());
        AuctionSettlementNotifier.sendAuctionEndNotification(targets);
        paymentTimeoutService.schedulePaymentTimeout(phien.getId(), targets);
    }

    // Đăng ký đối tượng quan sát để nhận thông báo khi có giá đặt mới
    public void subscribe(int idPhien, AuctionObserver obs) {
        realtimeNotifier.subscribe(idPhien, obs);
    }

    // Gửi thông báo đến tất cả đối tượng quan sát đang theo dõi phiên
    private void notifyNewBid(int idPhien, BidTransaction tx) {
        realtimeNotifier.notifyNewBid(idPhien, tx);
    }

    public void updateStatusAfterPayment(int idPhien, AuctionStatus status) {
        paymentTimeoutService.cancelPaymentTimeout(idPhien);
        Auction phien = getAuctionById(idPhien);
        if (phien != null) {
            synchronized (phien) {
                phien.setStatus(status);
                cancelAuctionCloseSchedule(idPhien);
                notifyStatusChange(idPhien, status);
            }
        }
    }

    private boolean isBuyNowBid(Auction phien, long giaDat) {
        return phien.getBuyNowPrice() != null
                && phien.getBuyNowPrice() > 0
                && giaDat >= phien.getBuyNowPrice();
    }

    private void extendIfLateBid(Auction phien) {
        if (!phien.isAntiSnipingEnabled()) {
            return;
        }

        LocalDateTime endTime = phien.getEndTime();
        LocalDateTime now = LocalDateTime.now(SERVER_ZONE);
        if (endTime == null || now.isBefore(endTime.minusSeconds(30)) || !now.isBefore(endTime)) {
            return;
        }

        phien.setEndTime(endTime.plusSeconds(60));
        if (!auctionDao.updateEndTime(phien.getId(), phien.getEndTime())) {
            phien.setEndTime(endTime);
            return;
        }
        lifecycleService.scheduleAuctionClose(phien);
    }

    private void notifyStatusChange(int idPhien, AuctionStatus status) {
        realtimeNotifier.notifyStatusChange(idPhien, status);
    }

    public void broadcastAuctionChanged(int auctionId, String reason, String status) {
        realtimeNotifier.broadcastAuctionChanged(auctionId, reason, status);
    }

    private void cancelAuctionCloseSchedule(int idPhien) {
        lifecycleService.cancelAuctionCloseSchedule(idPhien);
    }

    private void cancelAuctionStartSchedule(int idPhien) {
        lifecycleService.cancelAuctionStartSchedule(idPhien);
    }

    public void removeAutoBid(int idPhien, User bidder) throws Exception {
        bidCommandService.removeAutoBid(idPhien, bidder);
    }

    // Gửi tin nhắn chat đến tất cả đối tượng quan sát trong phiên
    public void broadcastChatMessage(int idPhien, String senderName, String message, boolean isSystem) {
        realtimeNotifier.broadcastChatMessage(idPhien, senderName, message, isSystem);
    }

    /** Lấy danh sách các phiên đang chạy (từ bộ nhớ tạm ConcurrentHashMap) */
    public List<Auction> getRunningAuctions() {
        return new ArrayList<>(dsPhienDangChay.values());
    }

    /** Lấy chi tiết 1 phiên đang chạy */
    public Auction getAuctionById(int idPhien) {
        Auction phien = dsPhienDangChay.get(idPhien);

        if (phien == null) {
            for (Auction a : auctionDao.getRunningAuctions()) {
                if (a.getId() == idPhien) {
                    loadAutoBidsIntoAuction(a);

                    dsPhienDangChay.put(a.getId(), a);
                    scheduleAuctionClose(a);
                    return a;
                }
            }
        }
        return phien;
    }

    /** Gỡ Client khỏi danh sách nhận thông báo Real-time (Rời phòng) */
    public void unsubscribe(int idPhien, AuctionObserver obs) {
        realtimeNotifier.unsubscribe(idPhien, obs);
    }

    /** Ép buộc đóng phiên đấu giá ngay lập tức (Dành cho Admin) */
    public boolean forceCloseAuction(int idPhien) {
        if (dsPhienDangChay.containsKey(idPhien)) {
            closeAuction(idPhien);
            return true;
        }
        return false;
    }
    
    //-----------CHỨC NĂNG ADMIN: QUẢN LÝ PHIÊN ĐẤU GIÁ -----------/
    /**
     * Duyệt phiên PENDING: tính đúng trạng thái theo thời gian rồi lên lịch bộ lập lịch
     */
    public boolean approveAuctionSession(int auctionId) {
        boolean approved = adminSyncService.approveAuctionSession(auctionId);
        if (approved) {
            logger.info("Admin da duyet phien {}", auctionId);
        }
        return approved;
    }

    /** Admin thay đổi trạng thái: đồng bộ RAM và scheduler sau khi DB đã update */
    public void syncAfterStatusUpdate(int auctionId, String newStatus) {
        adminSyncService.syncAfterStatusUpdate(auctionId, newStatus);
    }

    public void syncAfterAuctionDeleted(int auctionId) {
        adminSyncService.syncAfterAuctionDeleted(auctionId);
    }
}
