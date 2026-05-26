package com.auction.server.manager;

import com.auction.common.exception.AuctionClosedException;
import com.auction.common.exception.InvalidBidException;
import com.auction.common.enums.ActionType;
import com.auction.common.enums.AuctionStatus;
import com.auction.common.model.bid.Auction;
import com.auction.common.model.bid.AutoBidConfig;
import com.auction.common.model.bid.BidTransaction;
import com.auction.common.model.user.Bidder;
import com.auction.common.model.user.User;
import com.auction.common.observer.AuctionObserver;
import com.auction.server.dao.AuctionDao;
import com.auction.server.dao.BidderPenaltyDao;
import com.google.gson.JsonObject;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;

public class AuctionManager {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AuctionManager.class);
    private static volatile AuctionManager instance;
    private static final ZoneId SERVER_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final long PAYMENT_TIMEOUT_MINUTES = 24 * 60;

    private final Map<Integer, Auction> dsPhienDangChay = new ConcurrentHashMap<>();
    private final Map<Integer, List<AuctionObserver>> dsNguoiTheoDoi = new ConcurrentHashMap<>();
    private final Map<Integer, ScheduledFuture<?>> tasksDongPhien = new ConcurrentHashMap<>();
    private final Map<Integer, ScheduledFuture<?>> tasksMoPhien = new ConcurrentHashMap<>();
    private final Map<Integer, ScheduledFuture<?>> tasksQuaHanThanhToan = new ConcurrentHashMap<>();

    private final AuctionDao auctionDao = new AuctionDao();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final ExecutorService notifierPool = Executors.newFixedThreadPool(50);

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
            // ---> PHỤC HỒI BOT TỪ DATABASE LÊN RAM
            loadAutoBidsIntoAuction(a);

            dsPhienDangChay.put(a.getId(), a);
            scheduleAuctionClose(a);
        }
        // Lên lịch mở các phiên đang chờ
        for (Auction a : auctionDao.getPendingAuctionSessions()) {
            loadAutoBidsIntoAuction(a);

            scheduleAuctionStart(a);
        }
    }

    // Double-checked locking: đảm bảo thread-safe cho Singleton
    public static AuctionManager getInstance() {
        if (instance == null) {
            synchronized (AuctionManager.class) {
                if (instance == null)
                    instance = new AuctionManager();
            }
        }
        return instance;
    }

    // Lên lịch mở phiên vào thời điểm startTime
    public void scheduleAuctionStart(Auction phien) {
        ScheduledFuture<?> taskCu = tasksMoPhien.get(phien.getId());
        if (taskCu != null && !taskCu.isDone())
            taskCu.cancel(false);

        long delay = java.time.Duration.between(LocalDateTime.now(SERVER_ZONE), phien.getStartTime()).toMillis();

        if (delay <= 0) {
            openAuction(phien);
        } else {
            ScheduledFuture<?> taskMoi = scheduler.schedule(() -> openAuction(phien), delay, TimeUnit.MILLISECONDS);
            tasksMoPhien.put(phien.getId(), taskMoi);
        }
    }

    // Cập nhật trạng thái phiên thành RUNNING và lên lịch đóng
    private void openAuction(Auction phien) {
        if (auctionDao.updateStatus(phien.getId(), AuctionStatus.RUNNING.name())) {
            phien.setStatus(AuctionStatus.RUNNING);
            dsPhienDangChay.put(phien.getId(), phien);

            tasksMoPhien.remove(phien.getId());

            logger.info("Đã tự động mở phiên đấu giá ID: {}", phien.getId());
            scheduleAuctionClose(phien); // Bắt đầu đếm ngược đến giờ đóng
            notifyStatusChange(phien.getId(), AuctionStatus.RUNNING);
        }
    }

    // Lên lịch đóng phiên, hủy task cũ nếu có (dùng cho Anti-sniping khi gia hạn)
    public void scheduleAuctionClose(Auction phien) {
        // Hủy tác vụ đóng phiên cũ nếu đang chạy
        ScheduledFuture<?> taskCu = tasksDongPhien.get(phien.getId());
        if (taskCu != null && !taskCu.isDone()) {
            taskCu.cancel(false);
        }

        long delay = java.time.Duration.between(LocalDateTime.now(SERVER_ZONE), phien.getEndTime()).toMillis();

        if (delay <= 0) {
            closeAuction(phien.getId()); // Đã quá giờ thì đóng luôn
        } else {
            ScheduledFuture<?> taskMoi = scheduler.schedule(() -> closeAuction(phien.getId()), delay,
                    TimeUnit.MILLISECONDS);
            tasksDongPhien.put(phien.getId(), taskMoi);
        }
    }

    // Đóng phiên: xác định trạng thái FINISHED/CANCELED, dọn dẹp tài nguyên
    private void closeAuction(int idPhien) {
        Auction p = dsPhienDangChay.remove(idPhien);
        if (p != null) {
            synchronized (p) {
                AuctionDao.AuctionNotificationTargets targets = auctionDao.getAuctionEndNotificationTargets(idPhien);
                // Có người thắng -> FINISHED, không có -> CANCELED
                AuctionStatus statusMoi = targets != null && targets.winnerId != null
                        ? AuctionStatus.FINISHED
                        : AuctionStatus.CANCELED;
                p.setStatus(statusMoi);
                auctionDao.updateStatus(idPhien, statusMoi.name());
                if (statusMoi == AuctionStatus.FINISHED) {
                    logger.info("Dong phien {} FINISHED. winnerId={}, sellerId={}",
                            idPhien,
                            targets != null ? targets.winnerId : null,
                            targets != null ? targets.sellerId : null);
                    AuctionSettlementNotifier.sendAuctionEndNotification(targets);
                    schedulePaymentTimeout(idPhien, targets);
                }
                notifyStatusChange(idPhien, statusMoi);

                // Xóa phiên khỏi bộ nhớ và dọn dẹp tài nguyên liên quan
                dsNguoiTheoDoi.remove(idPhien);
                tasksDongPhien.remove(idPhien);
            }
        }
    }

    // Xử lý đặt giá: kiểm tra điều kiện, chống sniping, lưu DB và kích hoạt
    // auto-bid
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

        synchronized (phien) { // Đồng bộ trên phiên để tránh race condition
            if (!phien.isAcceptingBids()) {
                closeAuction(idPhien);
                throw new AuctionClosedException("Phiên đã kết thúc!");
            }

            // Chặn seller tự bid sản phẩm của mình
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
                triggerAutoBid(phien, giaoDich.getBidder().getId()); // Kích hoạt auto-bid để đáp trả nếu cần
                return true;
            }
            return false;
        }
    }

    /** Mua đứt luôn chốt ở giá mua đứt trong DB, không lấy số tiền từ client. */
    public BidTransaction handleBuyNow(int idPhien, Bidder bidder)
            throws InvalidBidException, AuctionClosedException {
        Auction phien = dsPhienDangChay.get(idPhien);
        if (phien == null) {
            throw new InvalidBidException("Phiên không khả dụng!");
        }

        synchronized (phien) {
            if (!phien.isAcceptingBids()) {
                closeAuction(idPhien);
                throw new AuctionClosedException("Phiên đã kết thúc!");
            }
            if (bidder.getId() == phien.getItem().getSellerId()) {
                throw new InvalidBidException("Không được mua sản phẩm của chính mình!");
            }
            if (phien.getBuyNowPrice() == null || phien.getBuyNowPrice() <= 0) {
                throw new InvalidBidException("Phiên này không hỗ trợ mua đứt.");
            }

            long giaMuaDut = phien.getBuyNowPrice();
            if (giaMuaDut <= phien.getCurrentHighestBid()) {
                throw new InvalidBidException("Giá mua đứt không còn hợp lệ ở thời điểm hiện tại.");
            }

            BidTransaction giaoDich = new BidTransaction(idPhien, bidder, giaMuaDut, LocalDateTime.now());
            if (!auctionDao.executeBidTransaction(idPhien, giaoDich)) {
                throw new InvalidBidException("Đã có người trả giá cao hơn.");
            }

            phien.updateWinner(giaoDich);
            phien.setStatus(AuctionStatus.FINISHED);
            auctionDao.updateStatus(idPhien, AuctionStatus.FINISHED.name());
            cancelAuctionCloseSchedule(idPhien);
            notifyNewBid(idPhien, giaoDich);
            notifyStatusChange(idPhien, AuctionStatus.FINISHED);
            AuctionDao.AuctionNotificationTargets targets = auctionDao.getAuctionEndNotificationTargets(idPhien);
            schedulePaymentTimeout(idPhien, targets);
            return giaoDich;
        }
    }

    // Đăng ký auto-bid: đặt giá tự động đến mức tối đa cho phép
    public void registerAutoBid(int idPhien, User bidder, long maxBid, long bidStep) throws Exception {
        Auction phien = dsPhienDangChay.get(idPhien);
        if (phien == null)
            throw new Exception("Phiên không khả dụng!");

        synchronized (phien) {
            if (!phien.isAcceptingBids()) {
                closeAuction(idPhien);
                throw new AuctionClosedException("Phiên đã kết thúc!");
            }

            // Chặn seller cài auto-bid cho sản phẩm của mình
            if (bidder.getId() == phien.getItem().getSellerId()) {
                throw new Exception("Seller không thể đăng ký Auto-bid cho sản phẩm của mình!");
            }

            long giaHienTai = phien.getCurrentHighestBid();
            long buocGiaNguoiBan = phien.getItem().getBidIncrement();
            if (maxBid <= giaHienTai) {
                throw new Exception("Giá max Auto-bid phải lớn hơn giá hiện tại.");
            }
            if (bidStep < buocGiaNguoiBan) {
                throw new Exception("Bước giá Auto-bid phải >= bước giá người bán.");
            }

            Queue<AutoBidConfig> queue = phien.getAutoBidders();
            queue.removeIf(bot -> bot.getBidder().getId() == bidder.getId());

            phien.addAutoBidConfig(new AutoBidConfig(bidder, maxBid, bidStep));

            boolean luuThanhCong = auctionDao.saveOrUpdateAutoBid(idPhien, bidder.getId(), maxBid, bidStep);
            if (!luuThanhCong) {
                throw new Exception("Không thể lưu cấu hình Auto-bid. Vui lòng thử lại.");
            }

            triggerAutoBid(phien, null);
        }
    }

    // Xử lý auto-bid: bot đại diện người dùng tự động trả giá theo bước giá
    private void triggerAutoBid(Auction phien, Integer manualBidderId) {
        while (phien.isAcceptingBids()) {
            List<AutoBidConfig> activeBots = getActiveAutoBids(phien);
            if (activeBots.isEmpty()) {
                return;
            }

            AutoBidConfig leader = activeBots.get(0);
            AutoBidConfig challenger = findNextCompetitor(activeBots, leader);
            if (isCurrentWinner(phien, leader) && challenger == null) {
                return;
            }

            long nextBidAmount = calculateAutoBidAmount(
                    phien,
                    leader,
                    challenger,
                    isManualCurrentWinner(phien, leader, manualBidderId));
            long minimumNextBid = phien.getCurrentHighestBid() + phien.getItem().getBidIncrement();
            Long buyNowPrice = phien.getBuyNowPrice();
            boolean reachedBuyNow = buyNowPrice != null
                    && buyNowPrice > 0
                    && phien.getCurrentHighestBid() < buyNowPrice
                    && nextBidAmount >= buyNowPrice
                    && leader.getMaxBid() >= buyNowPrice;

            if (!reachedBuyNow && (nextBidAmount < minimumNextBid || nextBidAmount > leader.getMaxBid())) {
                return;
            }

            if (reachedBuyNow) {
                nextBidAmount = buyNowPrice;
            }

            BidTransaction autoTx = new BidTransaction(
                    phien.getId(),
                    (Bidder) leader.getBidder(),
                    nextBidAmount,
                    LocalDateTime.now());

            if (!auctionDao.executeBidTransaction(phien.getId(), autoTx)) {
                return;
            }

            phien.updateWinner(autoTx);
            extendIfLateBid(phien);
            notifyNewBid(phien.getId(), autoTx);

            if (reachedBuyNow) {
                finishAuctionAfterAutoBuyNow(phien);
                return;
            }
        }
    }

    private List<AutoBidConfig> getActiveAutoBids(Auction phien) {
        long currentPrice = phien.getCurrentHighestBid();
        List<AutoBidConfig> activeBots = new ArrayList<>();
        for (AutoBidConfig bot : phien.getAutoBidders()) {
            if (bot.getMaxBid() > currentPrice) {
                activeBots.add(bot);
            }
        }
        Collections.sort(activeBots);
        return activeBots;
    }

    private AutoBidConfig findNextCompetitor(List<AutoBidConfig> activeBots, AutoBidConfig leader) {
        for (AutoBidConfig bot : activeBots) {
            if (bot.getBidder().getId() != leader.getBidder().getId()) {
                return bot;
            }
        }
        return null;
    }

    private long calculateAutoBidAmount(
            Auction phien,
            AutoBidConfig leader,
            AutoBidConfig challenger,
            boolean currentWinnerIsManualBidder
    ) {
        long currentPrice = phien.getCurrentHighestBid();
        long minimumNextBid = currentPrice + phien.getItem().getBidIncrement();
        long targetBid;
        if (currentWinnerIsManualBidder) {
            targetBid = currentPrice + effectiveAutoBidStep(phien, leader);
            return targetBid;
        } else if (challenger == null) {
            targetBid = currentPrice + effectiveAutoBidStep(phien, leader);
        } else if (leader.getMaxBid() == challenger.getMaxBid()) {
            targetBid = currentPrice + effectiveAutoBidStep(phien, leader);
        } else {
            targetBid = challenger.getMaxBid() + effectiveAutoBidStep(phien, challenger);
        }
        return Math.max(targetBid, minimumNextBid);
    }

    private boolean isManualCurrentWinner(
            Auction phien,
            AutoBidConfig leader,
            Integer manualBidderId
    ) {
        return manualBidderId != null
                && phien.getCurrentWinner() != null
                && phien.getCurrentWinner().getId() == manualBidderId
                && leader.getBidder().getId() != manualBidderId;
    }

    private long effectiveAutoBidStep(Auction phien, AutoBidConfig bot) {
        return Math.max(bot.getBidStep(), phien.getItem().getBidIncrement());
    }

    private boolean isCurrentWinner(Auction phien, AutoBidConfig bot) {
        return phien.getCurrentWinner() != null
                && phien.getCurrentWinner().getId() == bot.getBidder().getId();
    }

    private void finishAuctionAfterAutoBuyNow(Auction phien) {
        phien.setStatus(AuctionStatus.FINISHED);
        auctionDao.updateStatus(phien.getId(), AuctionStatus.FINISHED.name());
        cancelAuctionCloseSchedule(phien.getId());
        notifyStatusChange(phien.getId(), AuctionStatus.FINISHED);
        AuctionDao.AuctionNotificationTargets targets =
                auctionDao.getAuctionEndNotificationTargets(phien.getId());
        AuctionSettlementNotifier.sendAuctionEndNotification(targets);
        schedulePaymentTimeout(phien.getId(), targets);
    }

    // Đăng ký observer để nhận thông báo khi có bid mới
    public void subscribe(int idPhien, AuctionObserver obs) {
        List<AuctionObserver> observers = dsNguoiTheoDoi.computeIfAbsent(idPhien, k -> new CopyOnWriteArrayList<>());
        if (!observers.contains(obs)) {
            observers.add(obs);
        }
    }

    // Gửi thông báo đến tất cả observer đang theo dõi phiên
    private void notifyNewBid(int idPhien, BidTransaction tx) {
        List<AuctionObserver> observers = dsNguoiTheoDoi.get(idPhien);
        if (observers != null) {
            for (AuctionObserver obs : observers)
                obs.onNewBid(tx);
        }
        Auction phien = dsPhienDangChay.get(idPhien);
        broadcastAuctionChanged(
                idPhien,
                "BID",
                phien != null && phien.getStatus() != null ? phien.getStatus().name() : null
        );
    }

    public void updateStatusAfterPayment(int idPhien, AuctionStatus status) {
        cancelPaymentTimeoutSchedule(idPhien);
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
        scheduleAuctionClose(phien);
    }

    private void notifyStatusChange(int idPhien, AuctionStatus status) {
        List<AuctionObserver> observers = dsNguoiTheoDoi.get(idPhien);
        if (observers != null) {
            for (AuctionObserver obs : observers) {
                obs.onStatusChanged(status);
            }
        }
        broadcastAuctionChanged(idPhien, "STATUS", status != null ? status.name() : null);
    }

    public void broadcastAuctionChanged(int auctionId, String reason, String status) {
        JsonObject payload = new JsonObject();
        payload.addProperty("type", ActionType.AUCTION_CHANGED);
        payload.addProperty("auctionId", auctionId);
        payload.addProperty("reason", reason != null ? reason : "UNKNOWN");
        if (status != null) {
            payload.addProperty("status", status);
        }
        payload.addProperty("serverNow", LocalDateTime.now(SERVER_ZONE).toString());
        UserManager.getInstance().broadcastPushEvent(payload);
    }

    private void cancelAuctionCloseSchedule(int idPhien) {
        ScheduledFuture<?> taskDong = tasksDongPhien.remove(idPhien);
        if (taskDong != null && !taskDong.isDone()) {
            taskDong.cancel(false);
        }
        dsPhienDangChay.remove(idPhien);
    }

    private void cancelAuctionStartSchedule(int idPhien) {
        ScheduledFuture<?> taskMo = tasksMoPhien.remove(idPhien);
        if (taskMo != null && !taskMo.isDone()) {
            taskMo.cancel(false);
        }
    }

    private void schedulePaymentTimeout(int auctionId, AuctionDao.AuctionNotificationTargets targets) {
        if (targets == null || targets.winnerId == null) {
            return;
        }
        cancelPaymentTimeoutSchedule(auctionId);
        ScheduledFuture<?> task = scheduler.schedule(
                () -> autoCancelOverduePayment(auctionId, targets),
                PAYMENT_TIMEOUT_MINUTES,
                TimeUnit.MINUTES);
        tasksQuaHanThanhToan.put(auctionId, task);
        logger.info("Da len lich tu dong huy thanh toan cho phien {} sau {} phut.", auctionId, PAYMENT_TIMEOUT_MINUTES);
    }

    private void cancelPaymentTimeoutSchedule(int auctionId) {
        ScheduledFuture<?> task = tasksQuaHanThanhToan.remove(auctionId);
        if (task != null && !task.isDone()) {
            task.cancel(false);
        }
    }

    private void autoCancelOverduePayment(int auctionId, AuctionDao.AuctionNotificationTargets targets) {
        try {
            if (targets == null || targets.winnerId == null) {
                return;
            }
            com.auction.server.dao.BidderMoneySellerDao.PaymentResult ketQua = new com.auction.server.dao.BidderMoneySellerDao()
                    .settleBuyNow(auctionId, targets.winnerId, false);

            if (!ketQua.success) {
            logger.info("Auto settlement skipped for auction {}: {}", auctionId, ketQua.message);
            if (ketQua.message != null
                    && (ketQua.message.toLowerCase().contains("không đủ")
                    || ketQua.message.toLowerCase().contains("khong du"))) {
                applyLatePaymentPenalty(auctionId, targets.winnerId, targets.sellerId, "Không đủ số dư để thanh toán quá hạn.");
            }
            return;
            }

            updateStatusAfterPayment(auctionId, AuctionStatus.CANCELED);
            String itemName = ketQua.itemName == null || ketQua.itemName.isBlank()
                    ? "sản phẩm"
                    : ketQua.itemName;

            SystemNotificationManager.getInstance().sendPrivateNotification(
                    auctionId,
                    targets.winnerId,
                    "Phiên " + auctionId
                            + " đã quá hạn thanh toán. Hệ thống tự động hủy và trừ phí phạt 10% cho sản phẩm "
                            + itemName + ".",
                    false);

            applyLatePaymentPenalty(auctionId, targets.winnerId, targets.sellerId, "Quá hạn thanh toán phiên đấu giá.");

            if (targets.sellerId > 0) {
                SystemNotificationManager.getInstance().sendPrivateNotification(
                        auctionId,
                        targets.sellerId,
                        "Bidder đã quá hạn thanh toán ở phiên " + auctionId
                                + ". Hệ thống đã tự động hủy và chuyển phí phạt cho bạn.",
                        false);
            }
            logger.info("Auto settlement success for auction {}.", auctionId);
        } catch (Exception e) {
            logger.error("Auto settlement failed for auction {}.", auctionId, e);
        } finally {
            tasksQuaHanThanhToan.remove(auctionId);
        }
    }

    public void removeAutoBid(int idPhien, User bidder) throws Exception {
        Auction phien = dsPhienDangChay.get(idPhien);
        if (phien == null) {
            throw new Exception("Phiên không khả dụng!");
        }
        synchronized (phien) {
            phien.getAutoBidders().removeIf(bot -> bot.getBidder().getId() == bidder.getId());
            if (!auctionDao.removeAutoBid(idPhien, bidder.getId())) {
                throw new Exception("Không thể xóa Auto-bid.");
            }
        }
    }

    private void applyLatePaymentPenalty(int auctionId, int winnerId, int sellerId, String reason) {
        BidderPenaltyDao.SanctionResult sanction = new BidderPenaltyDao().recordLatePaymentViolation(winnerId, reason);
        if (sanction.violationCount <= 0) {
            return;
        }

        String bidderMessage;
        if (sanction.permanentLock) {
            UserManager.getInstance().updateAccountStatus(winnerId, "LOCKED");
            bidderMessage = "Bạn đã vi phạm quá hạn thanh toán " + sanction.violationCount
                    + " lần. Tài khoản bị khóa vĩnh viễn, vui lòng liên hệ Admin.";
        } else {
            bidderMessage = "Bạn đã vi phạm quá hạn thanh toán lần " + sanction.violationCount
                    + ". Tạm cấm đấu giá đến " + sanction.lockUntil + ".";
        }

        SystemNotificationManager.getInstance().sendPrivateNotification(
                auctionId,
                winnerId,
                bidderMessage,
                false);

        if (sellerId > 0) {
            SystemNotificationManager.getInstance().sendPrivateNotification(
                    auctionId,
                    sellerId,
                    "Hệ thống đã áp dụng xử phạt bidder vì quá hạn thanh toán.",
                    false
            );
        }
    }

    // Gửi tin nhắn chat đến tất cả observer trong phiên
    public void broadcastChatMessage(int idPhien, String senderName, String message, boolean isSystem) {
        notifierPool.execute(() -> {
            List<AuctionObserver> observers = dsNguoiTheoDoi.get(idPhien);
            if (observers != null) {
                for (AuctionObserver obs : observers) {
                    obs.onChatMessage(senderName, message, isSystem);
                }
            }
        });
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
        List<AuctionObserver> observers = dsNguoiTheoDoi.get(idPhien);
        if (observers != null) {
            observers.removeIf(observer -> observer == obs);
        }
    }

    /** Ép buộc đóng phiên đấu giá ngay lập tức (Dành cho Admin) */
    public boolean forceCloseAuction(int idPhien) {
        if (dsPhienDangChay.containsKey(idPhien)) {
            closeAuction(idPhien);
            return true;
        }
        return false;
    }
    
    //-----------ADMIN FUNCTION: QUẢN LÝ PHIÊN ĐẤU GIÁ -----------/
    /**
     * Duyệt phiên PENDING: tính đúng status theo thời gian rồi lên lịch scheduler
     */
    public boolean approveAuctionSession(int auctionId) {
        Auction phien = auctionDao.getAuctionById(auctionId);
        if (phien == null)
            return false;

        LocalDateTime now = LocalDateTime.now(SERVER_ZONE);
        String statusMoi;
        if (now.isBefore(phien.getStartTime())) {
            statusMoi = AuctionStatus.OPEN.name();
        } else if (now.isAfter(phien.getEndTime())) {
            statusMoi = AuctionStatus.CANCELED.name(); // quá giờ, không có winner
        } else {
            statusMoi = AuctionStatus.RUNNING.name();
        }

        if (!auctionDao.updateStatus(auctionId, statusMoi))
            return false;

        phien.setStatus(AuctionStatus.valueOf(statusMoi));
        loadAutoBidsIntoAuction(phien);

        switch (statusMoi) {
            case "OPEN":
                scheduleAuctionStart(phien);
                break;
            case "RUNNING":
                dsPhienDangChay.put(auctionId, phien);
                scheduleAuctionClose(phien);
                break;
            // CANCELED: không cần lên lịch gì
        }

        logger.info("Admin da duyet phien {} -> {}", auctionId, statusMoi);
        broadcastAuctionChanged(auctionId, "APPROVED", statusMoi);
        return true;
    }

    /** Admin thay đổi trạng thái: đồng bộ RAM và scheduler sau khi DB đã update */
    public void syncAfterStatusUpdate(int auctionId, String newStatus) {
        switch (newStatus) {
            case "CANCELED":
                cancelAuctionStartSchedule(auctionId);
                cancelAuctionCloseSchedule(auctionId);
                notifyStatusChange(auctionId, AuctionStatus.CANCELED);
                break;
            case "OPEN":
                Auction phienOpen = auctionDao.getAuctionById(auctionId);
                if (phienOpen != null) {
                    dsPhienDangChay.remove(auctionId); // đảm bảo không còn trong RAM cũ
                    loadAutoBidsIntoAuction(phienOpen);
                    scheduleAuctionStart(phienOpen);
                    notifyStatusChange(auctionId, AuctionStatus.OPEN);
                }
                break;
            case "RUNNING":
                Auction phienRunning = auctionDao.getAuctionById(auctionId);
                if (phienRunning != null) {
                    loadAutoBidsIntoAuction(phienRunning);
                    dsPhienDangChay.put(auctionId, phienRunning);
                    scheduleAuctionClose(phienRunning);
                    notifyStatusChange(auctionId, AuctionStatus.RUNNING);
                }
                break;
            default:
                // FINISHED, PAID, REJECTED: chỉ cần xóa khỏi RAM nếu có
                cancelAuctionStartSchedule(auctionId);
                cancelAuctionCloseSchedule(auctionId);
                try {
                    notifyStatusChange(auctionId, AuctionStatus.valueOf(newStatus));
                } catch (IllegalArgumentException ignored) {
                    broadcastAuctionChanged(auctionId, "STATUS", newStatus);
                }
                break;
        }
    }
}
