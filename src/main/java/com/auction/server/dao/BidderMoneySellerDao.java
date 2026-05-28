package com.auction.server.dao;

import com.auction.server.db.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * Nhiệm vụ: Xử lý giao dịch tiền giữa người đặt giá và người bán một cách an toàn.
 */
public class BidderMoneySellerDao {
    private static final Logger logger = LoggerFactory.getLogger(BidderMoneySellerDao.class);

    public static class PaymentResult {
        public final boolean success;
        public final String message;
        public final String auctionStatus;
        public final long amount;
        public final int bidderId;
        public final int sellerId;
        public final String itemName;
        public final String bidderName;
        public final String bidderTransactionType;
        public final String sellerTransactionType;
        public final String bidderDescription;
        public final String sellerDescription;

        public PaymentResult(boolean success, String message, String auctionStatus, long amount) {
            this(success, message, auctionStatus, amount, -1, -1, null, null, null, null, null, null);
        }

        public PaymentResult(
                boolean success,
                String message,
                String auctionStatus,
                long amount,
                int bidderId,
                int sellerId,
                String itemName,
                String bidderName,
                String bidderTransactionType,
                String sellerTransactionType,
                String bidderDescription,
                String sellerDescription
        ) {
            this.success = success;
            this.message = message;
            this.auctionStatus = auctionStatus;
            this.amount = amount;
            this.bidderId = bidderId;
            this.sellerId = sellerId;
            this.itemName = itemName;
            this.bidderName = bidderName;
            this.bidderTransactionType = bidderTransactionType;
            this.sellerTransactionType = sellerTransactionType;
            this.bidderDescription = bidderDescription;
            this.sellerDescription = sellerDescription;
        }
    }

    /**
     * Chuyển tiền từ người đặt giá thắng phiên sang người bán.
     */
    public boolean payAuction(int idBidder, int idSeller, long soTien) {
        // Sử dụng khối try tự đóng tài nguyên
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {

            // Tắt tự động commit để bắt đầu một giao dịch thủ công
            conn.setAutoCommit(false);

            try {
                // Trừ tiền người đặt giá (chỉ trừ nếu đủ số dư)
                String sqlTruTien = "UPDATE users SET balance = balance - ? " +
                        "WHERE id = ? AND role = 'BIDDER' AND balance >= ?";
                try (PreparedStatement p1 = conn.prepareStatement(sqlTruTien)) {
                    p1.setLong(1, soTien); // Gán tham số kiểu long
                    p1.setInt(2, idBidder);
                    p1.setLong(3, soTien);

                    if (p1.executeUpdate() == 0) {
                        conn.rollback(); // Không đủ điều kiện (thiếu tiền) thì hủy toàn bộ
                        return false;
                    }
                }

                // Cộng tiền người bán
                String sqlCongTien = "UPDATE users SET balance = balance + ? " +
                        "WHERE id = ? AND role = 'SELLER'";
                try (PreparedStatement p2 = conn.prepareStatement(sqlCongTien)) {
                    p2.setLong(1, soTien); // Gán tham số kiểu long
                    p2.setInt(2, idSeller);
                    p2.executeUpdate();
                }

                // Ghi log biến động số dư cho người đặt giá
                String sqlLogBidder = "INSERT INTO wallet_transactions (user_id, transaction_type, amount, description) VALUES (?, 'PAYMENT_SENT', ?, 'Thanh toán đấu giá')";
                try (PreparedStatement p3 = conn.prepareStatement(sqlLogBidder)) {
                    p3.setInt(1, idBidder);
                    p3.setLong(2, soTien);
                    p3.executeUpdate();
                }

                // Ghi log biến động số dư cho người bán
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
                // Nếu có bất kỳ lỗi SQL nào, hoàn tác để tránh mất tiền oan
                conn.rollback();
                logger.error("[Transaction Error] Lỗi thanh toán: ", e);
                return false;
            } finally {
                // Trả trạng thái tự động commit về mặc định trước khi trả kết nối về nhóm kết nối
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            logger.error("[Pool Error] Không thể lấy kết nối: ", e);
            return false;
        }
    }

    /**
     * Quyết toán phiên đã có người thắng. Người đặt giá/người bán/số tiền đều được đọc
     * lại từ DB trong giao dịch để client không thể tự đổi người nhận hoặc giá.
     */
    public PaymentResult settleBuyNow(int auctionId, int bidderId, boolean thanhToan) {
        String lockAuction = "SELECT a.status, a.current_price, a.buy_now_price, "
                + "a.highest_bidder_id, i.seller_id, i.name AS item_name, u.full_name AS bidder_name "
                + "FROM auctions a JOIN items i ON a.item_id = i.id "
                + "LEFT JOIN users u ON u.id = a.highest_bidder_id "
                + "WHERE a.id = ? FOR UPDATE";

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            try {
                int sellerId;
                Long buyNowPrice;
                long currentPrice;
                String status;
                Integer winnerId;
                String itemName;
                String bidderName;

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
                        itemName = rs.getString("item_name");
                        bidderName = rs.getString("bidder_name");
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
                String itemLabel = itemName == null || itemName.isBlank() ? "sản phẩm" : itemName;
                String auctionLabel = "sản phẩm " + itemLabel + " của phiên ID " + auctionId;
                String bidderDescription = thanhToan
                        ? "Thanh toán " + settlementLabel + " " + auctionLabel
                        : "Phạt hủy thanh toán " + settlementLabel + " " + auctionLabel;
                String sellerDescription = thanhToan
                        ? "Nhận tiền " + settlementLabel + " " + auctionLabel
                        : "Nhận phạt hủy thanh toán " + settlementLabel + " " + auctionLabel;
                String bidderTransactionType = "PAYMENT_SENT";
                String sellerTransactionType = "PAYMENT_RECEIVED";

                if (!deductBidderBalance(conn, bidderId, amount)) {
                    conn.rollback();
                    return new PaymentResult(
                            false,
                            "Số dư ví bidder không đủ.",
                            status,
                            amount,
                            bidderId,
                            sellerId,
                            itemName,
                            bidderName,
                            bidderTransactionType,
                            sellerTransactionType,
                            bidderDescription,
                            sellerDescription
                    );
                }
                if (!creditSeller(conn, sellerId, amount)) {
                    conn.rollback();
                    return new PaymentResult(false, "Không tìm thấy seller nhận tiền.", status, amount);
                }

                recordWalletHistory(conn, bidderId, bidderTransactionType, amount, bidderDescription);
                recordWalletHistory(conn, sellerId, sellerTransactionType, amount, sellerDescription);

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
                        amount,
                        bidderId,
                        sellerId,
                        itemName,
                        bidderName,
                        bidderTransactionType,
                        sellerTransactionType,
                        bidderDescription,
                        sellerDescription
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

    private boolean deductBidderBalance(Connection conn, int bidderId, long amount) throws SQLException {
        String sql = "UPDATE users SET balance = balance - ? "
                + "WHERE id = ? AND role = 'BIDDER' AND balance >= ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, amount);
            pstmt.setInt(2, bidderId);
            pstmt.setLong(3, amount);
            return pstmt.executeUpdate() == 1;
        }
    }

    private boolean creditSeller(Connection conn, int sellerId, long amount) throws SQLException {
        String sql = "UPDATE users SET balance = balance + ? WHERE id = ? AND role = 'SELLER'";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, amount);
            pstmt.setInt(2, sellerId);
            return pstmt.executeUpdate() == 1;
        }
    }

    private void recordWalletHistory(Connection conn, int userId, String type, long amount, String description)
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
