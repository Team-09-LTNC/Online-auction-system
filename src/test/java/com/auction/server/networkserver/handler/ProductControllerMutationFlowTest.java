package com.auction.server.networkserver.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.auction.common.enums.ActionType;
import com.auction.common.model.item.Item;
import com.auction.common.model.user.Seller;
import com.auction.server.networkserver.ClientHandler;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

class ProductControllerMutationFlowTest extends ProductControllerFlowSupport {

    @Test
    void sellerCanUpdateOwnOpenProduct() {
        Seller seller = null;
        int itemId = -1;

        try {
            seller = seller();
            ClientHandler client = login(seller);
            itemId = ProductControllerSeedData.openProductFor(seller, "Product Before Update");

            JsonObject response = send(updateRequest(itemId), client);
            Item updated = itemDao.getProductById(itemId);

            assertThat(response.get("type").getAsString()).isEqualTo(ActionType.UPDATE_PRODUCT);
            assertThat(response.get("success").getAsBoolean()).isTrue();
            assertThat(updated.getName()).startsWith("Updated Controller Product");
            assertThat(updated.getStartingPrice()).isEqualTo(1_500_000L);
        } finally {
            cleanup(seller, itemId);
        }
    }

}
