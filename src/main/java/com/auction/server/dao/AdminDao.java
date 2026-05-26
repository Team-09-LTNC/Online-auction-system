package com.auction.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auction.common.enums.AuctionStatus;
import com.auction.common.model.bid.Auction;
import com.auction.common.model.item.OtherItem;
import com.auction.server.db.DatabaseConnection;
import com.google.gson.JsonObject;

/**
 * Tầng quản lý truy cập dữ liệu (DAO) cho các phiên đấu giá.
 */
public class AdminDao {
    private static final Logger logger = LoggerFactory.getLogger(AdminDao.class);

    /**
     * Lấy danh sách tất cả phiên đấu giá, bao gồm cả thông tin sản phẩm và trạng
     * thái.
     */
    public List<Auction> getAllAuctions() {
        new AuctionDao().updateStatusByTime();
        List<Auction> auctions = new ArrayList<>();
        String sql = "SELECT a.id, i.name AS item_name, a.start_time, a.end_time, a.status, i.image_url " +
                "FROM auctions a JOIN items i ON a.item_id = i.id"; // Câu truy vấn lấy thêm image_url từ bảng items
        // ... rest of the method implementation
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int auctionId = rs.getInt("id");
                String itemName = rs.getString("item_name");
                LocalDateTime startTime = rs.getTimestamp("start_time").toLocalDateTime();
                LocalDateTime endTime = rs.getTimestamp("end_time").toLocalDateTime();
                String statusStr = rs.getString("status");
                AuctionStatus status = statusStr != null ? AuctionStatus.valueOf(statusStr) : AuctionStatus.OPEN;
                String imageUrl = rs.getString("image_url");
                // TẠM THỜI: Chỉ tạo đối tượng Auction với thông tin cơ bản, không nạp đầy đủ
                // Item
                OtherItem item = new OtherItem(itemName, imageUrl); // Giá và hình ảnh tạm thời
                Auction auction = new Auction(item);
                auction.setId(auctionId);
                auction.setStartTime(startTime);
                auction.setEndTime(endTime);
                auction.setStatus(status);

                auctions.add(auction);
            }
        } catch (SQLException e) {
            logger.error("Lỗi layDanhSachTatCaAuctions: ", e);
        }

        return auctions;
    }

    // Lấy danh sách phiên chờ duyệt
    public List<Auction> getPendingAuctions() {
        List<Auction> auctions = new ArrayList<>();
        String sql = "SELECT a.id, a.status, i.seller_id, i.name AS item_name, i.category, " +
                "i.starting_price, i.description, i.image_url, " +
                "a.start_time, a.end_time " +
                "FROM auctions a JOIN items i ON a.item_id = i.id " +
                "WHERE a.status = 'PENDING' " +
                "ORDER BY a.start_time ASC"; // Phiên sắp diễn ra hiện lên trước

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int auctionId = rs.getInt("id");
                int sellerId = rs.getInt("seller_id"); // ← THÊM
                String imageUrl = rs.getString("image_url"); // ← THÊM
                String itemName = rs.getString("item_name");
                String description = rs.getString("description");
                long startingPrice = rs.getLong("starting_price");
                String category = rs.getString("category");
                LocalDateTime start = rs.getTimestamp("start_time").toLocalDateTime();
                LocalDateTime end = rs.getTimestamp("end_time").toLocalDateTime();
                AuctionStatus status = AuctionStatus.valueOf(rs.getString("status"));

                OtherItem item = new OtherItem(itemName, sellerId, description, startingPrice, category, imageUrl);
                item.setImageUrl(imageUrl); // ← THÊM nếu Item có field này

                Auction auction = new Auction(item);
                auction.setId(auctionId);
                auction.setStartTime(start);
                auction.setEndTime(end);
                auction.setStatus(status);
                auctions.add(auction);
            }

            logger.info("Đã lấy danh sách phiên đấu giá chờ duyệt từ database trong AdminDao");
        } catch (SQLException e) {
            logger.error("Lỗi layDanhSachChoDuyet: ", e);
        }
        return auctions;
    }

    // Duyệt hoặc từ chối phiên đấu giá
    public boolean approveAuction(int auctionId, String newStatus) {
        String sql = "UPDATE auctions SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, auctionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Lỗi duyetAuction: ", e);
            return false;
        }
    }

    /*
     * Lấy danh sách hóa đơn đã thanh toán, bao gồm thông tin về phiên đấu giá, sản
     * phẩm, người bán, người mua và giá cuối cùng.
     */
    public List<com.auction.common.dto.AdminDTOs.InvoiceDTO> getInvoices() {
        new AuctionDao().updateStatusByTime();
        List<com.auction.common.dto.AdminDTOs.InvoiceDTO> list = new ArrayList<>();
        String sql = "SELECT a.id AS auction_id, a.item_id, i.name AS item_name, " +
                "       i.seller_id, a.highest_bidder_id AS winner_id, " +
                "       a.current_price AS final_price " +
                "FROM auctions a " +
                "JOIN items i ON a.item_id = i.id " +
                "WHERE a.status = 'PAID' " +
                "ORDER BY a.id DESC";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new com.auction.common.dto.AdminDTOs.InvoiceDTO(
                        rs.getInt("auction_id"),
                        rs.getInt("item_id"),
                        rs.getString("item_name"),
                        rs.getInt("seller_id"), // ← lấy từ items
                        rs.getInt("winner_id"),
                        rs.getLong("final_price")));
            }
        } catch (SQLException e) {
            logger.error("Lỗi layDanhSachHoaDon: ", e);
        }
        return list;
    }

    public List<com.auction.common.dto.AdminDTOs.TransactionDTO> getTransactions() {
        new AuctionDao().updateStatusByTime();
        List<com.auction.common.dto.AdminDTOs.TransactionDTO> list = new ArrayList<>();
        String sql = "SELECT a.id AS auction_id, a.item_id, i.name AS item_name, "
                + "a.start_time, a.end_time, a.status, "
                + "COALESCE(a.highest_bidder_id, 0) AS winner_id, "
                + "a.current_price AS final_price "
                + "FROM auctions a "
                + "JOIN items i ON a.item_id = i.id "
                + "WHERE a.status IN ('FINISHED', 'PAID', 'CANCELED') "
                + "ORDER BY a.end_time DESC, a.id DESC";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new com.auction.common.dto.AdminDTOs.TransactionDTO(
                        rs.getInt("auction_id"),
                        rs.getInt("item_id"),
                        rs.getString("item_name"),
                        rs.getTimestamp("start_time").toLocalDateTime().toString(),
                        rs.getTimestamp("end_time").toLocalDateTime().toString(),
                        rs.getString("status"),
                        rs.getInt("winner_id"),
                        rs.getLong("final_price")));
            }
        } catch (SQLException e) {
            logger.error("Lỗi layDanhSachGiaoDich: ", e);
        }
        return list;
    }

    public boolean deleteAuction(int auctionId) {
        String sql = "DELETE FROM auctions WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Lỗi xoaPhienDauGia: ", e);
            return false;
        }
    }

    /*
     * Cập nhật trạng thái của phiên đấu giá (ví dụ: từ OPEN sang CANCELED, hoặc từ
     * RUNNING sang FINISHED)
     */
    public boolean updateAuctionStatus(int auctionId, String newStatus) {
        String sql = "UPDATE auctions SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, auctionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Lỗi capNhatTrangThaiAuction: ", e);
            return false;
        }
    }

    public JsonObject getAuctionInfo(int auctionId) {
        String sql = "SELECT status, start_time, end_time FROM auctions WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, auctionId); // ← set tham số TRƯỚC

            try (ResultSet rs = ps.executeQuery()) { // ← executeQuery SAU
                if (rs.next()) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("status", rs.getString("status"));
                    obj.addProperty("start_time", rs.getTimestamp("start_time").toLocalDateTime().toString());
                    obj.addProperty("end_time", rs.getTimestamp("end_time").toLocalDateTime().toString());
                    return obj;
                } else {
                    logger.warn("layThongTinAuction: Không tìm thấy auction với id: {}", auctionId);
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi layThongTinAuction: ", e);
        }
        return null;
    }
}
