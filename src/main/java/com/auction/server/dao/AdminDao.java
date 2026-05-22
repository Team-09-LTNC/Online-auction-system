package com.auction.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auction.common.enums.AuctionStatus;
import com.auction.common.model.bid.Auction;
import com.auction.common.model.item.OtherItem;
import com.auction.server.db.DatabaseConnection;

/**
 * Tầng quản lý truy cập dữ liệu (DAO) cho các phiên đấu giá.
 */
public class AdminDao {
    private static final Logger logger = LoggerFactory.getLogger(AdminDao.class);
    /**
     * Lấy danh sách tất cả phiên đấu giá, bao gồm cả thông tin sản phẩm và trạng thái.
     */ 
    public List<Auction> layDanhSachTatCaAuctions() {
        List<Auction> auctions = new ArrayList<>();
        String sql = "SELECT i.name AS item_name, a.start_time, a.end_time, a.status, i.image_url " +
             "FROM auctions a JOIN items i ON a.item_id = i.id"; // Câu truy vấn lấy thêm image_url từ bảng items
        // ... rest of the method implementation
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String itemName = rs.getString("item_name");
                LocalDateTime startTime = rs.getTimestamp("start_time").toLocalDateTime();
                LocalDateTime endTime = rs.getTimestamp("end_time").toLocalDateTime();
                String statusStr = rs.getString("status");
                AuctionStatus status = statusStr != null ? AuctionStatus.valueOf(statusStr) : AuctionStatus.OPEN;
                String imageUrl = rs.getString("image_url");
                // TẠM THỜI: Chỉ tạo đối tượng Auction với thông tin cơ bản, không nạp đầy đủ Item
                OtherItem item = new OtherItem(itemName, imageUrl); // Giá và hình ảnh tạm thời
                Auction auction = new Auction(item);
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
}

