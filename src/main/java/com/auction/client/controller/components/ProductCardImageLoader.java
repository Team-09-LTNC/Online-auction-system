package com.auction.client.controller.components;

import com.auction.client.util.ImageCacheManager;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;

final class ProductCardImageLoader {
  private ProductCardImageLoader() {
  }

  static void load(ImageView target, String imageUrl, String imageThumbUrl, Logger logger) {
    if (target == null) {
      return;
    }
    try {
      target.setImage(ImageCacheManager.getPreviewImage(resolvePreviewUrl(imageUrl, imageThumbUrl)));
      preload(imageUrl, imageThumbUrl);
    } catch (Exception e) {
      logger.error("Lỗi load ảnh", e);
      target.setImage(null);
    }
  }

  static void preload(String imageUrl, String imageThumbUrl) {
    if (imageThumbUrl != null && !imageThumbUrl.isBlank()) {
      ImageCacheManager.preloadPreviewImage(imageThumbUrl);
    }
    if (imageUrl != null && !imageUrl.isBlank()) {
      ImageCacheManager.preloadDetailImage(imageUrl);
    }
  }

  private static String resolvePreviewUrl(String imageUrl, String imageThumbUrl) {
    return imageThumbUrl != null && !imageThumbUrl.isBlank() ? imageThumbUrl : imageUrl;
  }
}
