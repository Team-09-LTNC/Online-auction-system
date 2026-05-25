package com.auction.server.networkserver.handler;

import com.auction.common.enums.ActionType;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductControllerTest {

    private final ProductController controller = new ProductController();

    @Test
    void nullRequestReturnsBadRequestErrorResponse() {
        String response = controller.handleRequest(null, null);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        assertEquals("ERROR_RESPONSE", json.get("type").getAsString());
        assertEquals(400, json.get("statusCode").getAsInt());
        assertEquals("ERR_BAD_REQUEST", json.get("errorCode").getAsString());
    }

    @Test
    void unsupportedActionReturnsBadRequestErrorResponse() {
        JsonObject request = new JsonObject();
        request.addProperty("type", "UNKNOWN_PRODUCT_ACTION");
        request.addProperty("requestId", "req-product-1");

        String response = controller.handleRequest(request, null);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        assertEquals("ERROR_RESPONSE", json.get("type").getAsString());
        assertEquals(400, json.get("statusCode").getAsInt());
        assertEquals("ERR_BAD_REQUEST", json.get("errorCode").getAsString());
        assertEquals("req-product-1", json.get("requestId").getAsString());
    }

    @Test
    void searchProductWithoutKeywordReturnsBadRequest() {
        JsonObject request = new JsonObject();
        request.addProperty("type", ActionType.SEARCH_PRODUCT);

        String response = controller.handleRequest(request, null);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        assertEquals(ActionType.SEARCH_PRODUCT, json.get("type").getAsString());
        assertEquals(400, json.get("statusCode").getAsInt());
        assertEquals("ERR_BAD_REQUEST", json.get("errorCode").getAsString());
    }

    @Test
    void deleteProductWithoutValidItemIdReturnsBadRequest() {
        JsonObject request = new JsonObject();
        request.addProperty("type", ActionType.DELETE_PRODUCT);
        request.addProperty("itemId", "x");

        String response = controller.handleRequest(request, null);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        assertEquals(ActionType.DELETE_PRODUCT, json.get("type").getAsString());
        assertEquals(400, json.get("statusCode").getAsInt());
    }

    @Test
    void getProductByIdWithoutValidItemIdReturnsBadRequest() {
        JsonObject request = new JsonObject();
        request.addProperty("type", ActionType.GET_PRODUCT_BY_ID);
        request.addProperty("requestId", "get-item-1");

        String response = controller.handleRequest(request, null);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        assertEquals(ActionType.GET_PRODUCT_BY_ID, json.get("type").getAsString());
        assertEquals(400, json.get("statusCode").getAsInt());
        assertEquals("get-item-1", json.get("requestId").getAsString());
        assertTrue(json.get("message").getAsString().contains("itemId"));
    }
}
