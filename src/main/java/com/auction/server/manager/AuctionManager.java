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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;

public class AuctionManager {
    // Singleton: chỉ có 1 instance duy nhất
    private static volatile AuctionManager instance;

    // Lưu các phiên đấu giá đang chạy
    private final Map<Integer, Auction> dsPhienDangChay = new ConcurrentHashMap<>();
    // Lưu danh sách observer theo dõi từng phiên
    private final Map<Integer, List<AuctionObserver>> dsNguoiTheoDoi = new ConcurrentHashMap<>();

    // Quản lý tác vụ đóng phiên để tránh trùng lặp khi gia hạn (Anti-sniping)
    private final Map<Integer, ScheduledFuture<?>> tasksDongPhien = new ConcurrentHashMap<>();

    private final AuctionDao auctionDao = new AuctionDao();
    // Thread pool lên lịch đóng/mở phiên
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    // Thread pool gửi thông báo đến người theo dõi (không block luồng chính)
    private final ExecutorService notifierPool = Executors.newFixedThreadPool(50);

    // Constructor private: tải phiên từ DB và lên lịch khi khởi động
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

    // Lên lịch mở phiên vào thời điểm startTime
    public void henGioMoPhien(Auction phien) {
        long delay = ChronoUnit.SECONDS.between(LocalDateTime.now(), phien.getStartTime());
        if (delay <= 0) thucThiMoPhien(phien); // Đã đến giờ thì mở luôn
        else scheduler.schedule(() -> thucThiMoPhien(phien), delay, TimeUnit.SECONDS);
    }

    // Cập nhật trạng thái phiên thành RUNNING và lên lịch đóng
    private void thucThiMoPhien(Auction phien) {
        if (auctionDao.capNhatTrangThai(phien.getId(), AuctionStatus.RUNNING.name())) {
            phien.setStatus(AuctionStatus.RUNNING);
            dsPhienDangChay.put(phien.getId(), phien);
            henGioDongPhien(phien); // Bắt đầu đếm ngược đến giờ đóng
        }
    }

    // Lên lịch đóng phiên, hủy task cũ nếu có (dùng cho Anti-sniping khi gia hạn)
    public void henGioDongPhien(Auction phien) {
        // Hủy tác vụ đóng phiên cũ nếu đang chạy (quan trọng khi gia hạn thời gian)
        ScheduledFuture<?> taskCu = tasksDongPhien.get(phien.getId());
        if (taskCu != null && !taskCu.isDone()) {
            taskCu.cancel(false);
        }

        long delay = ChronoUnit.SECONDS.between(LocalDateTime.now(), phien.getEndTime());
        if (delay <= 0) {
            dongPhien(phien.getId()); // Đã quá giờ thì đóng luôn
        } else {
            ScheduledFuture<?> taskMoi = scheduler.schedule(() -> dongPhien(phien.getId()), delay, TimeUnit.SECONDS);
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
            // Giá tối thiểu: giá khởi điểm (nếu chưa có ai bid) hoặc giá cao nhất + bước giá
            long giaToiThieu = (phien.getCurrentWinner() == null)
                    ? phien.getItem().getStartingPrice()
                    : (giaHienTai + phien.getItem().getBidIncrement());

            if (giaoDich.getBidAmount() < giaToiThieu)
                throw new InvalidBidException("Giá đặt tối thiểu: " + giaToiThieu);

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
        // Dùng CopyOnWriteArrayList để thread-safe khi duyệt gửi thông báo
        dsNguoiTheoDoi.computeIfAbsent(idPhien, k -> new CopyOnWriteArrayList<>()).add(obs);
    }

    // Gửi thông báo đến tất cả observer đang theo dõi phiên
    private void thongBaoGiaMoi(int idPhien, BidTransaction tx) {
        List<AuctionObserver> observers = dsNguoiTheoDoi.get(idPhien);
        if (observers != null) {
            for (AuctionObserver obs : observers) obs.onNewBid(tx);
        }
    }
    /** Lấy danh sách các phiên đang chạy (từ bộ nhớ tạm ConcurrentHashMap) */
    public List<Auction> layDanhSachPhienDangChay() {
        return new ArrayList<>(dsPhienDangChay.values());
    }

    /** Lấy chi tiết 1 phiên đang chạy */
    public Auction layPhienTheoId(int idPhien) {
        return dsPhienDangChay.get(idPhien);
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
            dongPhien(idPhien); // Gọi hàm private dongPhien() bạn đã viết sẵn
            return true;
        }
        return false;
    }
}