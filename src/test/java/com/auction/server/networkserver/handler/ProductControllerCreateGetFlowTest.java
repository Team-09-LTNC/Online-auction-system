package com.auction.server.networkserver.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.auction.common.enums.ActionType;
import com.auction.common.model.user.Seller;
import com.auction.server.networkserver.ClientHandler;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

class ProductControllerCreateGetFlowTest extends ProductControllerFlowSupport {

    @Test
    void sellerCanCreateProductThroughController() {
        Seller seller = null;
        int itemId = -1;

        try {
            seller = seller();
            JsonObject response = send(createRequest("Created Controller Product"), login(seller));

            assertThat(response.get("type").getAsString()).isEqualTo(ActionType.CREATE_PRODUCT);
            assertThat(response.get("success").getAsBoolean()).isTrue();
            itemId = response.get("itemId").getAsInt();
            assertThat(itemDao.getProductById(itemId).getName()).isEqualTo("Created Controller Product");
        } finally {
            cleanup(seller, itemId);
        }
    }

    @Test
    void sellerCanGetOnlyOwnProducts() {
        Seller seller = null;
        Seller otherSeller = null;
        int ownItemId = -1;
        int otherItemId = -1;

        try {
            seller = seller();
            otherSeller = seller();
            ClientHandler client = login(seller);
            ownItemId = send(createRequest("Own Controller Product"), client).get("itemId").getAsInt();
            otherItemId = send(createRequest("Other Controller Product"), login(otherSeller))
                    .get("itemId").getAsInt();

            JsonObject response = send(myProductsRequest(), client);
            JsonArray data = response.getAsJsonArray("data");

            assertThat(response.get("type").getAsString()).isEqualTo(ActionType.GET_MY_PRODUCTS);
            assertThat(response.get("success").getAsBoolean()).isTrue();
            assertThat(containsItem(data, ownItemId)).isTrue();
            assertThat(containsItem(data, otherItemId)).isFalse();
        } finally {
            cleanup(seller, ownItemId);
            cleanup(otherSeller, otherItemId);
        }
    }

    private JsonObject myProductsRequest() {
        JsonObject request = new JsonObject();
        request.addProperty("type", ActionType.GET_MY_PRODUCTS);
        return request;
    }

    private boolean containsItem(JsonArray data, int itemId) {
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getAsJsonObject().get("itemId").getAsInt() == itemId) {
                return true;
            }
        }
        return false;
    }
}
