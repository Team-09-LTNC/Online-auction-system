package com.auction.server.dao;

import com.auction.server.db.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * Nhiệm vụ: Xử lý giao dịch tiền giữa Bidder và Seller một cách an toàn.
 */
public class BidderMoneySellerDao {
    private static final Logger logger = LoggerFactory.getLogger(BidderMoneySellerDao.class);

    public static class PaymentResult {
        public final boolean success;
        public final String message;
        public final String auctionStatus;
        public final long amount;

        public PaymentResult(boolean success, String message, String auctionStatus, long amount) {
            this.success = success;
            this.message = message;
            this.auctionStatus = auctionStatus;
            this.amount = amount;
        }
    }

    /**
     * Chuyển tiền từ Bidder (người thắng) sang Seller.
     */
    public boolean thanhToanPhienDauGia(int idBidder, int idSeller, long soTien) {
        // Sử dụng try-with-resources
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {

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

                // Ghi log biến động số dư cho Bidder
                String sqlLogBidder = "INSERT INTO wallet_transactions (user_id, transaction_type, amount, description) VALUES (?, 'PAYMENT_SENT', ?, 'Thanh toán đấu giá')";
                try (PreparedStatement p3 = conn.prepareStatement(sqlLogBidder)) {
                    p3.setInt(1, idBidder);
                    p3.setLong(2, soTien);
                    p3.executeUpdate();
                }

                // Ghi log biến động số dư cho Seller
                String sqlLogSeller = "INSERT INTO wallet_transactions (user_id, transaction_type, amount, description) VALUES (?, 'PAYMENT_RECEIVED', ?, 'Nhận tiền bán đấu giá')";
                try (PreparedStatement p4 = conn.prepareStatement(sqlLogSeller)) {
                    p4.setInt(1, idSeller);
                    p4.setLong(2, soTien);
                    p4.executeUpdate();
                }

                // Nếu cả hai bước thành công, xác nhận thay đổi vĩnh viễn vào DB
                conn.commit();
                return true;

            } catch (SQLException e) {
                // Nếu có bất kỳ lỗi SQL nào, hoàn tác (rollback) để tránh mất tiền oan
                conn.rollback();
                logger.error("[Transaction Error] Lỗi thanh toán: ", e);
                return false;
            } finally {
                // Trả trạng thái AutoCommit về mặc định trước khi trả kết nối về Pool
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            logger.error("[Pool Error] Không thể lấy kết nối: ", e);
            return false;
        }
    }

    /**
     * Quyết toán phiên đã có người thắng. Bidder/seller/số tiền đều được đọc
     * lại từ DB trong transaction để client không thể tự đổi người nhận hoặc giá.
     */
    public PaymentResult quyetToanMuaDut(int auctionId, int bidderId, boolean thanhToan) {
        String lockAuction = "SELECT a.status, a.current_price, a.buy_now_price, "
                + "a.highest_bidder_id, i.seller_id "
                + "FROM auctions a JOIN items i ON a.item_id = i.id "
                + "WHERE a.id = ? FOR UPDATE";

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            try {
                int sellerId;
                Long buyNowPrice;
                long currentPrice;
                String status;
                Integer winnerId;

                try (PreparedStatement lock = conn.prepareStatement(lockAuction)) {
                    lock.setInt(1, auctionId);
                    try (ResultSet rs = lock.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return new PaymentResult(false, "Phiên đấu giá không tồn tại.", null, 0);
                        }

                        status = rs.getString("status");
                        currentPrice = rs.getLong("current_price");
                        long rawBuyNowPrice = rs.getLong("buy_now_price");
                        buyNowPrice = rs.wasNull() ? null : rawBuyNowPrice;

                        int rawWinnerId = rs.getInt("highest_bidder_id");
                        winnerId = rs.wasNull() ? null : rawWinnerId;
                        sellerId = rs.getInt("seller_id");
                    }
                }

                if (!"FINISHED".equalsIgnoreCase(status)) {
                    conn.rollback();
                    return new PaymentResult(false, "Phiên không ở trạng thái chờ thanh toán.", status, 0);
                }
                if (winnerId == null || winnerId != bidderId) {
                    conn.rollback();
                    return new PaymentResult(false, "Chỉ bidder thắng phiên mới được quyết toán.", status, 0);
                }
                if (currentPrice <= 0) {
                    conn.rollback();
                    return new PaymentResult(false, "Giá chốt phiên không hợp lệ.", status, 0);
                }

                boolean isBuyNow = buyNowPrice != null && currentPrice >= buyNowPrice;
                long amount = thanhToan ? currentPrice : (currentPrice + 9) / 10;
                String nextStatus = thanhToan ? "PAID" : "CANCELED";
                String settlementLabel = isBuyNow ? "mua đứt" : "phiên";
                String bidderDescription = thanhToan
                        ? "Thanh toán " + settlementLabel + " #" + auctionId
                        : "Phạt hủy thanh toán " + settlementLabel + " #" + auctionId;
                String sellerDescription = thanhToan
                        ? "Nhận tiền " + settlementLabel + " #" + auctionId
                        : "Nhận phạt hủy thanh toán " + settlementLabel + " #" + auctionId;

                if (!truTienBidder(conn, bidderId, amount)) {
                    conn.rollback();
                    return new PaymentResult(false, "Số dư ví bidder không đủ.", status, amount);
                }
                if (!congTienSeller(conn, sellerId, amount)) {
                    conn.rollback();
                    return new PaymentResult(false, "Không tìm thấy seller nhận tiền.", status, amount);
                }

                ghiLichSuVi(conn, bidderId, "PAYMENT_SENT", amount, bidderDescription);
                ghiLichSuVi(conn, sellerId, "PAYMENT_RECEIVED", amount, sellerDescription);

                try (PreparedStatement updateStatus =
                             conn.prepareStatement("UPDATE auctions SET status = ? WHERE id = ? AND status = 'FINISHED'")) {
                    updateStatus.setString(1, nextStatus);
                    updateStatus.setInt(2, auctionId);
                    if (updateStatus.executeUpdate() == 0) {
                        conn.rollback();
                        return new PaymentResult(false, "Trạng thái phiên đã thay đổi.", status, amount);
                    }
                }

                conn.commit();
                return new PaymentResult(
                        true,
                        thanhToan ? "Thanh toán phiên đấu giá thành công." : "Đã hủy thanh toán và trừ phí phạt.",
                        nextStatus,
                        amount
                );
            } catch (SQLException e) {
                conn.rollback();
                logger.error("[Transaction Error] Lỗi quyết toán phiên đấu giá: ", e);
                return new PaymentResult(false, "Không quyết toán được phiên đấu giá.", null, 0);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("[Pool Error] Không thể lấy kết nối khi quyết toán phiên đấu giá: ", e);
            return new PaymentResult(false, "Không kết nối được database.", null, 0);
        }
    }

    private boolean truTienBidder(Connection conn, int bidderId, long amount) throws SQLException {
        String sql = "UPDATE users SET balance = balance - ? "
                + "WHERE id = ? AND role = 'BIDDER' AND balance >= ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, amount);
            pstmt.setInt(2, bidderId);
            pstmt.setLong(3, amount);
            return pstmt.executeUpdate() == 1;
        }
    }

    private boolean congTienSeller(Connection conn, int sellerId, long amount) throws SQLException {
        String sql = "UPDATE users SET balance = balance + ? WHERE id = ? AND role = 'SELLER'";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, amount);
            pstmt.setInt(2, sellerId);
            return pstmt.executeUpdate() == 1;
        }
    }

    private void ghiLichSuVi(Connection conn, int userId, String type, long amount, String description)
            throws SQLException {
        String sql = "INSERT INTO wallet_transactions "
                + "(user_id, transaction_type, amount, description) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, type);
            pstmt.setLong(3, amount);
            pstmt.setString(4, description);
            pstmt.executeUpdate();
        }
    }
}
