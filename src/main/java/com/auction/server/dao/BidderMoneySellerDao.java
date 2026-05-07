package com.auction.server.dao;

import com.auction.server.utils.DatabaseConnection;
import java.sql.*;

/**
 * Nhiệm vụ: Xử lý giao dịch tiền giữa Bidder và Seller một cách an toàn.
 */
public class BidderMoneySellerDao {

    /**
     * Chuyển tiền từ Bidder (người thắng) sang Seller.
     */
    public boolean thanhToanPhienDauGia(int idBidder, int idSeller, long soTien) {
        // Sử dụng try-with-resources
        try (Connection conn = DatabaseConnection.getConnection()) {

            //Tắt AutoCommit để bắt đầu một Transaction thủ công
            conn.setAutoCommit(false);

            try {
                // Trừ tiền Bidder (Chỉ trừ nếu đủ số dư)
                String sqlTruTien = "UPDATE users SET balance = balance - ? " +
                        "WHERE id = ? AND role = 'BIDDER' AND balance >= ?";
                try (PreparedStatement p1 = conn.prepareStatement(sqlTruTien)) {
                    p1.setLong(1, soTien); // Sử dụng setLong
                    p1.setInt(2, idBidder);
                    p1.setLong(3, soTien);

                    if (p1.executeUpdate() == 0) {
                        conn.rollback(); // Không đủ điều kiện (thiếu tiền) thì hủy toàn bộ
                        return false;
                    }
                }

                // Cộng tiền Seller
                String sqlCongTien = "UPDATE users SET balance = balance + ? " +
                        "WHERE id = ? AND role = 'SELLER'";
                try (PreparedStatement p2 = conn.prepareStatement(sqlCongTien)) {
                    p2.setLong(1, soTien); // Sử dụng setLong
                    p2.setInt(2, idSeller);
                    p2.executeUpdate();
                }

                // Nếu cả hai bước thành công, xác nhận thay đổi vĩnh viễn vào DB
                conn.commit();
                return true;

            } catch (SQLException e) {
                // Nếu có bất kỳ lỗi SQL nào, hoàn tác (rollback) để tránh mất tiền oan
                conn.rollback();
                System.err.println("[Transaction Error] Lỗi thanh toán: " + e.getMessage());
                return false;
            } finally {
                // Trả trạng thái AutoCommit về mặc định trước khi trả kết nối về Pool
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("[Pool Error] Không thể lấy kết nối: " + e.getMessage());
            return false;
        }
    }
}