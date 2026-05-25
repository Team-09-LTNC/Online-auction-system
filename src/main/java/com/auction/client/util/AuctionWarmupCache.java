package com.auction.client.util;

import com.auction.client.networkclient.ClientSocket;
import com.auction.common.enums.ActionType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AuctionWarmupCache {
    private static final String ALL_AUCTIONS = "ALL_AUCTIONS";
    private static final String FEATURED_AUCTIONS = "FEATURED_AUCTIONS";
    private static final Set<String> inFlightRequests = ConcurrentHashMap.newKeySet();

    private static volatile JsonObject allAuctionsResponse;
    private static volatile JsonObject featuredAuctionsResponse;

    private AuctionWarmupCache() {
    }

    public static void warmAfterLogin(String role) {
        if (!"BIDDER".equalsIgnoreCase(role)) {
            return;
        }
        requestAllAuctions();
        requestFeaturedAuctions();
    }

    public static JsonObject getAllAuctionsResponse() {
        return copy(allAuctionsResponse);
    }

    public static JsonObject getFeaturedAuctionsResponse() {
        return copy(featuredAuctionsResponse);
    }

    public static void storeAllAuctions(JsonObject response) {
        if (!isSuccessfulAuctionResponse(response)) {
            return;
        }
        allAuctionsResponse = copy(response);
        preloadImages(response);
    }

    public static void storeFeaturedAuctions(JsonObject response) {
        if (!isSuccessfulAuctionResponse(response)) {
            return;
        }
        featuredAuctionsResponse = copy(response);
        preloadImages(response);
    }

    public static void clear() {
        allAuctionsResponse = null;
        featuredAuctionsResponse = null;
        inFlightRequests.clear();
    }

    private static void requestAllAuctions() {
        if (!inFlightRequests.add(ALL_AUCTIONS)) {
            return;
        }

        JsonObject request = new JsonObject();
        request.addProperty("type", ActionType.GET_ALL_AUCTIONS);
        request.addProperty("category", "ALL");
        request.addProperty("requestId", UUID.randomUUID().toString());

        ClientSocket.getInstance().sendJsonRequest(request, "AUCTION_LIST_RESPONSE", response -> {
            inFlightRequests.remove(ALL_AUCTIONS);
            if (isSuccessfulAuctionResponse(response)) {
                storeAllAuctions(response);
            }
        });
    }

    private static void requestFeaturedAuctions() {
        if (!inFlightRequests.add(FEATURED_AUCTIONS)) {
            return;
        }

        JsonObject request = new JsonObject();
        request.addProperty("type", ActionType.GET_ALL_AUCTIONS);
        request.addProperty("featuredRunning", true);
        request.addProperty("requestId", UUID.randomUUID().toString());

        ClientSocket.getInstance().sendJsonRequest(request, "AUCTION_LIST_RESPONSE", response -> {
            inFlightRequests.remove(FEATURED_AUCTIONS);
            if (isSuccessfulAuctionResponse(response)) {
                storeFeaturedAuctions(response);
            }
        });
    }

    private static boolean isSuccessfulAuctionResponse(JsonObject response) {
        return response != null
                && response.has("success")
                && response.get("success").getAsBoolean()
                && response.has("auctions")
                && response.get("auctions").isJsonArray();
    }

    private static void preloadImages(JsonObject response) {
        if (response == null || !response.has("auctions") || !response.get("auctions").isJsonArray()) {
            return;
        }

        JsonArray auctions = response.getAsJsonArray("auctions");
        List<String> imageUrls = new ArrayList<>();
        for (JsonElement element : auctions) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject auction = element.getAsJsonObject();
            if (auction.has("imageThumbUrl") && !auction.get("imageThumbUrl").isJsonNull()) {
                imageUrls.add(auction.get("imageThumbUrl").getAsString());
            } else if (auction.has("imageUrl") && !auction.get("imageUrl").isJsonNull()) {
                imageUrls.add(auction.get("imageUrl").getAsString());
            }
        }
        ImageCacheManager.preloadPreviewImages(imageUrls);
    }

    private static JsonObject copy(JsonObject object) {
        return object == null ? null : object.deepCopy();
    }
}
