package com.auction.server.networkserver;

import com.auction.server.networkserver.handler.RequestHandler;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RequestDispatcherTest {

    @Test
    void unknownActionReturnsErrorPayload() {
        RequestDispatcher dispatcher = RequestDispatcher.layInstance();

        JsonObject request = new JsonObject();
        request.addProperty("type", "NOT_EXISTING_ACTION");
        String response = dispatcher.dieuPhoi("NOT_EXISTING_ACTION", request, null);

        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        assertFalse(json.get("success").getAsBoolean());
        assertEquals("ERR_UNKNOWN", json.get("errorCode").getAsString());
    }

    @Test
    void nullHandlerResponseReturnsFallbackErrorAndPreservesRequestId() throws Exception {
        RequestDispatcher dispatcher = RequestDispatcher.layInstance();

        Field field = RequestDispatcher.class.getDeclaredField("danhSachTrinhXuLy");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, RequestHandler> handlers = (Map<String, RequestHandler>) field.get(dispatcher);

        String action = "TEST_NULL_RESPONSE_ACTION";
        handlers.put(action, (req, client) -> null);
        try {
            JsonObject request = new JsonObject();
            request.addProperty("type", action);
            request.addProperty("requestId", "req-123");

            String response = dispatcher.dieuPhoi(action, request, null);
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();

            assertEquals("ERROR_RESPONSE", json.get("type").getAsString());
            assertFalse(json.get("success").getAsBoolean());
            assertEquals("ERR_HANDLER_NO_RESPONSE", json.get("errorCode").getAsString());
            assertEquals("req-123", json.get("requestId").getAsString());
        } finally {
            handlers.remove(action);
        }
    }
}
