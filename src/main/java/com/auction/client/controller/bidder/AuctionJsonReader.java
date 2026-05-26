package com.auction.client.controller.bidder;

import com.google.gson.JsonObject;

final class AuctionJsonReader {
    private AuctionJsonReader() {
    }

    static boolean hasValue(JsonObject obj, String key) {
        return obj != null && obj.has(key) && !obj.get(key).isJsonNull();
    }

    static String getString(JsonObject obj, String key, String fallback) {
        return hasValue(obj, key) ? obj.get(key).getAsString() : fallback;
    }

    static long getLong(JsonObject obj, String key, long fallback) {
        if (!hasValue(obj, key)) {
            return fallback;
        }
        try {
            return obj.get(key).getAsLong();
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    static boolean getBoolean(JsonObject obj, String key, boolean fallback) {
        if (!hasValue(obj, key)) {
            return fallback;
        }
        try {
            return obj.get(key).getAsBoolean();
        } catch (RuntimeException e) {
            return fallback;
        }
    }
}
