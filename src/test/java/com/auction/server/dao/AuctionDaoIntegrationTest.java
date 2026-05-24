package com.auction.server.dao;

import com.auction.common.model.bid.Auction;
import com.auction.common.model.bid.BidTransaction;
import com.auction.common.model.user.Bidder;
import com.auction.common.model.user.Seller;
import com.auction.common.model.item.OtherItem;
import com.auction.server.db.DatabaseConnection;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionDaoIntegrationTest extends DaoIntegrationTestSupport {

    private final AuctionDao auctionDao = new AuctionDao();
    private final UserDao userDao = new UserDao();
    private final ItemDao itemDao = new ItemDao();

    @Test
    void capNhatGiaHienTaiThanhCong_KhiGiaoDichDatGiaHopLe() {
        int maNguoiBan = -1;
        int maNguoiMua = -1;
        int maSanPham = -1;
        int maPhienDauGia = -1;

        try {
            long thoiGianHienTai = System.currentTimeMillis();
            
            Seller nguoiBan = new Seller("seller_" + thoiGianHienTai, "pass", "Test Seller");
            userDao.luuNguoiDung(nguoiBan);
            maNguoiBan = userDao.timTheoTenDangNhap(nguoiBan.getUsername()).get().getId();

            Bidder nguoiMua = new Bidder("bidder_" + thoiGianHienTai, "pass", "Test Bidder");
            userDao.luuNguoiDung(nguoiMua);
            maNguoiMua = userDao.timTheoTenDangNhap(nguoiMua.getUsername()).get().getId();
            nguoiMua.setId(maNguoiMua);

            OtherItem sanPham = new OtherItem("Test Item", maNguoiBan, "Desc", 100000L, "OTHER", "");
            sanPham.setCategory("OTHER"); 
            
            maSanPham = itemDao.luuSanPham(sanPham);
            
            LocalDateTime thoiGianBatDau = LocalDateTime.now().minusMinutes(10);
            LocalDateTime thoiGianKetThuc = LocalDateTime.now().plusHours(1);
            boolean taoPhienThanhCong = auctionDao.taoPhienDauGia(maSanPham, 100000L, thoiGianBatDau, thoiGianKetThuc);
            assertTrue(taoPhienThanhCong, "Phiên đấu giá phải được tạo thành công vào Database");
            
            Auction phienDauGiaTest = auctionDao.layPhienTheoItemId(maSanPham);
            assertNotNull(phienDauGiaTest, "Phiên đấu giá không được null");
            maPhienDauGia = phienDauGiaTest.getId();

            long mucGiaTiepTheo = phienDauGiaTest.getCurrentHighestBid() + 50000L;
            BidTransaction giaoDich = new BidTransaction(maPhienDauGia, nguoiMua, mucGiaTiepTheo, LocalDateTime.now());
            
            boolean giaoDichThanhCong = auctionDao.thucHienGiaoDichDatGia(maPhienDauGia, giaoDich);

            assertTrue(giaoDichThanhCong, "Giao dịch đặt giá phải thành công và trả về true");
            
            Auction phienDauGiaDaCapNhat = auctionDao.layPhienTheoId(maPhienDauGia);
            assertNotNull(phienDauGiaDaCapNhat);
            assertEquals(mucGiaTiepTheo, phienDauGiaDaCapNhat.getCurrentHighestBid(), "Giá hiện tại của phiên phải được cập nhật đúng");

        } finally {
            if (maSanPham != -1) xoaSanPhamKhoiDatabase(maSanPham);
            if (maNguoiMua != -1) xoaNguoiDungKhoiDatabase(maNguoiMua);
            if (maNguoiBan != -1) xoaNguoiDungKhoiDatabase(maNguoiBan);
        }
    }

    /**
     * Xóa người dùng 
     */
    private void xoaNguoiDungKhoiDatabase(int maNguoiDung) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, maNguoiDung);
            pstmt.executeUpdate();
        } catch (SQLException ignored) {}
    }

    /**
     * Xóa sản phẩm
     */
    private void xoaSanPhamKhoiDatabase(int maSanPham) {
        String sql = "DELETE FROM items WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, maSanPham);
            pstmt.executeUpdate();
        } catch (SQLException ignored) {}
    }
}