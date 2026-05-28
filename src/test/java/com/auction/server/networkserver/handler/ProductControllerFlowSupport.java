package com.auction.server.networkserver.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.auction.common.enums.ActionType;
import com.auction.common.model.user.Seller;
import com.auction.server.dao.DaoIntegrationTestSupport;
import com.auction.server.dao.ItemDao;
import com.auction.server.dao.UserDao;
import com.auction.server.db.DatabaseConnection;
import com.auction.server.networkserver.ClientHandler;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;

abstract class ProductControllerFlowSupport extends DaoIntegrationTestSupport {
    final ProductController controller = new ProductController();
    final ItemDao itemDao = new ItemDao();
    final UserDao userDao = new UserDao();

    JsonObject send(JsonObject request, ClientHandler client) {
        return JsonParser.parseString(controller.handleRequest(request, client)).getAsJsonObject();
    }

    ClientHandler login(Seller seller) {
        ClientHandler client = new ClientHandler(new Socket());
        client.setCurrentUser(seller);
        return client;
    }

    Seller seller() {
        String suffix = String.valueOf(System.nanoTime());
        Seller seller = new Seller("seller_" + suffix, "pass", "Product Seller");
        assertThat(userDao.saveUser(seller)).isTrue();
        seller.setId(userDao.findByUsername(seller.getUsername()).orElseThrow().getId());
        return seller;
    }

    JsonObject createRequest(String name) {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        JsonObject request = new JsonObject();
        request.addProperty("type", ActionType.CREATE_PRODUCT);
        request.addProperty("name", name);
        request.addProperty("description", "Original product description");
        request.addProperty("startingPrice", 1_200_000L);
        request.addProperty("category", "OTHER");
        request.addProperty("imageUrl", "https://image.test/product.png");
        request.addProperty("imageThumbUrl", "https://image.test/product-thumb.png");
        request.addProperty("startTime", start.toString());
        request.addProperty("endTime", start.plusHours(3).toString());
        request.addProperty("bidIncrement", 100_000L);
        request.addProperty("buyNowPrice", 2_000_000L);
        request.addProperty("antiSnipingEnabled", true);
        return request;
    }

    JsonObject updateRequest(int itemId) {
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        JsonObject request = new JsonObject();
        request.addProperty("type", ActionType.UPDATE_PRODUCT);
        request.addProperty("itemId", itemId);
        request.addProperty("name", "Updated Controller Product " + itemId);
        request.addProperty("description", "Updated product description");
        request.addProperty("startingPrice", 1_500_000L);
        request.addProperty("category", "OTHER");
        request.addProperty("imageUrl", "https://image.test/updated-product.png");
        request.addProperty("imageThumbUrl", "https://image.test/updated-product-thumb.png");
        request.addProperty("startTime", start.toString());
        request.addProperty("endTime", start.plusHours(2).toString());
        return request;
    }

    JsonObject typedRequest(String type, int itemId) {
        JsonObject request = new JsonObject();
        request.addProperty("type", type);
        request.addProperty("itemId", itemId);
        return request;
    }

    void cleanup(Seller seller, int itemId) {
        if (itemId > 0) {
            itemDao.deleteProduct(itemId);
        }
        if (seller == null || seller.getId() <= 0) {
            return;
        }
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
            ps.setInt(1, seller.getId());
            ps.executeUpdate();
        } catch (Exception ignored) {
        }
    }
}
