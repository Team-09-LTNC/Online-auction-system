package com.auction.server.dao;

import com.auction.server.db.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;

final class AdminTestData {

    private AdminTestData() {
    }

    static Seed createAuction(String status, long startingPrice, long currentPrice, boolean withWinner)
            throws SQLException {
        String marker = status.toLowerCase() + "_" + System.nanoTime();
        int sellerId = insertUser("seller_" + marker, "SELLER");
        int bidderId = withWinner ? insertUser("bidder_" + marker, "BIDDER") : 0;
        int itemId = insertItem(sellerId, "Admin Test Item " + marker, startingPrice);
        int auctionId = insertAuction(itemId, bidderId, status, currentPrice);
        return new Seed(sellerId, bidderId, itemId, auctionId, "Admin Test Item " + marker, currentPrice);
    }

    static void cleanup(Seed seed) {
        deleteById("auctions", seed.auctionId());
        deleteById("items", seed.itemId());
        if (seed.bidderId() > 0) {
            deleteById("users", seed.bidderId());
        }
        deleteById("users", seed.sellerId());
    }

    private static int insertUser(String username, String role) throws SQLException {
        String sql = "INSERT INTO users (username, password, full_name, role, balance, status) "
                + "VALUES (?, 'pass', ?, ?, 10000000, 'ACTIVE')";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, "Test " + role);
            ps.setString(3, role);
            ps.executeUpdate();
            return generatedId(ps);
        }
    }

    private static int insertItem(int sellerId, String itemName, long startingPrice) throws SQLException {
        String sql = "INSERT INTO items "
                + "(seller_id, name, description, category, starting_price, bid_increment, image_url) "
                + "VALUES (?, ?, 'Admin test description', 'OTHER', ?, 100000, '')";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, sellerId);
            ps.setString(2, itemName);
            ps.setLong(3, startingPrice);
            ps.executeUpdate();
            return generatedId(ps);
        }
    }

    private static int insertAuction(int itemId, int bidderId, String status, long currentPrice)
            throws SQLException {
        String sql = "INSERT INTO auctions "
                + "(item_id, current_price, highest_bidder_id, start_time, end_time, status, anti_sniping_enabled) "
                + "VALUES (?, ?, ?, ?, ?, ?, FALSE)";
        LocalDateTime now = LocalDateTime.now();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, itemId);
            ps.setLong(2, currentPrice);
            if (bidderId > 0) {
                ps.setInt(3, bidderId);
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            ps.setTimestamp(4, Timestamp.valueOf(now.minusHours(1)));
            ps.setTimestamp(5, Timestamp.valueOf(now.plusHours(1)));
            ps.setString(6, status);
            ps.executeUpdate();
            return generatedId(ps);
        }
    }

    private static int generatedId(PreparedStatement ps) throws SQLException {
        try (var keys = ps.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getInt(1);
            }
        }
        throw new SQLException("No generated key returned.");
    }

    private static void deleteById(String table, int id) {
        String sql = "DELETE FROM " + table + " WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    record Seed(int sellerId, int bidderId, int itemId, int auctionId, String itemName, long currentPrice) {
    }
}
