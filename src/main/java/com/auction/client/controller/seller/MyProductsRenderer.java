package com.auction.client.controller.seller;

import com.auction.client.controller.components.ProductCardController;
import com.auction.client.util.AuctionTimeUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class MyProductsRenderer {
  private static final Logger logger = LoggerFactory.getLogger(MyProductsRenderer.class);
  private static final int CARD_BATCH_SIZE = 10;

  private final Class<?> resourceOwner;
  private final FlowPane productFlowPane;
  private final AtomicInteger renderVersion;
  private final Consumer<JsonObject> editAction;
  private final Consumer<JsonObject> deleteAction;

  MyProductsRenderer(
      Class<?> resourceOwner,
      FlowPane productFlowPane,
      AtomicInteger renderVersion,
      Consumer<JsonObject> editAction,
      Consumer<JsonObject> deleteAction
  ) {
    this.resourceOwner = resourceOwner;
    this.productFlowPane = productFlowPane;
    this.renderVersion = renderVersion;
    this.editAction = editAction;
    this.deleteAction = deleteAction;
  }

  void render(JsonArray products, String keyword, String category, String status, String serverNow) {
    int currentRenderVersion = renderVersion.incrementAndGet();
    String selectedKeyword = keyword.trim().toLowerCase(Locale.ROOT);

    com.auction.client.util.ClientTaskExecutor.execute(() -> {
      try {
        renderInBatches(currentRenderVersion, products, selectedKeyword, category, status, serverNow);
      } catch (Exception ex) {
        logger.error("Lỗi lọc và dựng sản phẩm seller: ", ex);
      }
    });
  }

  private void renderInBatches(
      int currentRenderVersion,
      JsonArray products,
      String keyword,
      String selectedCategory,
      String selectedStatus,
      String serverNow
  ) {
    List<VBox> cardsToRender = new ArrayList<>();
    boolean cardsPublished = false;

    for (JsonElement element : products) {
      JsonObject itemObj = element.getAsJsonObject();
      AuctionTimeUtil.AuctionState state = calculateState(itemObj, serverNow);
      String status = resolveStatus(itemObj, state);
      if (!matchesFilters(itemObj, keyword, selectedCategory, selectedStatus, status)) {
        continue;
      }

      VBox card = createCard(itemObj, state, status);
      if (card == null) {
        continue;
      }
      cardsToRender.add(card);
      if (cardsToRender.size() >= CARD_BATCH_SIZE) {
        publishCardBatch(currentRenderVersion, cardsToRender, !cardsPublished);
        cardsPublished = true;
        cardsToRender = new ArrayList<>();
      }
    }

    publishCardBatch(currentRenderVersion, cardsToRender, !cardsPublished);
  }

  private boolean matchesFilters(
      JsonObject itemObj,
      String keyword,
      String selectedCategory,
      String selectedStatus,
      String status
  ) {
    String name = MyProductsHelper.getString(itemObj, "name", "Sản phẩm không tên");
    if (!keyword.isEmpty() && !name.toLowerCase(Locale.ROOT).contains(keyword)) {
      return false;
    }

    String category = MyProductsHelper.getString(itemObj, "category", "");
    if (!"Tất cả".equalsIgnoreCase(selectedCategory) && !selectedCategory.equalsIgnoreCase(category)) {
      return false;
    }

    if ("Tất cả".equalsIgnoreCase(selectedStatus)) {
      return true;
    }
    if ("FINISHED".equalsIgnoreCase(selectedStatus)) {
      return "FINISHED".equalsIgnoreCase(status) || "PAID".equalsIgnoreCase(status);
    }
    return selectedStatus.equalsIgnoreCase(status);
  }

  private VBox createCard(JsonObject itemObj, AuctionTimeUtil.AuctionState state, String status) {
    int auctionId = itemObj.has("auctionId") ? itemObj.get("auctionId").getAsInt() : -1;
    double currentPrice = itemObj.has("currentPrice") ? itemObj.get("currentPrice").getAsDouble() : 0.0;
    String imageUrl = MyProductsHelper.getString(itemObj, "imageUrl", "");
    String imageThumbUrl = MyProductsHelper.getString(itemObj, "imageThumbUrl", imageUrl);
    com.auction.client.util.ImageCacheManager.preloadPreviewImage(imageThumbUrl);

    try {
      FXMLLoader loader = new FXMLLoader(resourceOwner.getResource("/fxml/components/ProductCard.fxml"));
      VBox card = loader.load();
      ProductCardController controller = loader.getController();
      controller.setProductData(
          auctionId,
          MyProductsHelper.getString(itemObj, "name", "Sản phẩm không tên"),
          currentPrice,
          state.countdownSeconds,
          status,
          imageUrl,
          imageThumbUrl,
          false);
      JsonObject cardData = itemObj.deepCopy();
      boolean canManage = "OPEN".equalsIgnoreCase(status);
      controller.configureSellerActions(
          canManage,
          () -> editAction.accept(cardData),
          () -> deleteAction.accept(cardData));
      return card;
    } catch (IOException e) {
      logger.error("Lỗi vẽ thẻ sản phẩm: {}", e.getMessage());
      return null;
    }
  }

  private void publishCardBatch(int currentRenderVersion, List<VBox> cards, boolean replaceExisting) {
    List<VBox> batch = new ArrayList<>(cards);
    Platform.runLater(() -> {
      if (productFlowPane == null || renderVersion.get() != currentRenderVersion) {
        return;
      }

      if (replaceExisting) {
        productFlowPane.getChildren().setAll(batch);
        if (batch.isEmpty()) {
          productFlowPane.getChildren().add(new Label("Không có sản phẩm phù hợp."));
        }
      } else if (!batch.isEmpty()) {
        productFlowPane.getChildren().addAll(batch);
      }
    });
  }

  private AuctionTimeUtil.AuctionState calculateState(JsonObject itemObj, String serverNow) {
    return AuctionTimeUtil.calculateState(
        MyProductsHelper.getString(itemObj, "startTime", null),
        MyProductsHelper.getString(itemObj, "endTime", null),
        serverNow);
  }

  private String resolveStatus(JsonObject itemObj, AuctionTimeUtil.AuctionState state) {
    return MyProductsHelper.resolveDisplayStatus(
        MyProductsHelper.getString(itemObj, "status", null),
        state.finalStatus);
  }
}
