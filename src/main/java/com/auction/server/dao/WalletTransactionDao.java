package com.auction.server.dao;

import com.auction.server.db.DatabaseConnection;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class WalletTransactionDao {
    private static final Logger logger = LoggerFactory.getLogger(WalletTransactionDao.class);

    public boolean logTransaction(int userId, String transactionType, long amount, String description) {
        String sql = "INSERT INTO wallet_transactions (user_id, transaction_type, amount, description) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setString(2, transactionType);
            pstmt.setLong(3, amount);
            pstmt.setString(4, description);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Lỗi khi ghi log biến động số dư: ", e);
            return false;
        }
    }

    public JsonArray getTransactionHistory(int userId) {
        JsonArray history = new JsonArray();
        String sql = "SELECT * FROM wallet_transactions WHERE user_id = ? ORDER BY transaction_time DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    JsonObject trans = new JsonObject();
                    trans.addProperty("id", rs.getInt("id"));
                    trans.addProperty("type", rs.getString("transaction_type"));
                    trans.addProperty("amount", rs.getLong("amount"));
                    trans.addProperty("description", rs.getString("description"));
                    trans.addProperty("time", rs.getTimestamp("transaction_time").toString());
                    history.add(trans);
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi khi lấy lịch sử giao dịch: ", e);
        }
        return history;
    }
}