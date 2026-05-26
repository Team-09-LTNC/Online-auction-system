package com.auction.server.networkserver.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.auction.common.enums.ActionType;
import com.auction.common.enums.AuctionStatus;
import com.auction.server.dao.DaoIntegrationTestSupport;
import com.auction.server.db.DatabaseConnection;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class AdminControllerTest extends DaoIntegrationTestSupport {
  private final AdminController controller = new AdminController();

  @Test
  void getPendingAuctionsReturnsAdminPayloadAndRequestId() throws Exception {
    int auctionId = insertAuction(AuctionStatus.PENDING, 1_000_000L);
    JsonObject request = request(ActionType.ADMIN_GET_PENDING_AUCTIONS, "admin-pending-1");

    JsonObject response = JsonParser.parseString(controller.handleRequest(request, null)).getAsJsonObject();

    assertThat(response.get("type").getAsString()).isEqualTo("ADMIN_GET_PENDING_AUCTIONS_RESPONSE");
    assertThat(response.get("success").getAsBoolean()).isTrue();
    assertThat(response.get("requestId").getAsString()).isEqualTo("admin-pending-1");
    assertThat(response.getAsJsonArray("data"))
        .anySatisfy(element ->
            assertThat(element.getAsJsonObject().get("id").getAsInt()).isEqualTo(auctionId));
  }

  @Test
  void getInvoicesCalculatesTotalRevenueFromPaidAuctions() throws Exception {
    int paidAuctionId = insertAuction(AuctionStatus.PAID, 3_500_000L);
    insertAuction(AuctionStatus.FINISHED, 2_000_000L);
    JsonObject request = request(ActionType.ADMIN_GET_INVOICES, "admin-invoice-1");

    JsonObject response = JsonParser.parseString(controller.handleRequest(request, null)).getAsJsonObject();

    assertThat(response.get("type").getAsString()).isEqualTo("ADMIN_GET_INVOICES_RESPONSE");
    assertThat(response.get("success").getAsBoolean()).isTrue();
    assertThat(response.get("tongDoanhThu").getAsLong()).isGreaterThanOrEqualTo(3_500_000L);
    assertThat(response.getAsJsonArray("data"))
        .anySatisfy(element ->
            assertThat(element.getAsJsonObject().get("auctionId").getAsInt()).isEqualTo(paidAuctionId));
  }

  @Test
  void getTransactionsIncludesTerminalAuctions() throws Exception {
    int canceledAuctionId = insertAuction(AuctionStatus.CANCELED, 900_000L);
    JsonObject request = request(ActionType.ADMIN_GET_TRANSACTIONS, "admin-tx-1");

    JsonObject response = JsonParser.parseString(controller.handleRequest(request, null)).getAsJsonObject();

    assertThat(response.get("type").getAsString()).isEqualTo("ADMIN_GET_TRANSACTIONS_RESPONSE");
    assertThat(response.get("success").getAsBoolean()).isTrue();
    assertThat(response.getAsJsonArray("data"))
        .anySatisfy(element ->
            assertThat(element.getAsJsonObject().get("auctionId").getAsInt()).isEqualTo(canceledAuctionId));
  }

  private JsonObject request(String type, String requestId) {
    JsonObject request = new JsonObject();
    request.addProperty("type", type);
    request.addProperty("requestId", requestId);
    return request;
  }

  private int insertAuction(AuctionStatus status, long price) throws Exception {
    long suffix = System.nanoTime();
    LocalDateTime start = LocalDateTime.now().minusHours(2);
    LocalDateTime end = LocalDateTime.now().minusMinutes(30);
    if (status == AuctionStatus.PENDING) {
      start = LocalDateTime.now().plusHours(1);
      end = LocalDateTime.now().plusHours(2);
    }

    try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
      int sellerId = insertUser(conn, "admin_controller_seller_" + suffix, "SELLER");
      int winnerId = insertUser(conn, "admin_controller_bidder_" + suffix, "BIDDER");
      int itemId = insertItem(conn, sellerId, "Admin Controller Item " + suffix, price);
      return insertAuctionRow(conn, itemId, winnerId, status, price, start, end);
    }
  }

  private int insertUser(Connection conn, String username, String role) throws Exception {
    String sql = "INSERT INTO users (username, password, full_name, role, status) VALUES (?, 'pw', ?, ?, 'ACTIVE')";
    try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setString(1, username);
      ps.setString(2, username);
      ps.setString(3, role);
      ps.executeUpdate();
      try (var keys = ps.getGeneratedKeys()) {
        keys.next();
        return keys.getInt(1);
      }
    }
  }

  private int insertItem(Connection conn, int sellerId, String name, long price) throws Exception {
    String sql = "INSERT INTO items "
        + "(seller_id, name, description, category, starting_price, bid_increment, image_url) "
        + "VALUES (?, ?, 'Admin controller test', 'OTHER', ?, 100000, 'http://image.test/admin-controller.png')";
    try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setInt(1, sellerId);
      ps.setString(2, name);
      ps.setLong(3, price);
      ps.executeUpdate();
      try (var keys = ps.getGeneratedKeys()) {
        keys.next();
        return keys.getInt(1);
      }
    }
  }

  private int insertAuctionRow(
      Connection conn,
      int itemId,
      int winnerId,
      AuctionStatus status,
      long price,
      LocalDateTime start,
      LocalDateTime end
  ) throws Exception {
    String sql = "INSERT INTO auctions "
        + "(item_id, current_price, highest_bidder_id, start_time, end_time, status) "
        + "VALUES (?, ?, ?, ?, ?, ?)";
    try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setInt(1, itemId);
      ps.setLong(2, price);
      ps.setInt(3, winnerId);
      ps.setTimestamp(4, Timestamp.valueOf(start));
      ps.setTimestamp(5, Timestamp.valueOf(end));
      ps.setString(6, status.name());
      ps.executeUpdate();
      try (var keys = ps.getGeneratedKeys()) {
        keys.next();
        return keys.getInt(1);
      }
    }
  }
}
