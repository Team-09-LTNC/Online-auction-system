package com.auction.client.util;

import javafx.scene.image.Image;
import java.util.concurrent.ConcurrentHashMap;
// Tải sẵn ảnh và lưu trong RAM
public class ImageCacheManager {
    private static final ConcurrentHashMap<String, Image> imageCache = new ConcurrentHashMap<>();

    public static Image getImage(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) return null;

        return imageCache.computeIfAbsent(imageUrl, url -> new Image(url, true));
    }
}