package com.auction.server.networkserver.handler;

import com.auction.common.enums.ActionType;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AuctionControllerTest {

    private final AuctionController controller = new AuctionController();

    @Test
    void nullRequestReturnsBadRequestErrorResponse() {
        String response = controller.xuLy(null, null);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        assertEquals("ERROR_RESPONSE", json.get("type").getAsString());
        assertEquals(400, json.get("statusCode").getAsInt());
        assertEquals("ERR_BAD_REQUEST", json.get("errorCode").getAsString());
    }

    @Test
    void unsupportedActionReturnsBadRequestErrorResponse() {
        JsonObject request = new JsonObject();
        request.addProperty("type", "UNKNOWN_AUCTION_ACTION");
        request.addProperty("requestId", "req-auction-1");

        String response = controller.xuLy(request, null);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        assertEquals("ERROR_RESPONSE", json.get("type").getAsString());
        assertEquals(400, json.get("statusCode").getAsInt());
        assertEquals("ERR_BAD_REQUEST", json.get("errorCode").getAsString());
        assertEquals("req-auction-1", json.get("requestId").getAsString());
    }

    @Test
    void createAuctionDirectlyReturnsGuidanceError() {
        JsonObject request = new JsonObject();
        request.addProperty("type", ActionType.CREATE_AUCTION);

        String response = controller.xuLy(request, null);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        assertEquals(400, json.get("statusCode").getAsInt());
        assertFalse(json.get("success").getAsBoolean());
        assertEquals("ERR_BAD_REQUEST", json.get("errorCode").getAsString());
    }

    @Test
    void joinAuctionWithoutAuctionIdReturnsBadRequestAndKeepsRequestId() {
        JsonObject request = new JsonObject();
        request.addProperty("type", ActionType.JOIN_AUCTION);
        request.addProperty("requestId", "join-req-1");

        String response = controller.xuLy(request, null);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        assertEquals("ERROR_RESPONSE", json.get("type").getAsString());
        assertEquals(400, json.get("statusCode").getAsInt());
        assertEquals("join-req-1", json.get("requestId").getAsString());
    }

    @Test
    void leaveAuctionWithInvalidAuctionIdReturnsBadRequest() {
        JsonObject request = new JsonObject();
        request.addProperty("type", ActionType.LEAVE_AUCTION);
        request.addProperty("auctionId", "abc");

        String response = controller.xuLy(request, null);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        assertEquals("ERROR_RESPONSE", json.get("type").getAsString());
        assertEquals(400, json.get("statusCode").getAsInt());
    }

}
