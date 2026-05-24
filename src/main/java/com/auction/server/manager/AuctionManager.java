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
    private static final long PAYMENT_TIMEOUT_MINUTES = 24 * 60 ;

    private final Map<Integer, Auction> dsPhienDangChay = new ConcurrentHashMap<>();
    private final Map<Integer, List<AuctionObserver>> dsNguoiTheoDoi = new ConcurrentHashMap<>();
    private final Map<Integer, ScheduledFuture<?>> tasksDongPhien = new ConcurrentHashMap<>();
    private final Map<Integer, ScheduledFuture<?>> tasksMoPhien = new ConcurrentHashMap<>();
    private final Map<Integer, ScheduledFuture<?>> tasksQuaHanThanhToan = new ConcurrentHashMap<>();

    private final AuctionDao auctionDao = new AuctionDao();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final ExecutorService notifierPool = Executors.newFixedThreadPool(50);

    // Tải phiên từ DB và lên lịch khi khởi động
    private AuctionManager() {
        // Khôi phục các phiên đang chạy
        for (Auction a : auctionDao.layDanhSachPhienDangChay()) {
            // ---> PHỤC HỒI BOT TỪ DATABASE LÊN RAM
            List<AutoBidConfig> bots = auctionDao.layDanhSachAutoBidCuaPhien(a.getId());
            for(AutoBidConfig bot : bots) {
                a.addAutoBidConfig(bot);
            }

            dsPhienDangChay.put(a.getId(), a);
            henGioDongPhien(a);
        }
        // Lên lịch mở các phiên đang chờ
        for (Auction a : auctionDao.layDanhSachPhienChoMo()) {
            List<AutoBidConfig> bots = auctionDao.layDanhSachAutoBidCuaPhien(a.getId());
            for(AutoBidConfig bot : bots) {
                a.addAutoBidConfig(bot);
            }

            henGioMoPhien(a);
        }
    }

    // Double-checked locking: đảm bảo thread-safe cho Singleton
    public static AuctionManager getInstance() {
        if (instance == null) {
            synchronized (AuctionManager.class) {
                if (instance == null) instance = new AuctionManager();
            }
        }
        return instance;
    }

    // Lên lịch mở phiên vào thời điểm startTime
    public void henGioMoPhien(Auction phien) {
        ScheduledFuture<?> taskCu = tasksMoPhien.get(phien.getId());
        if (taskCu != null && !taskCu.isDone()) taskCu.cancel(false);

        long delay = java.time.Duration.between(LocalDateTime.now(SERVER_ZONE), phien.getStartTime()).toMillis();

        if (delay <= 0) {
            thucThiMoPhien(phien);
        } else {
            ScheduledFuture<?> taskMoi = scheduler.schedule(() -> thucThiMoPhien(phien), delay, TimeUnit.MILLISECONDS);
            tasksMoPhien.put(phien.getId(), taskMoi);
        }
    }

    // Cập nhật trạng thái phiên thành RUNNING và lên lịch đóng
    private void thucThiMoPhien(Auction phien) {
        if (auctionDao.capNhatTrangThai(phien.getId(), AuctionStatus.RUNNING.name())) {
            phien.setStatus(AuctionStatus.RUNNING);
            dsPhienDangChay.put(phien.getId(), phien);

            tasksMoPhien.remove(phien.getId());

            logger.info("Đã tự động mở phiên đấu giá ID: {}", phien.getId());
            henGioDongPhien(phien); // Bắt đầu đếm ngược đến giờ đóng
        }
    }

    // Lên lịch đóng phiên, hủy task cũ nếu có (dùng cho Anti-sniping khi gia hạn)
    public void henGioDongPhien(Auction phien) {
        // Hủy tác vụ đóng phiên cũ nếu đang chạy
        ScheduledFuture<?> taskCu = tasksDongPhien.get(phien.getId());
        if (taskCu != null && !taskCu.isDone()) {
            taskCu.cancel(false);
        }

        long delay = java.time.Duration.between(LocalDateTime.now(SERVER_ZONE), phien.getEndTime()).toMillis();

        if (delay <= 0) {
            dongPhien(phien.getId()); // Đã quá giờ thì đóng luôn
        } else {
            ScheduledFuture<?> taskMoi = scheduler.schedule(() -> dongPhien(phien.getId()), delay, TimeUnit.MILLISECONDS);
            tasksDongPhien.put(phien.getId(), taskMoi);
        }
    }

    // Đóng phiên: xác định trạng thái FINISHED/CANCELED, dọn dẹp tài nguyên
    private void dongPhien(int idPhien) {
        Auction p = dsPhienDangChay.remove(idPhien);
        if (p != null) {
            synchronized (p) {
                AuctionDao.AuctionNotificationTargets targets =
                        auctionDao.layNguoiNhanThongBaoKetThuc(idPhien);
                // Có người thắng -> FINISHED, không có -> CANCELED
                AuctionStatus statusMoi = targets != null && targets.winnerId != null
                        ? AuctionStatus.FINISHED
                        : AuctionStatus.CANCELED;
                p.setStatus(statusMoi);
                auctionDao.capNhatTrangThai(idPhien, statusMoi.name());
                if (statusMoi == AuctionStatus.FINISHED) {
                    logger.info("Dong phien {} FINISHED. winnerId={}, sellerId={}",
                            idPhien,
                            targets != null ? targets.winnerId : null,
                            targets != null ? targets.sellerId : null);
                    AuctionSettlementNotifier.guiThongBaoKetThucPhien(targets);
                    henGioQuaHanThanhToan(idPhien, targets);
                }

                // Xóa phiên khỏi bộ nhớ và dọn dẹp tài nguyên liên quan
                dsNguoiTheoDoi.remove(idPhien);
                tasksDongPhien.remove(idPhien);
            }
        }
    }

    // Xử lý đặt giá: kiểm tra điều kiện, chống sniping, lưu DB và kích hoạt auto-bid
    public boolean xuLyDatGia(int idPhien, BidTransaction giaoDich) throws InvalidBidException, AuctionClosedException {
        Auction phien = dsPhienDangChay.get(idPhien);
        if (phien == null) {
            Auction tuDb = auctionDao.layPhienTheoId(idPhien);
            if (tuDb == null) {
                throw new InvalidBidException("Phiên đấu giá không tồn tại.");
            }
            AuctionStatus status = tuDb.getStoredStatus();
            if (status == AuctionStatus.OPEN) {
                throw new InvalidBidException("Phiên chưa mở, chưa thể đặt giá.");
            }
            if (status == AuctionStatus.RUNNING) {
                dsPhienDangChay.put(tuDb.getId(), tuDb);
                henGioDongPhien(tuDb);
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
                dongPhien(idPhien);
                throw new AuctionClosedException("Phiên đã kết thúc!");
            }

            // Chặn seller tự bid sản phẩm của mình
            if (giaoDich.getBidder().getId() == phien.getItem().getSellerId())
                throw new InvalidBidException("Không được tự bid sản phẩm của mình!");

            long giaHienTai = phien.getCurrentHighestBid();
            long buocGia = phien.getItem().getBidIncrement();

            // Áp dụng đúng 1 công thức bắt buộc cho mọi lượt đặt:
            long giaToiThieu = giaHienTai + buocGia;

            if (giaoDich.getBidAmount() < giaToiThieu) {
                throw new InvalidBidException("Giá đặt tối thiểu: " + giaToiThieu);
            }
            if (coDatGiaDatMuaDut(phien, giaoDich.getBidAmount())) {
                throw new InvalidBidException("Mức giá này đạt giá mua đứt. Hãy xác nhận mua ngay.");
            }

            // Lưu giao dịch vào DB và cập nhật người thắng hiện tại
            if (auctionDao.thucHienGiaoDichDatGia(idPhien, giaoDich)) {
                phien.updateWinner(giaoDich);
                giaHanNeuDatGiaCuoiPhien(phien);
                notifierPool.execute(() -> thongBaoGiaMoi(idPhien, giaoDich)); // Gửi thông báo bất đồng bộ
                kichHoatAutoBid(phien); // Kích hoạt auto-bid để đáp trả nếu cần
                return true;
            }
            return false;
        }
    }

    /** Mua đứt luôn chốt ở giá mua đứt trong DB, không lấy số tiền từ client. */
    public BidTransaction xuLyMuaDut(int idPhien, Bidder bidder)
            throws InvalidBidException, AuctionClosedException {
        Auction phien = dsPhienDangChay.get(idPhien);
        if (phien == null) {
            throw new InvalidBidException("Phiên không khả dụng!");
        }

        synchronized (phien) {
            if (!phien.isAcceptingBids()) {
                dongPhien(idPhien);
                throw new AuctionClosedException("Phiên đã kết thúc!");
            }
            if (bidder.getId() == phien.getItem().getSellerId()) {
                throw new InvalidBidException("Không được mua sản phẩm của chính mình!");
            }
            if (phien.getBuyNowPrice() == null || phien.getBuyNowPrice() <= 0) {
                throw new InvalidBidException("Phiên này không hỗ trợ mua đứt.");
            }

            long giaMuaDut = phien.getBuyNowPrice();
            if (giaMuaDut < phien.getCurrentHighestBid() + phien.getItem().getBidIncrement()) {
                throw new InvalidBidException("Giá mua đứt không còn hợp lệ ở thời điểm hiện tại.");
            }

            BidTransaction giaoDich = new BidTransaction(idPhien, bidder, giaMuaDut, LocalDateTime.now());
            if (!auctionDao.thucHienGiaoDichDatGia(idPhien, giaoDich)) {
                throw new InvalidBidException("Đã có người trả giá cao hơn.");
            }

            phien.updateWinner(giaoDich);
            phien.setStatus(AuctionStatus.FINISHED);
            auctionDao.capNhatTrangThai(idPhien, AuctionStatus.FINISHED.name());
            huyLichDongPhien(idPhien);
            notifierPool.execute(() -> thongBaoGiaMoi(idPhien, giaoDich));
            thongBaoTrangThai(idPhien, AuctionStatus.FINISHED);
            AuctionDao.AuctionNotificationTargets targets = auctionDao.layNguoiNhanThongBaoKetThuc(idPhien);
            henGioQuaHanThanhToan(idPhien, targets);
            return giaoDich;
        }
    }

    // Đăng ký auto-bid: đặt giá tự động đến mức tối đa cho phép
    public void dangKyAutoBid(int idPhien, User bidder, long maxBid) throws Exception {
        Auction phien = dsPhienDangChay.get(idPhien);
        if (phien == null) throw new Exception("Phiên không khả dụng!");

        synchronized (phien) {
            if (!phien.isAcceptingBids()) {
                dongPhien(idPhien);
                throw new AuctionClosedException("Phiên đã kết thúc!");
            }

            // Chặn seller cài auto-bid cho sản phẩm của mình
            if (bidder.getId() == phien.getItem().getSellerId()) {
                throw new Exception("Seller không thể đăng ký Auto-bid cho sản phẩm của mình!");
            }

            Queue<AutoBidConfig> queue = phien.getAutoBidders();
            queue.removeIf(bot -> bot.getBidder().getId() == bidder.getId());

            phien.addAutoBidConfig(new AutoBidConfig(bidder, maxBid));

            boolean luuThanhCong = auctionDao.luuHoacCapNhatAutoBid(idPhien, bidder.getId(), maxBid);
            if (!luuThanhCong) {
                logger.warn("Không thể lưu giá trần auto-bid xuống Database cho userId={}", bidder.getId());
            }

            kichHoatAutoBid(phien);
        }
    }

    // Xử lý auto-bid: bot đại diện người dùng tự động trả giá theo bước giá
    private void kichHoatAutoBid(Auction phien) {
        Queue<AutoBidConfig> queue = phien.getAutoBidders();
        while (!queue.isEmpty()) {
            AutoBidConfig topBot = queue.peek();
            if (phien.getCurrentWinner() != null &&
                    topBot.getBidder().getId() == phien.getCurrentWinner().getId()) {
                break;
            }

            long giaTiepTheo = phien.getCurrentHighestBid() + phien.getItem().getBidIncrement();
            if (coDatGiaDatMuaDut(phien, giaTiepTheo)) {
                break;
            }

            // Bot không đủ tiền theo bước giá tiếp theo -> loại bỏ
            if (giaTiepTheo > topBot.getMaxBid()) {
                queue.poll();
                continue;
            }

            // Tạo giao dịch tự động và thực hiện
            BidTransaction autoTx = new BidTransaction(
                    phien.getId(),
                    (Bidder) topBot.getBidder(),
                    giaTiepTheo,
                    LocalDateTime.now()
            );

            if (auctionDao.thucHienGiaoDichDatGia(phien.getId(), autoTx)) {
                phien.updateWinner(autoTx);
                giaHanNeuDatGiaCuoiPhien(phien);
                notifierPool.execute(() -> thongBaoGiaMoi(phien.getId(), autoTx));
            } else {
                break;
            }
        }
    }

    // Đăng ký observer để nhận thông báo khi có bid mới
    public void dangKyTheoDoi(int idPhien, AuctionObserver obs) {
        dsNguoiTheoDoi.computeIfAbsent(idPhien, k -> new CopyOnWriteArrayList<>()).add(obs);
    }

    // Gửi thông báo đến tất cả observer đang theo dõi phiên
    private void thongBaoGiaMoi(int idPhien, BidTransaction tx) {
        List<AuctionObserver> observers = dsNguoiTheoDoi.get(idPhien);
        if (observers != null) {
            for (AuctionObserver obs : observers) obs.onNewBid(tx);
        }
    }

    public void capNhatTrangThaiSauThanhToan(int idPhien, AuctionStatus status) {
        huyLichQuaHanThanhToan(idPhien);
        Auction phien = layPhienTheoId(idPhien);
        if (phien != null) {
            synchronized (phien) {
                phien.setStatus(status);
                huyLichDongPhien(idPhien);
                thongBaoTrangThai(idPhien, status);
            }
        }
    }

    private boolean coDatGiaDatMuaDut(Auction phien, long giaDat) {
        return phien.getBuyNowPrice() != null
                && phien.getBuyNowPrice() > 0
                && giaDat >= phien.getBuyNowPrice();
    }

    private void giaHanNeuDatGiaCuoiPhien(Auction phien) {
        if (!phien.isAntiSnipingEnabled()) {
            return;
        }

        LocalDateTime endTime = phien.getEndTime();
        LocalDateTime now = LocalDateTime.now(SERVER_ZONE);
        if (endTime == null || now.isBefore(endTime.minusSeconds(30)) || !now.isBefore(endTime)) {
            return;
        }

        phien.setEndTime(endTime.plusSeconds(60));
        if (!auctionDao.capNhatThoiGianKetThuc(phien.getId(), phien.getEndTime())) {
            phien.setEndTime(endTime);
            return;
        }
        henGioDongPhien(phien);
    }

    private void thongBaoTrangThai(int idPhien, AuctionStatus status) {
        List<AuctionObserver> observers = dsNguoiTheoDoi.get(idPhien);
        if (observers != null) {
            for (AuctionObserver obs : observers) {
                obs.onStatusChanged(status);
            }
        }
    }

    private void guiThongBaoKetThucPhien(AuctionDao.AuctionNotificationTargets targets) {
        if (targets == null || targets.winnerId == null) {
            logger.warn("Bo qua gui thong bao ket thuc phien vi thieu winner.");
            return;
        }
        String itemName = targets.itemName == null ? "sản phẩm" : targets.itemName;
        String winnerName = targets.winnerName == null ? "người thắng phiên" : targets.winnerName;
        SystemNotificationManager.getInstance().guiThongBaoRieng(
                targets.auctionId,
                targets.winnerId,
                taoNoiDungThongBaoThanhToan(itemName, targets.auctionId),
                true
        );
        SystemNotificationManager.getInstance().guiThongBaoRieng(
                targets.auctionId,
                targets.sellerId,
                taoNoiDungThongBaoSeller(itemName, targets.auctionId, winnerName),
                false
        );
    }

    private String taoNoiDungThongBaoThanhToan(String itemName, int auctionId) {
        return "Chúc mừng bạn đã chiến thắng phiên đấu giá " + itemName
                + " của phiên ID " + auctionId + ".\n"
                + "Xác nhận thanh toán để chính thức sở hữu sản phẩm.\n\n"
                + "Nếu hủy thanh toán, bạn sẽ chịu phạt 10% tiền đặt giá.";
    }

    private String taoNoiDungThongBaoSeller(String itemName, int auctionId, String winnerName) {
        return "Chúc mừng sản phẩm " + itemName + " phiên " + auctionId
                + " đã được bán thành công, người chiến thắng là " + winnerName + ".";
    }

    private void huyLichDongPhien(int idPhien) {
        ScheduledFuture<?> taskDong = tasksDongPhien.remove(idPhien);
        if (taskDong != null && !taskDong.isDone()) {
            taskDong.cancel(false);
        }
        dsPhienDangChay.remove(idPhien);
    }

    private void henGioQuaHanThanhToan(int auctionId, AuctionDao.AuctionNotificationTargets targets) {
        if (targets == null || targets.winnerId == null) {
            return;
        }
        huyLichQuaHanThanhToan(auctionId);
        ScheduledFuture<?> task = scheduler.schedule(
                () -> tuDongHuyThanhToanQuaHan(auctionId, targets),
                PAYMENT_TIMEOUT_MINUTES,
                TimeUnit.MINUTES
        );
        tasksQuaHanThanhToan.put(auctionId, task);
        logger.info("Da len lich tu dong huy thanh toan cho phien {} sau {} phut.", auctionId, PAYMENT_TIMEOUT_MINUTES);
    }

    private void huyLichQuaHanThanhToan(int auctionId) {
        ScheduledFuture<?> task = tasksQuaHanThanhToan.remove(auctionId);
        if (task != null && !task.isDone()) {
            task.cancel(false);
        }
    }

    private void tuDongHuyThanhToanQuaHan(int auctionId, AuctionDao.AuctionNotificationTargets targets) {
        try {
            if (targets == null || targets.winnerId == null) {
                return;
            }
            com.auction.server.dao.BidderMoneySellerDao.PaymentResult ketQua =
                    new com.auction.server.dao.BidderMoneySellerDao()
                            .quyetToanMuaDut(auctionId, targets.winnerId, false);

            if (!ketQua.success) {
                logger.info("Auto settlement skipped for auction {}: {}", auctionId, ketQua.message);
                return;
            }

            capNhatTrangThaiSauThanhToan(auctionId, AuctionStatus.CANCELED);
            String itemName = ketQua.itemName == null || ketQua.itemName.isBlank()
                    ? "sản phẩm"
                    : ketQua.itemName;

            SystemNotificationManager.getInstance().guiThongBaoRieng(
                    auctionId,
                    targets.winnerId,
                    "Phiên " + auctionId + " đã quá hạn thanh toán. Hệ thống tự động hủy và trừ phí phạt 10% cho sản phẩm " + itemName + ".",
                    false
            );

            if (targets.sellerId > 0) {
                SystemNotificationManager.getInstance().guiThongBaoRieng(
                        auctionId,
                        targets.sellerId,
                        "Bidder đã quá hạn thanh toán ở phiên " + auctionId + ". Hệ thống đã tự động hủy và chuyển phí phạt cho bạn.",
                        false
                );
            }
            logger.info("Auto settlement success for auction {}.", auctionId);
        } catch (Exception e) {
            logger.error("Auto settlement failed for auction {}.", auctionId, e);
        } finally {
            tasksQuaHanThanhToan.remove(auctionId);
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
    public List<Auction> layDanhSachPhienDangChay() {
        return new ArrayList<>(dsPhienDangChay.values());
    }

    /** Lấy chi tiết 1 phiên đang chạy */
    public Auction layPhienTheoId(int idPhien) {
        Auction phien = dsPhienDangChay.get(idPhien);

        if (phien == null) {
            for (Auction a : auctionDao.layDanhSachPhienDangChay()) {
                if (a.getId() == idPhien) {
                    List<AutoBidConfig> bots = auctionDao.layDanhSachAutoBidCuaPhien(a.getId());
                    for(AutoBidConfig bot : bots) {
                        a.addAutoBidConfig(bot);
                    }

                    dsPhienDangChay.put(a.getId(), a);
                    henGioDongPhien(a);
                    return a;
                }
            }
        }
        return phien;
    }

    /** Gỡ Client khỏi danh sách nhận thông báo Real-time (Rời phòng) */
    public void huyTheoDoi(int idPhien, AuctionObserver obs) {
        List<AuctionObserver> observers = dsNguoiTheoDoi.get(idPhien);
        if (observers != null) {
            observers.remove(obs);
        }
    }

    /** Ép buộc đóng phiên đấu giá ngay lập tức (Dành cho Admin) */
    public boolean buocDongPhien(int idPhien) {
        if (dsPhienDangChay.containsKey(idPhien)) {
            dongPhien(idPhien);
            return true;
        }
        return false;
    }
}
