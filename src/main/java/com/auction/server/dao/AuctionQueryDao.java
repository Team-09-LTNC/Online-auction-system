package com.auction.server.dao;

import com.auction.common.model.bid.Auction;
import com.auction.server.db.DatabaseConnection;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class AuctionQueryDao {
  private static final Logger logger = LoggerFactory.getLogger(AuctionQueryDao.class);
  private static final AtomicBoolean checkedImageThumbColumn = new AtomicBoolean(false);

  List<Auction> getRunningAuctions() {
    return executeListQuery(
        "SELECT a.*, i.name, i.description, i.category, i.starting_price, "
            + "i.bid_increment, i.seller_id, i.image_url, i.image_thumb_url "
            + "FROM auctions a JOIN items i ON a.item_id = i.id "
            + "WHERE a.status = 'RUNNING' AND a.start_time <= NOW() AND a.end_time > NOW()");
  }

  List<Auction> getPendingAuctionSessions() {
    return executeListQuery(
        "SELECT a.*, i.name, i.description, i.category, i.starting_price, "
            + "i.bid_increment, i.seller_id, i.image_url, i.image_thumb_url "
            + "FROM auctions a JOIN items i ON a.item_id = i.id "
            + "WHERE a.status = 'OPEN' AND a.start_time > NOW()");
  }

  List<Auction> getAllAuctionSessions() {
    return executeListQuery(
        "SELECT a.*, i.name, i.description, i.category, i.starting_price, "
            + "i.bid_increment, i.seller_id, i.image_url, i.image_thumb_url "
            + "FROM auctions a JOIN items i ON a.item_id = i.id ORDER BY a.start_time DESC, a.id DESC");
  }

  List<Auction> getTopRunningAuctionsByBids() {
    return executeListQuery(
        "SELECT a.*, i.name, i.description, i.category, i.starting_price, i.bid_increment, "
            + "i.seller_id, i.image_url, i.image_thumb_url, COUNT(b.id) AS bid_count "
            + "FROM auctions a "
            + "JOIN items i ON a.item_id = i.id "
            + "LEFT JOIN bid_history b ON a.id = b.auction_id "
            + "WHERE a.status = 'RUNNING' AND a.start_time <= NOW() AND a.end_time > NOW() "
            + "GROUP BY a.id, i.id "
            + "ORDER BY bid_count DESC, a.id DESC "
            + "LIMIT 6");
  }

  List<Auction> getJoinedAuctions(int userId, String role) {
    if ("SELLER".equals(role)) {
      return getSellerAuctions(userId);
    }
    return getBidderJoinedAuctions(userId);
  }

  int countRunningAuctions() {
    return countBySql(
        "SELECT COUNT(*) FROM auctions WHERE status = 'RUNNING' AND start_time <= NOW() AND end_time > NOW()");
  }

  int countEndingSoonAuctions() {
    return countBySql(
        "SELECT COUNT(*) FROM auctions "
            + "WHERE status = 'RUNNING' AND start_time <= NOW() "
            + "AND end_time > NOW() AND end_time <= DATE_ADD(NOW(), INTERVAL 1 HOUR)");
  }

  int countJoinedAuctions(int bidderId) {
    String sql = "SELECT COUNT(*) "
        + "FROM ("
        + "  SELECT DISTINCT b.auction_id "
        + "  FROM bid_history b "
        + "  JOIN auctions a ON a.id = b.auction_id "
        + "  WHERE b.bidder_id = ?"
        + ") joined";
    return countByPreparedSql(sql, bidderId, "Lỗi đếm phiên bidder đã tham gia: ");
  }

  int countActiveJoinedAuctions(int bidderId) {
    String sql = "SELECT COUNT(DISTINCT b.auction_id) "
        + "FROM bid_history b "
        + "JOIN auctions a ON a.id = b.auction_id "
        + "WHERE b.bidder_id = ? "
        + "AND a.status IN ('OPEN', 'RUNNING') "
        + "AND a.end_time > NOW()";
    return countByPreparedSql(sql, bidderId, "Lỗi đếm phiên bidder đang tham gia: ");
  }

  Auction getAuctionById(int idPhien) {
    String sql = "SELECT a.*, i.name, i.description, i.category, i.starting_price, "
        + "i.bid_increment, i.seller_id, i.image_url, i.image_thumb_url "
        + "FROM auctions a JOIN items i ON a.item_id = i.id WHERE a.id = ?";
    List<Auction> result = executeListQuery(sql, idPhien);
    return result.isEmpty() ? null : result.get(0);
  }

  Auction getAuctionByItemId(int itemId) {
    String sql = "SELECT a.*, i.name, i.description, i.category, i.starting_price, "
        + "i.bid_increment, i.seller_id, i.image_url, i.image_thumb_url "
        + "FROM auctions a JOIN items i ON a.item_id = i.id "
        + "WHERE i.id = ? ORDER BY a.id DESC LIMIT 1";
    List<Auction> result = executeListQuery(sql, itemId);
    return result.isEmpty() ? null : result.get(0);
  }

  List<Auction> searchAndFilterAuctions(String keyword, String status) {
    StringBuilder sql = new StringBuilder(
        "SELECT a.*, i.name, i.description, i.category, i.starting_price, "
            + "i.bid_increment, i.seller_id, i.image_url, i.image_thumb_url "
            + "FROM auctions a JOIN items i ON a.item_id = i.id WHERE 1=1 ");
    List<Object> params = new ArrayList<>();

    if (keyword != null && !keyword.trim().isEmpty()) {
      sql.append("AND i.name LIKE ? ");
      params.add("%" + keyword.trim() + "%");
    }
    if (status != null && !status.equalsIgnoreCase("ALL") && !status.equalsIgnoreCase("Tất cả")) {
      sql.append("AND a.status = ? ");
      params.add(status.toUpperCase());
    }

    List<Auction> result = new ArrayList<>();
    try (Connection conn = DatabaseConnection.getInstance().getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
      for (int i = 0; i < params.size(); i++) {
        pstmt.setObject(i + 1, params.get(i));
      }
      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          Auction auction = mapResultSetToAuction(rs);
          if (auction != null) {
            result.add(auction);
          }
        }
      }
    } catch (SQLException e) {
      logger.error("Lỗi timKiemVaLocPhienDauGia: ", e);
    }
    return result;
  }

  private List<Auction> getSellerAuctions(int sellerId) {
    String sql = "SELECT a.*, i.name, i.description, i.category, i.starting_price, "
        + "i.bid_increment, i.seller_id, i.image_url, i.image_thumb_url "
        + "FROM auctions a "
        + "JOIN items i ON a.item_id = i.id "
        + "WHERE i.seller_id = ? "
        + "ORDER BY a.start_time DESC, a.id DESC";
    return executeListQuery(sql, sellerId);
  }

  private List<Auction> getBidderJoinedAuctions(int bidderId) {
    String sql = "SELECT a.*, i.name, i.description, i.category, i.starting_price, "
        + "i.bid_increment, i.seller_id, i.image_url, i.image_thumb_url "
        + "FROM auctions a "
        + "JOIN items i ON a.item_id = i.id "
        + "JOIN ("
        + "  SELECT auction_id, MAX(bid_time) AS latest_bid_time "
        + "  FROM bid_history "
        + "  WHERE bidder_id = ? "
        + "  GROUP BY auction_id"
        + ") joined ON joined.auction_id = a.id "
        + "ORDER BY joined.latest_bid_time DESC, a.id DESC";
    return executeListQuery(sql, bidderId);
  }

  private List<Auction> executeListQuery(String sql) {
    ensureImageThumbColumn();
    List<Auction> result = new ArrayList<>();
    try (Connection conn = DatabaseConnection.getInstance().getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
      while (rs.next()) {
        Auction auction = mapResultSetToAuction(rs);
        if (auction != null) {
          result.add(auction);
        }
      }
    } catch (SQLException e) {
      logger.error("Lỗi truy vấn danh sách: ", e);
    }
    return result;
  }

  private List<Auction> executeListQuery(String sql, int param) {
    ensureImageThumbColumn();
    List<Auction> result = new ArrayList<>();
    try (Connection conn = DatabaseConnection.getInstance().getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setInt(1, param);
      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          Auction auction = mapResultSetToAuction(rs);
          if (auction != null) {
            result.add(auction);
          }
        }
      }
    } catch (SQLException e) {
      logger.error("Lỗi truy vấn danh sách có tham số: ", e);
    }
    return result;
  }

  private int countBySql(String sql) {
    try (Connection conn = DatabaseConnection.getInstance().getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
      return rs.next() ? rs.getInt(1) : 0;
    } catch (SQLException e) {
      logger.error("Lỗi truy vấn số lượng phiên đấu giá: ", e);
      return 0;
    }
  }

  private int countByPreparedSql(String sql, int param, String errorMessage) {
    try (Connection conn = DatabaseConnection.getInstance().getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setInt(1, param);
      try (ResultSet rs = pstmt.executeQuery()) {
        return rs.next() ? rs.getInt(1) : 0;
      }
    } catch (SQLException e) {
      logger.error(errorMessage, e);
      return 0;
    }
  }

  private Auction mapResultSetToAuction(ResultSet rs) throws SQLException {
    return AuctionRowMapper.mapResultSetToAuction(rs, logger);
  }

  private void ensureImageThumbColumn() {
    if (checkedImageThumbColumn.get()) {
      return;
    }

    synchronized (AuctionQueryDao.class) {
      if (checkedImageThumbColumn.get()) {
        return;
      }

      try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet columns = metaData.getColumns(null, null, "items", "image_thumb_url")) {
          if (columns.next()) {
            checkedImageThumbColumn.set(true);
            return;
          }
        }

        try (Statement stmt = conn.createStatement()) {
          stmt.executeUpdate("ALTER TABLE items ADD COLUMN image_thumb_url VARCHAR(500) NULL");
        }
        checkedImageThumbColumn.set(true);
      } catch (Exception e) {
        logger.error("Cannot ensure image_thumb_url column.", e);
      }
    }
  }
}
