package com.auction.server.manager;

import com.auction.common.enums.AuctionStatus;
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
 * AuctionManager tối ưu: Xử lý đồng thời, Anti-sniping và Thread-pool
 */
public class AuctionManager {
    private static volatile AuctionManager instance;
    private final Map<Integer, Auction> danhSachPhienDangChay;
    private final Map<Integer, List<AuctionObserver>> danhSachNguoiTheoDoi;
    private final AuctionDao auctionDao;
    private final BidTransactionDao bidTransactionDao;

    private final ScheduledExecutorService boLapLichKiemTra;
    private final ExecutorService notificationThreadPool; // Dùng để bắn thông báo

    private AuctionManager() {
        this.danhSachPhienDangChay = new ConcurrentHashMap<>();
        this.danhSachNguoiTheoDoi = new ConcurrentHashMap<>();
        this.auctionDao = new AuctionDao();
        this.bidTransactionDao = new BidTransactionDao();

        this.notificationThreadPool = Executors.newFixedThreadPool(50);
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

    private void khoiDongLuongKiemTra() {
        // Quét mỗi giây để đảm bảo tính realtime
        boLapLichKiemTra.scheduleAtFixedRate(this::kiemTraVaDongPhienHetHan, 0, 1, TimeUnit.SECONDS);
    }

    private void kiemTraVaDongPhienHetHan() {
        try {
            List<Integer> dsHetHan = auctionDao.layDanhSachPhienHetHan();
            for (int idPhien : dsHetHan) {
                dongPhienDauGia(idPhien);
            }
        } catch (Exception e) {
            System.err.println("Lỗi quét phiên hết hạn: " + e.getMessage());
        }
    }

    public void dongPhienDauGia(int idPhien) {
        Auction phien = danhSachPhienDangChay.get(idPhien);
        if (phien != null) {
            synchronized (phien) { // Khóa trên thực thể phiên để tránh race condition
                if (phien.getStatus() == AuctionStatus.RUNNING) {
                    phien.setStatus(AuctionStatus.FINISHED);
                    auctionDao.capNhatTrangThai(idPhien, "FINISHED");

                    System.out.println(">>> Phiên #" + idPhien + " KẾT THÚC.");
                    xoaPhienDauGia(idPhien);
                }
            }
        }
    }
    public boolean xuLyDatGia(int idPhien, BidTransaction giaoDich) {
        Auction phien = danhSachPhienDangChay.get(idPhien);
        if (phien == null || phien.getStatus() != AuctionStatus.RUNNING) return false;

        synchronized (phien) {
            try {
                // Kiểm tra logic giá
                if (phien.processBidLogic(giaoDich)) {
                    // Anti-sniping
                    kiemTraVaGiaHanPhien(phien);
                    // Lưu Database nếu thành công
                    boolean thanhCong = auctionDao.capNhatGiaVaNguoiDanDau(
                            idPhien, giaoDich.getBidAmount(), giaoDich.getBidder().getId()
                    );
                    if (thanhCong) {
                        bidTransactionDao.luuLichSuDatGia(idPhien, giaoDich.getBidder().getId(), giaoDich.getBidAmount());
                        // Thông báo Realtime (Sử dụng Pool)
                        thongBaoGiaMoi(idPhien, giaoDich);
                        return true;
                    }
                }
            } catch (InvalidBidException e) {
                System.err.println("Giá thầu không hợp lệ: " + e.getMessage());
            }
            return false;
        }
    }

    private void kiemTraVaGiaHanPhien(Auction phien) {
        LocalDateTime bayGio = LocalDateTime.now();
        // Nếu đặt giá trong 30s cuối, gia hạn thêm 1 phút
        if (phien.getEndTime().minusSeconds(30).isBefore(bayGio)) {
            phien.setEndTime(phien.getEndTime().plusMinutes(1));
            auctionDao.capNhatThoiGianKetThuc(phien.getId(), phien.getEndTime());
            System.out.println("Anti-sniping: Gia hạn phiên #" + phien.getId());
        }
    }

    public void dangKyNguoiTheoDoi(int idPhien, AuctionObserver nguoiTheoDoi) {
        danhSachNguoiTheoDoi.computeIfAbsent(idPhien, k -> new CopyOnWriteArrayList<>()).add(nguoiTheoDoi);
    }

    private void thongBaoGiaMoi(int idPhien, BidTransaction giaoDich) {
        List<AuctionObserver> dsTheoDoi = danhSachNguoiTheoDoi.get(idPhien);
        if (dsTheoDoi != null) {
            for (AuctionObserver client : dsTheoDoi) {
                notificationThreadPool.execute(() -> client.onNewBid(giaoDich));
            }
        }
    }

    public void xoaPhienDauGia(int idPhien) {
        danhSachPhienDangChay.remove(idPhien);
        danhSachNguoiTheoDoi.remove(idPhien);
    }

    public void themPhienDauGia(Auction phien) {
        danhSachPhienDangChay.put(phien.getId(), phien);
    }

    // Đảm bảo đóng pool khi server stop
    public void shutdown() {
        boLapLichKiemTra.shutdown();
        notificationThreadPool.shutdown();
    }
}