package com.auction.server.dao;

import com.auction.common.model.bid.BidLine;
import com.auction.server.db.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * NHIỆM VỤ: Quản lý lịch sử đặt giá (Bid History)
 */
//  bảng bid_history: ( cần tạo trong database)
//  - id (INT, PK, Auto Increment): Mã định danh của lượt bid
//  - auction_id (INT): Liên kết tới phiên đấu giá (Khóa ngoại)
//  - bidder_id (INT): ID bidder (Khóa ngoại)
//  - bid_amount (BIGINT): Số tiền đặt giá
//  - bid_time (TIMESTAMP): Thời điểm đặt giá

public class BidTransactionDao {
    private static final Logger logger = LoggerFactory.getLogger(BidTransactionDao.class);
    /**
     * Dùng để lưu: ai, đặt bao nhiêu tiền, vào lúc nào
     */
    public boolean luuLichSuDatGia(int idPhien, int idNguoiBid, long soTien) {
        String sql = "INSERT INTO bid_history (auction_id, bidder_id, bid_amount, bid_time) VALUES (?, ?, ?, NOW())";

        try (Connection ketNoi = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstm = ketNoi.prepareStatement(sql)) {

            // Điền dữ liệu vào
            pstm.setInt(1, idPhien);
            pstm.setInt(2, idNguoiBid);
            pstm.setLong(3, soTien);

            // Trả về true nếu lưu thành công
            return pstm.executeUpdate() > 0;

        } catch (SQLException e) {
            logger.error("Lỗi luuLichSuDatGia: ", e);
            return false;
        }
    }

    /**
     *  Trả về List<BidLine> để Client có thể dùng GSON giải mã và vẽ biểu đồ
     */
    public List<BidLine> layLichSuPhien(int idPhien) {
        List<BidLine> danhSach = new ArrayList<>();

        // Lấy tên người dùng và số tiền, sắp xếp lượt đặt giá theo thời gian để vẽ biểu đồ
        String sql = "SELECT u.full_name, b.bid_amount, b.bid_time " +
                "FROM bid_history b " +
                "JOIN users u ON b.bidder_id = u.id " +
                "WHERE b.auction_id = ? " +
                "ORDER BY b.bid_time ASC";

        try (Connection ketNoi = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstm = ketNoi.prepareStatement(sql)) {

            pstm.setInt(1, idPhien);

            try (ResultSet ketQua = pstm.executeQuery()) {
                while (ketQua.next()) {
                    // Chuyển sang sử dụng đối tượng BidLine để đồng bộ với Client
                    danhSach.add(new BidLine(
                            ketQua.getString("full_name"),
                            ketQua.getLong("bid_amount"),
                            ketQua.getTimestamp("bid_time").toLocalDateTime()                    ));
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi layLichSuPhien: ", e);
        }
        return danhSach;
    }
}
