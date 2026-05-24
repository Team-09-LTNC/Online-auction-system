package com.auction.client.util;

import javafx.scene.image.Image;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Tải sẵn ảnh và lưu trong RAM
public class ImageCacheManager {
    private static final double PREVIEW_WIDTH = 480;
    private static final double PREVIEW_HEIGHT = 320;
    private static final ConcurrentHashMap<String, Image> imageCache = new ConcurrentHashMap<>();
    private static final Set<String> inFlightPreviewLoads = ConcurrentHashMap.newKeySet();
    private static final ExecutorService preloadExecutor = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "image-preload");
        thread.setDaemon(true);
        return thread;
    });

    public static Image getImage(String imageUrl) {
        return getSizedImage(imageUrl, 0, 0);
    }

    public static Image getPreviewImage(String imageUrl) {
        return getSizedImage(imageUrl, PREVIEW_WIDTH, PREVIEW_HEIGHT);
    }

    public static void preloadPreviewImage(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return;
        }
        String normalizedUrl = imageUrl.trim();
        String cacheKey = normalizedUrl + "#" + (int) PREVIEW_WIDTH + "x" + (int) PREVIEW_HEIGHT;
        if (imageCache.containsKey(cacheKey) || !inFlightPreviewLoads.add(cacheKey)) {
            return;
        }

        preloadExecutor.execute(() -> {
            try {
                getPreviewImage(normalizedUrl);
            } finally {
                inFlightPreviewLoads.remove(cacheKey);
            }
        });
    }

    public static void preloadPreviewImages(Collection<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }
        for (String imageUrl : imageUrls) {
            preloadPreviewImage(imageUrl);
        }
    }

    private static Image getSizedImage(String imageUrl, double requestedWidth, double requestedHeight) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) return null;

        String normalizedUrl = imageUrl.trim();
        String cacheKey = normalizedUrl + "#" + (int) requestedWidth + "x" + (int) requestedHeight;
        return imageCache.computeIfAbsent(cacheKey, key -> new Image(
                normalizedUrl,
                requestedWidth,
                requestedHeight,
                true,
                true,
                true
        ));
    }
}
