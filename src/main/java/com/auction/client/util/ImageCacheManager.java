package com.auction.client.util;

import javafx.scene.image.Image;
import java.util.concurrent.ConcurrentHashMap;

// Tải sẵn ảnh và lưu trong RAM
public class ImageCacheManager {
    private static final double PREVIEW_WIDTH = 480;
    private static final double PREVIEW_HEIGHT = 320;
    private static final ConcurrentHashMap<String, Image> imageCache = new ConcurrentHashMap<>();

    public static Image getImage(String imageUrl) {
        return getSizedImage(imageUrl, 0, 0);
    }

    public static Image getPreviewImage(String imageUrl) {
        return getSizedImage(imageUrl, PREVIEW_WIDTH, PREVIEW_HEIGHT);
    }

    public static void preloadPreviewImage(String imageUrl) {
        getPreviewImage(imageUrl);
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
