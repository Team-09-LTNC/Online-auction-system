package com.auction.server.networkserver.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.auction.common.model.item.OtherItem;
import com.auction.common.model.user.Seller;
import com.auction.server.dao.ItemDao;
import com.auction.server.db.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

final class ProductControllerSeedData {
    private static final ItemDao ITEM_DAO = new ItemDao();

    private ProductControllerSeedData() {
    }

    static int openProductFor(Seller seller, String name) {
        OtherItem item = new OtherItem(
                name,
                seller.getId(),
                "Seeded open product description",
                1_200_000L,
                "OTHER",
                "https://image.test/seed.png");
        item.setCategory("OTHER");
        item.setBidIncrement(100_000L);
        item.setImageThumbUrl("https://image.test/seed-thumb.png");

        int itemId = ITEM_DAO.saveProduct(item);
        assertThat(itemId).isPositive();
        insertOpenAuction(itemId, item.getStartingPrice());
        return itemId;
    }

    private static void insertOpenAuction(int itemId, long startingPrice) {
        LocalDateTime start = LocalDateTime.now().plusDays(3);
        String sql = "INSERT INTO auctions "
                + "(item_id, current_price, buy_now_price, anti_sniping_enabled, status, start_time, end_time) "
                + "VALUES (?, ?, ?, ?, 'OPEN', ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            ps.setLong(2, startingPrice);
            ps.setLong(3, 2_000_000L);
            ps.setBoolean(4, true);
            ps.setTimestamp(5, Timestamp.valueOf(start));
            ps.setTimestamp(6, Timestamp.valueOf(start.plusHours(3)));
            assertThat(ps.executeUpdate()).isEqualTo(1);
        } catch (Exception e) {
            throw new AssertionError("Cannot seed open auction product.", e);
        }
    }
}
