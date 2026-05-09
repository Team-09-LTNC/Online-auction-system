package com.auction.server.manager;

import com.auction.common.model.AuctionStatus;
import com.auction.common.model.bid.Auction;
import com.auction.common.model.bid.BidTransaction;
import com.auction.common.observer.AuctionObserver;
import com.auction.common.exception.InvalidBidException;
import com.auction.server.dao.AuctionDao;
import com.auction.server.dao.BidTransactionDao;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * Quản lý nghiệp vụ đấu giá bằng mô hình Singleton
 */
public class AuctionManager {
    private static volatile AuctionManager instance;

    // Lưu các phiên đang hoạt động trên RAM để truy xuất
    private final Map<Integer, Auction> dsPhienDangChay = new ConcurrentHashMap<>();

    // Danh sách Client để gửi thông báo Real-time
    private final Map<Integer, List<AuctionObserver>> dsNguoiTheoDoi = new ConcurrentHashMap<>();

    private final AuctionDao auctionDao = new AuctionDao();
    private final BidTransactionDao bidTransactionDao = new BidTransactionDao();

    // Luồng tự động quét và đóng phiên mỗi giây
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // Thread Pool xử lý việc gửi thông báo mà không gây nghẽn luồng chính
    private final ExecutorService notifierPool = Executors.newFixedThreadPool(50);

    private AuctionManager() {
        // Nạp dữ liệu từ DB lên RAM khi khởi tạo
        for (Auction a : auctionDao.layDanhSachPhienDangChay()) {
            dsPhienDangChay.put(a.getId(), a);
        }
        scheduler.scheduleAtFixedRate(this::quetVaDongPhienHetHan, 0, 1, TimeUnit.SECONDS);
    }

    public static AuctionManager getInstance() {
        if (instance == null) {
            synchronized (AuctionManager.class) {
                if (instance == null) instance = new AuctionManager();
            }
        }
        return instance;
    }

    /**
     * Xử lý khi một Bidder đặt giá mới.
     * Sử dụng synchronized để tránh tranh chấp dữ liệu (Race Condition).
     */
    public boolean xuLyDatGia(int idPhien, BidTransaction giaoDich) throws InvalidBidException {
        Auction phien = dsPhienDangChay.get(idPhien);
        if (phien == null) throw new InvalidBidException("Phiên này hiện không khả dụng!");

        synchronized (phien) {
            // 1. Kiểm tra trạng thái phiên đấu giá
            if (phien.getStatus() != AuctionStatus.RUNNING)
                throw new InvalidBidException("Phiên đấu giá hiện không diễn ra!");

            // 2. Chặn Seller tự nâng giá sản phẩm của chính mình
            if (giaoDich.getBidder().getId() == phien.getItem().getSellerId())
                throw new InvalidBidException("Bạn không được phép tự đấu giá sản phẩm của mình!");

            // 3. Tính toán giá tối thiểu dựa trên Bước giá (Bid Increment)
            long giaHienTai = phien.getCurrentHighestBid();
            long buocGia = phien.getItem().getBidIncrement();

            // Nếu chưa có ai bid, giá tối thiểu là giá khởi điểm. Nếu có rồi, phải cộng thêm bước giá
            long giaToiThieu = phien.getBidHistory().isEmpty()
                    ? phien.getItem().getStartingPrice()
                    : (giaHienTai + buocGia);

            if (giaoDich.getBidAmount() < giaToiThieu) {
                throw new InvalidBidException("Giá đặt tối thiểu (bước giá " + buocGia + ") là: " + giaToiThieu);
            }

            // 4. Kiểm tra điều kiện tiền cọc (10% giá khởi điểm)
            long tienCocYeuCau = (long) (phien.getItem().getStartingPrice() * 0.1);
            if (giaoDich.getBidder().getBalance() < tienCocYeuCau) {
                throw new InvalidBidException("Bạn cần số dư tối thiểu " + tienCocYeuCau + " làm tiền cọc!");
            }

            // 5. Cập nhật RAM & Thuật toán Anti-sniping (Gia hạn nếu bid ở 30s cuối)
            phien.updateWinner(giaoDich);
            if (phien.getEndTime().minusSeconds(30).isBefore(LocalDateTime.now())) {
                phien.setEndTime(phien.getEndTime().plusMinutes(1));
                auctionDao.capNhatThoiGianKetThuc(phien.getId(), phien.getEndTime());
                System.out.println(">>> Gia hạn phiên #" + phien.getId() + " thêm 1 phút.");
            }

            // 6. Lưu vào DB và thông báo Real-time cho các Client khác
            if (auctionDao.capNhatGiaVaNguoiDanDau(idPhien, giaoDich.getBidAmount(), giaoDich.getBidder().getId())) {
                bidTransactionDao.luuLichSuDatGia(idPhien, giaoDich.getBidder().getId(), giaoDich.getBidAmount());
                notifierPool.execute(() -> thongBaoGiaMoi(idPhien, giaoDich));
                return true;
            }
            return false;
        }
    }

    /**
     * Kết thúc phiên đấu giá và giải phóng bộ nhớ RAM
     */
    public void dongPhien(int id) {
        Auction p = dsPhienDangChay.get(id);
        if (p != null) {
            synchronized (p) {
                // Xác định trạng thái cuối cùng dựa trên việc có người thắng hay không
                AuctionStatus statusMoi = (p.getCurrentWinner() != null) ? AuctionStatus.FINISHED : AuctionStatus.CANCELED;
                p.setStatus(statusMoi);
                auctionDao.capNhatTrangThai(id, statusMoi.name());
                dsPhienDangChay.remove(id); // Gỡ khỏi RAM để tối ưu hiệu năng
                dsNguoiTheoDoi.remove(id);
            }
        }
    }

    private void quetVaDongPhienHetHan() {
        List<Integer> dsHetHan = auctionDao.layDanhSachPhienHetHan();
        for (int id : dsHetHan) dongPhien(id);
    }

    /**
     * Gửi thông báo cập nhật giá tới các Client đang theo dõi phiên này[cite: 94, 95].
     */
    private void thongBaoGiaMoi(int idPhien, BidTransaction tx) {
        List<AuctionObserver> observers = dsNguoiTheoDoi.get(idPhien);
        if (observers != null) {
            for (AuctionObserver obs : observers) obs.onNewBid(tx);
        }
    }

    public void dangKyTheoDoi(int idPhien, AuctionObserver nguoiTheoDoi) {
        dsNguoiTheoDoi.computeIfAbsent(idPhien, k -> new CopyOnWriteArrayList<>()).add(nguoiTheoDoi);
    }
}