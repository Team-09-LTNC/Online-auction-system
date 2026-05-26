package com.auction.server.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.auction.common.model.item.Item;
import com.auction.common.model.item.OtherItem;
import com.auction.common.model.user.Seller;
import com.auction.server.db.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import org.junit.jupiter.api.Test;

class ItemDaoIntegrationTest extends DaoIntegrationTestSupport {

    private final ItemDao itemDao = new ItemDao();
    private final UserDao userDao = new UserDao();

    @Test
    void saveGetUpdateSearchAndDeleteProductWork() {
        int sellerId = -1;
        int itemId = -1;
        String suffix = String.valueOf(System.nanoTime());

        try {
            Seller seller = new Seller("item_seller_" + suffix, "pass", "Item Seller");
            assertThat(userDao.saveUser(seller)).isTrue();
            sellerId = userDao.findByUsername(seller.getUsername()).orElseThrow().getId();

            OtherItem item = new OtherItem("Vintage Camera " + suffix, "Original desc", 1_200_000L, "OTHER");
            item.setSellerId(sellerId);
            item.setCategory("OTHER");
            item.setBidIncrement(100_000L);
            item.setImageUrl("http://image.test/camera.png");
            item.setImageThumbUrl("http://image.test/camera-thumb.png");

            itemId = itemDao.saveProduct(item);
            assertThat(itemId).isPositive();

            Item saved = itemDao.getProductById(itemId);
            assertThat(saved).isNotNull();
            assertThat(saved.getName()).isEqualTo(item.getName());
            assertThat(saved.getSellerId()).isEqualTo(sellerId);

            saved.setName("Updated Camera " + suffix);
            saved.setDescription("Updated desc");
            saved.setStartingPrice(1_500_000L);
            saved.setCategory("OTHER");
            saved.setImageUrl("http://image.test/updated.png");
            saved.setImageThumbUrl("http://image.test/updated-thumb.png");
            assertThat(itemDao.updateProduct(saved)).isTrue();

            assertThat(itemDao.searchProductsByKeyword("Updated Camera " + suffix))
                    .extracting(Item::getId)
                    .contains(itemId);
            assertThat(itemDao.getProductsBySellerId(sellerId))
                    .extracting(Item::getId)
                    .contains(itemId);
            int deletedItemId = itemId;
            assertThat(itemDao.deleteProduct(deletedItemId)).isTrue();
            itemId = -1;
            assertThat(itemDao.getProductById(deletedItemId)).isNull();
        } finally {
            if (itemId > 0) {
                itemDao.deleteProduct(itemId);
            }
            deleteUser(sellerId);
        }
    }

    private void deleteUser(int userId) {
        if (userId <= 0) {
            return;
        }
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (Exception ignored) {
        }
    }
}
