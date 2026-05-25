package com.auction.server.dao;

import com.auction.common.model.bid.BidLine;
import com.auction.server.db.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quan ly lich su dat gia (bid history).
 */
public class BidTransactionDao {
  private static final Logger logger = LoggerFactory.getLogger(BidTransactionDao.class);

  /**
   * Luu 1 dong lich su dat gia.
   */
  public boolean saveBidHistory(int idPhien, int idNguoiBid, long soTien) {
    String sql = "INSERT INTO bid_history (auction_id, bidder_id, bid_amount, bid_time) "
        + "VALUES (?, ?, ?, NOW())";

    try (Connection ketNoi = DatabaseConnection.getInstance().getConnection();
         PreparedStatement pstm = ketNoi.prepareStatement(sql)) {
      pstm.setInt(1, idPhien);
      pstm.setInt(2, idNguoiBid);
      pstm.setLong(3, soTien);
      return pstm.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("Loi luuLichSuDatGia.", e);
      return false;
    }
  }

  /**
   * Lay lich su bid cua 1 phien theo thu tu tang dan thoi gian.
   */
  public List<BidLine> getAuctionBidHistory(int idPhien) {
    List<BidLine> danhSach = new ArrayList<>();
    String sql = "SELECT u.full_name, b.bid_amount, b.bid_time "
        + "FROM bid_history b "
        + "JOIN users u ON b.bidder_id = u.id "
        + "WHERE b.auction_id = ? "
        + "ORDER BY b.bid_time ASC";

    try (Connection ketNoi = DatabaseConnection.getInstance().getConnection();
         PreparedStatement pstm = ketNoi.prepareStatement(sql)) {
      pstm.setInt(1, idPhien);
      try (ResultSet ketQua = pstm.executeQuery()) {
        while (ketQua.next()) {
          danhSach.add(new BidLine(
              ketQua.getString("full_name"),
              ketQua.getLong("bid_amount"),
              ketQua.getTimestamp("bid_time").toLocalDateTime()));
        }
      }
    } catch (SQLException e) {
      logger.error("Loi layLichSuPhien.", e);
    }
    return danhSach;
  }
}
