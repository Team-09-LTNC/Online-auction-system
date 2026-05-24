package com.auction.server.networkserver.handler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthControllerTest {

    private final AuthController controller = new AuthController();

    @Test
    void nullRequestReturnsBadRequestError() {
        String response = controller.xuLy(null, null);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        assertEquals(400, json.get("statusCode").getAsInt());
        assertEquals("ERR_BAD_REQUEST", json.get("errorCode").getAsString());
    }

    @Test
    void missingTypeReturnsBadRequestError() {
        JsonObject request = new JsonObject();
        request.addProperty("requestId", "req-auth-1");

        String response = controller.xuLy(request, null);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        assertEquals(400, json.get("statusCode").getAsInt());
        assertEquals("ERR_BAD_REQUEST", json.get("errorCode").getAsString());
    }

    @Test
    void unsupportedActionReturnsBadRequestError() {
        JsonObject request = new JsonObject();
        request.addProperty("type", "UNKNOWN_AUTH_ACTION");

        String response = controller.xuLy(request, null);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        assertEquals(400, json.get("statusCode").getAsInt());
        assertEquals("ERR_BAD_REQUEST", json.get("errorCode").getAsString());
    }
}
