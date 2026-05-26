package com.auction.client.controller.bidder;

import com.auction.client.util.ImageCacheManager;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

final class AuctionRoomImageLoader {
    private final ImageView imageView;

    AuctionRoomImageLoader(ImageView imageView) {
        this.imageView = imageView;
    }

    void load(String detailUrl, String fallbackUrl) {
        if (imageView == null) {
            return;
        }

        String normalizedDetailUrl = normalizeImageUrl(detailUrl);
        String normalizedFallbackUrl = normalizeImageUrl(fallbackUrl);
        String primaryUrl = normalizedDetailUrl != null ? normalizedDetailUrl : normalizedFallbackUrl;
        if (primaryUrl == null) {
            imageView.setImage(null);
            return;
        }

        if (normalizedFallbackUrl != null) {
            imageView.setImage(ImageCacheManager.getPreviewImage(normalizedFallbackUrl));
        }

        Image image = ImageCacheManager.getDetailImage(primaryUrl);
        if (image == null || image.isError()) {
            setFallbackImage(normalizedFallbackUrl);
            return;
        }

        if (normalizedFallbackUrl == null) {
            imageView.setImage(image);
        }

        if (ImageCacheManager.isImageReady(image)) {
            imageView.setImage(image);
            return;
        }

        image.progressProperty().addListener((obs, oldProgress, newProgress) -> {
            if (newProgress.doubleValue() >= 1.0 && !image.isError()) {
                Platform.runLater(() -> imageView.setImage(image));
            }
        });
        image.errorProperty().addListener((obs, wasError, isError) -> {
            if (Boolean.TRUE.equals(isError)) {
                Platform.runLater(() -> setFallbackImage(normalizedFallbackUrl));
            }
        });
    }

    private void setFallbackImage(String fallbackUrl) {
        String normalizedFallbackUrl = normalizeImageUrl(fallbackUrl);
        if (normalizedFallbackUrl == null) {
            imageView.setImage(null);
            return;
        }
        imageView.setImage(ImageCacheManager.getPreviewImage(normalizedFallbackUrl));
    }

    private String normalizeImageUrl(String url) {
        return url == null || url.trim().isEmpty() ? null : url.trim();
    }
}
