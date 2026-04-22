package com.auction.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

import com.auction.model.bid.BidTransaction;

public class BidTransactionDAO {
    private Connection conn;

    public BidTransactionDAO(Connection conn) {
        this.conn = conn;
    }

    public boolean saveTransaction(int auctionId, BidTransaction tx) {
        // Cấu trúc lệnh INSERT SQL
        String sql = "INSERT INTO bid_transaction (auction_id, bidder_id, bid_amount, timestamp) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, auctionId);
            pstmt.setString(2, tx.getBidder().getId());
            pstmt.setDouble(3, tx.getBidAmount());

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            pstmt.setString(4, tx.getTimestamp().format(formatter));

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.out.println("Lỗi khi lưu vào Database: " + e.getMessage());
            return false;
        }
    }

    // Lấy toàn bộ lịch sử đấu giá của một món hàng
    public void printTransactionHistory(int auctionId) {
        String sql = "SELECT bidder_id, bid_amount, timestamp FROM bid_transaction WHERE auction_id = ? ORDER BY timestamp DESC";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, auctionId);

            // Dùng executeQuery() để LẤY dữ liệu, nó sẽ trả về một cái ResultSet (Bảng kết
            // quả)
            ResultSet rs = pstmt.executeQuery();

            System.out.println("=== LỊCH SỬ ĐẤU GIÁ TỪ DATABASE ===");
            while (rs.next()) {
                // Đọc từng dòng trong bảng kết quả
                String bidderId = rs.getString("bidder_id");
                double amount = rs.getDouble("bid_amount");
                String time = rs.getString("timestamp");

                System.out
                        .println("Thời gian: " + time + " | ID Người đấu giá: " + bidderId + " | Chốt giá: $" + amount);
            }
            System.out.println("===================================");

        } catch (SQLException e) {
            System.out.println("Lỗi khi đọc dữ liệu: " + e.getMessage());
        }
    }
}