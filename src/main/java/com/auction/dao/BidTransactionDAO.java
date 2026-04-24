package com.auction.dao;

import java.sql.*;
import com.auction.model.bid.BidTransaction;

public class BidTransactionDAO {
    private Connection conn;

    public BidTransactionDAO(Connection conn) {
        this.conn = conn;
    }

    public boolean saveTransaction(int auctionId, BidTransaction tx) {
        if (conn == null) return false;
        String sql = "INSERT INTO bid_transaction (auction_id, bidder_id, bid_amount, timestamp) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, auctionId);
            pstmt.setString(2, tx.getBidder().getUsername());
            pstmt.setDouble(3, tx.getBidAmount());
            pstmt.setTimestamp(4, Timestamp.valueOf(tx.getTimestamp()));
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Lỗi lưu DB: " + e.getMessage());
            return false;
        }
    }

    public void printTransactionHistory(int auctionId) {
        if (conn == null) return;
        String sql = "SELECT bidder_id, bid_amount, timestamp FROM bid_transaction WHERE auction_id = ? ORDER BY bid_amount DESC";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, auctionId);
            ResultSet rs = pstmt.executeQuery();
            System.out.println("\n=== LỊCH SỬ ĐẤU GIÁ (TỪ CLOUD AIVEN) ===");
            while (rs.next()) {
                System.out.printf("Người: %-10s | Giá: $%-10.2f | Lúc: %s\n",
                        rs.getString("bidder_id"), rs.getDouble("bid_amount"), rs.getTimestamp("timestamp"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}