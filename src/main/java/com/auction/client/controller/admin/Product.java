package com.auction.client.controller.admin;

import javafx.beans.property.SimpleStringProperty;

public class Product {
  private final SimpleStringProperty productId;
  private final SimpleStringProperty name;
  private final SimpleStringProperty sellerId;
  private final SimpleStringProperty description;
  private final SimpleStringProperty startPrice;
  private final SimpleStringProperty category;
  private final SimpleStringProperty startTime;
  private final SimpleStringProperty endTime;
  private final SimpleStringProperty imageUrl;

  public Product(String productId, String name, String sellerId, String description, String category,
      String startPrice, String startTime, String endTime, String imageUrl) {
    this.productId = new SimpleStringProperty(productId);
    this.name = new SimpleStringProperty(name);
    this.sellerId = new SimpleStringProperty(sellerId);
    this.description = new SimpleStringProperty(description);
    this.category = new SimpleStringProperty(category);
    this.startPrice = new SimpleStringProperty(startPrice);
    this.startTime = new SimpleStringProperty(startTime);
    this.endTime = new SimpleStringProperty(endTime);
    this.imageUrl = new SimpleStringProperty(imageUrl);
  }

  public String getProductId() {
    return productId.get();
  }

  public String getName() {
    return name.get();
  }

  public String getSellerId() {
    return sellerId.get();
  }

  public String getDescription() {
    return description.get();
  }

  public String getStartPrice() {
    return startPrice.get();
  }

  public String getCategory() {
    return category.get();
  }

  public String getStartTime() {
    return startTime.get();
  }

  public String getEndTime() {
    return endTime.get();
  }

  public String getImageUrl() {
    return imageUrl.get();
  }
}
