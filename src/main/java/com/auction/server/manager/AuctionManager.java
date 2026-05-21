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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;

public class AuctionManager {
    private static volatile AuctionManager instance;

    private final Map<Integer, Auction> dsPhienDangChay = new ConcurrentHashMap<>();
    private final Map<Integer, List<AuctionObserver>> dsNguoiTheoDoi = new ConcurrentHashMap<>();
    private final Map<Integer, ScheduledFuture<?>> tasksDongPhien = new ConcurrentHashMap<>();
    private final Map<Integer, ScheduledFuture<?>> tasksMoPhien = new ConcurrentHashMap<>();

    private final AuctionDao auctionDao = new AuctionDao();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final ExecutorService notifierPool = Executors.newFixedThreadPool(50);

    //tải phiên từ DB và lên lịch khi khởi động
    private AuctionManager() {
        // Khôi phục các phiên đang chạy
        for (Auction a : auctionDao.layDanhSachPhienDangChay()) {
            dsPhienDangChay.put(a.getId(), a);
            henGioDongPhien(a);
        }
        // Lên lịch mở các phiên đang chờ
        for (Auction a : auctionDao.layDanhSachPhienChoMo()) {
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

    // Lên lịch mở phiên vào thời điểm startTime (Chính xác đến mili-giây)
    public void henGioMoPhien(Auction phien) {
        ScheduledFuture<?> taskCu = tasksMoPhien.get(phien.getId());
        if (taskCu != null && !taskCu.isDone()) taskCu.cancel(false);

        long delay = java.time.Duration.between(LocalDateTime.now(), phien.getStartTime()).toMillis();

        if (delay <= 0) {
            thucThiMoPhien(phien);
        } else {
            ScheduledFuture<?> taskMoi = scheduler.schedule(() -> thucThiMoPhien(phien), delay, TimeUnit.MILLISECONDS);
            tasksMoPhien.put(phien.getId(), taskMoi); // Lưu lại thẻ quản lý
        }
    }

    // Cập nhật trạng thái phiên thành RUNNING và lên lịch đóng
    private void thucThiMoPhien(Auction phien) {
        if (auctionDao.capNhatTrangThai(phien.getId(), AuctionStatus.RUNNING.name())) {
            phien.setStatus(AuctionStatus.RUNNING);
            dsPhienDangChay.put(phien.getId(), phien);

            tasksMoPhien.remove(phien.getId());

            System.out.println("[AuctionManager] Đã TỰ ĐỘNG MỞ phiên đấu giá ID: " + phien.getId());
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

        // BẢN VÁ LỖI CỰC KỲ QUAN TRỌNG:
        // Thay getStartTime() thành getEndTime() để tính toán chính xác thời gian đóng phiên.
        long delay = java.time.Duration.between(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")), phien.getEndTime()).toMillis();

        if (delay <= 0) {
            dongPhien(phien.getId()); // Đã quá giờ thì đóng luôn
        } else {
            ScheduledFuture<?> taskMoi = scheduler.schedule(() -> dongPhien(phien.getId()), delay, TimeUnit.MILLISECONDS);
            tasksDongPhien.put(phien.getId(), taskMoi);
        }
    }

    // Đóng phiên: xác định trạng thái FINISHED/CANCELED, dọn dẹp tài nguyên
    private void dongPhien(int idPhien) {
        Auction p = dsPhienDangChay.get(idPhien);
        if (p != null) {
            synchronized (p) {
                // Có người thắng -> FINISHED, không có -> CANCELED
                AuctionStatus statusMoi = (p.getCurrentWinner() != null) ? AuctionStatus.FINISHED : AuctionStatus.CANCELED;
                p.setStatus(statusMoi);
                auctionDao.capNhatTrangThai(idPhien, statusMoi.name());

                // Xóa phiên khỏi bộ nhớ và dọn dẹp tài nguyên liên quan
                dsPhienDangChay.remove(idPhien);
                dsNguoiTheoDoi.remove(idPhien);
                tasksDongPhien.remove(idPhien);
            }
        }
    }

    // Xử lý đặt giá: kiểm tra điều kiện, chống sniping, lưu DB và kích hoạt auto-bid
    public boolean xuLyDatGia(int idPhien, BidTransaction giaoDich) throws InvalidBidException, AuctionClosedException {
        Auction phien = dsPhienDangChay.get(idPhien);
        if (phien == null) throw new InvalidBidException("Phiên không khả dụng!");

        synchronized (phien) { // Đồng bộ trên phiên để tránh race condition
            if (!phien.isAcceptingBids()) throw new AuctionClosedException("Phiên đã kết thúc!");

            // Chặn seller tự bid sản phẩm của mình
            if (giaoDich.getBidder().getId() == phien.getItem().getSellerId())
                throw new InvalidBidException("Không được tự bid sản phẩm của mình!");

            long giaHienTai = phien.getCurrentHighestBid();
            long buocGia = phien.getItem().getBidIncrement();

            // Áp dụng đúng 1 công thức bắt buộc cho mọi lượt đặt:
            // Giá tối thiểu = Giá cao nhất hiện tại + Bước giá
            long giaToiThieu = giaHienTai + buocGia;

            if (giaoDich.getBidAmount() < giaToiThieu) {
                throw new InvalidBidException("Giá đặt tối thiểu: " + giaToiThieu);
            }

            // Anti-sniping: nếu bid trong 30 giây cuối, gia hạn thêm 60 giây
            if (phien.getEndTime().minusSeconds(30).isBefore(LocalDateTime.now())) {
                phien.extendEndTime(60);
                auctionDao.capNhatThoiGianKetThuc(phien.getId(), phien.getEndTime());
                henGioDongPhien(phien); // Cập nhật lại lịch đóng với thời gian mới
            }

            // Lưu giao dịch vào DB và cập nhật người thắng hiện tại
            if (auctionDao.thucHienGiaoDichDatGia(idPhien, giaoDich)) {
                phien.updateWinner(giaoDich);
                notifierPool.execute(() -> thongBaoGiaMoi(idPhien, giaoDich)); // Gửi thông báo bất đồng bộ
                kichHoatAutoBid(phien); // Kích hoạt auto-bid để đáp trả nếu cần
                return true;
            }
            return false;
        }
    }

    // Đăng ký auto-bid: đặt giá tự động đến mức tối đa cho phép
    public void dangKyAutoBid(int idPhien, User bidder, long maxBid) throws Exception {
        Auction phien = dsPhienDangChay.get(idPhien);
        if (phien == null) throw new Exception("Phiên không khả dụng!");

        synchronized (phien) {
            // Chặn seller cài auto-bid cho sản phẩm của mình
            if (bidder.getId() == phien.getItem().getSellerId()) {
                throw new Exception("Seller không thể đăng ký Auto-bid cho sản phẩm của mình!");
            }
            phien.addAutoBidConfig(new AutoBidConfig(bidder, maxBid));
            kichHoatAutoBid(phien); // Kích hoạt ngay để cạnh tranh với giá hiện tại
        }
    }

    // Xử lý auto-bid: bot đại diện người dùng tự động trả giá theo bước giá
    private void kichHoatAutoBid(Auction phien) {
        Queue<AutoBidConfig> queue = phien.getAutoBidders();
        while (!queue.isEmpty()) {
            AutoBidConfig topBot = queue.peek();
            // Nếu bot đang dẫn đầu, dừng lại (không tự đấu với chính mình)
            if (phien.getCurrentWinner() != null &&
                    topBot.getBidder().getId() == phien.getCurrentWinner().getId()) {
                break;
            }

            long giaTiepTheo = phien.getCurrentHighestBid() + phien.getItem().getBidIncrement();

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

        //Nếu RAM không có, móc xuống Database tìm lại
        if (phien == null) {
            for (Auction a : auctionDao.layDanhSachPhienDangChay()) {
                if (a.getId() == idPhien) {
                    dsPhienDangChay.put(a.getId(), a); // Nạp lại vào RAM
                    henGioDongPhien(a); // Lên dây cót đếm ngược luôn
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