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
 * Xử lý đồng thời, Anti-sniping và tự động dọn dẹp các phiên hết hạn
 */
public class AuctionManager {
    private static volatile AuctionManager instance;
    private final Map<Integer, Auction> danhSachPhienDangChay;
    private final Map<Integer, List<AuctionObserver>> danhSachNguoiTheoDoi;
    private final AuctionDao auctionDao;
    private final BidTransactionDao bidTransactionDao;

    // Bộ lập lịch chạy ngầm để kiểm tra phiên hết hạn
    private final ScheduledExecutorService boLapLichKiemTra;

    private AuctionManager() {
        this.danhSachPhienDangChay = new ConcurrentHashMap<>();
        this.danhSachNguoiTheoDoi = new ConcurrentHashMap<>();
        this.auctionDao = new AuctionDao();
        this.bidTransactionDao = new BidTransactionDao();

        // Khởi tạo luồng ngầm chạy mỗi giây 1 lần
        this.boLapLichKiemTra = Executors.newSingleThreadScheduledExecutor();
        khoiDongLuongKiemTra();
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
     * Kích hoạt bộ đếm thời gian liên tục quét các phiên đấu giá
     */
    private void khoiDongLuongKiemTra() {
        boLapLichKiemTra.scheduleAtFixedRate(this::kiemTraVaDongPhienHetHan, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * Tự động đóng các phiên đã qua thời gian kết thúc
     */
    private void kiemTraVaDongPhienHetHan() {
        List<Integer> dsHetHan = auctionDao.layDanhSachPhienHetHan();
        for (int idPhien : dsHetHan) {
            dongPhienDauGia(idPhien);
        }
    }

    /**
     * Logic chốt kết quả và thông báo người chiến thắng
     */
    private void dongPhienDauGia(int idPhien) {
        Auction phien = danhSachPhienDangChay.get(idPhien);
        if (phien != null) {
            synchronized (phien) {
                if (AuctionStatus.RUNNING.equals(phien.getStatus())) {
                    phien.setStatus(AuctionStatus.FINISHED);
                    auctionDao.capNhatTrangThai(idPhien, "FINISHED");

                    // Tại đây có thể gọi thêm lớp BidderSellerMoney để thanh toán

                    System.out.println("Phiên đấu giá #" + idPhien + " đã kết thúc thành công");
                    // Code thông báo cho người dùng thắng cuộc sẽ được bổ sung ở hàm này

                    xoaPhienDauGia(idPhien);
                }
            }
        }
    }

    /**
     * Xử lý giao dịch đặt giá có kèm thuật toán Anti-sniping
     */
    public boolean xuLyDatGia(int idPhien, BidTransaction giaoDich) {
        Auction phien = danhSachPhienDangChay.get(idPhien);
        if (phien == null) return false;

        synchronized (phien) {
            try {
                if (phien.processBidLogic(giaoDich)) {
                    kiemTraVaGiaHanPhien(phien);

                    boolean thanhCong = auctionDao.capNhatGiaVaNguoiDanDau(
                            idPhien, giaoDich.getBidAmount(), giaoDich.getBidder().getId()
                    );

                    if (thanhCong) {
                        bidTransactionDao.luuLichSuDatGia(
                                idPhien, giaoDich.getBidder().getId(), giaoDich.getBidAmount()
                        );
                        thongBaoGiaMoi(idPhien, giaoDich);
                        return true;
                    }
                }
            } catch (InvalidBidException e) {
                System.err.println("Lỗi đặt giá: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Chống Sniping: Tự động cộng thêm thời gian nếu có biến động sát giờ
     */
    private void kiemTraVaGiaHanPhien(Auction phien) {
        LocalDateTime hienTai = LocalDateTime.now();
        if (phien.getEndTime().minusSeconds(30).isBefore(hienTai)) {
            phien.setEndTime(phien.getEndTime().plusMinutes(1));
            auctionDao.capNhatThoiGianKetThuc(phien.getId(), phien.getEndTime());
        }
    }

    /**
     * Client gọi hàm này khi mở xem chi tiết một sản phẩm
     */
    public void dangKyNguoiTheoDoi(int idPhien, AuctionObserver nguoiTheoDoi) {
        danhSachNguoiTheoDoi.computeIfAbsent(idPhien, k -> new CopyOnWriteArrayList<>()).add(nguoiTheoDoi);
    }
    /**
     * Bắn dữ liệu cập nhật qua Socket bằng một luồng phụ
     */
    private void thongBaoGiaMoi(int idPhien, BidTransaction giaoDich) {
        List<AuctionObserver> dsTheoDoi = danhSachNguoiTheoDoi.get(idPhien);
        if (dsTheoDoi != null) {
            for (AuctionObserver client : dsTheoDoi) {
                new Thread(() -> client.onNewBid(giaoDich)).start();
            }
        }
    }

    /**
     * Đưa phiên đấu giá vào bộ nhớ đệm
     */
    public void themPhienDauGia(Auction phien) {
        danhSachPhienDangChay.put(phien.getId(), phien);
    }

    /**
     * Dọn dẹp phiên đấu giá trên RAM khi đã kết thúc
     */
    public void xoaPhienDauGia(int idPhien) {
        danhSachPhienDangChay.remove(idPhien);
        danhSachNguoiTheoDoi.remove(idPhien);
    }

    /**
     * Lấy phiên đấu giá từ bộ nhớ đệm
     */
    public Auction layPhienDauGia(int idPhien) {
        return danhSachPhienDangChay.get(idPhien);
    }
}