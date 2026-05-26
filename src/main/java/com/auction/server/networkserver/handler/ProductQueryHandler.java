package com.auction.server.networkserver.handler;

import com.auction.common.dto.BaseDTOs;
import com.auction.common.enums.ActionType;
import com.auction.common.enums.ErrorCode;
import com.auction.common.enums.StatusCode;
import com.auction.common.model.item.Item;
import com.auction.server.manager.ProductManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.List;

final class ProductQueryHandler {
  private final Gson gson;
  private final ResponseBuilder responseBuilder;

  ProductQueryHandler(Gson gson, ResponseBuilder responseBuilder) {
    this.gson = gson;
    this.responseBuilder = responseBuilder;
  }

  String handleGetAllProducts(JsonObject request) {
    List<Item> products = ProductManager.getInstance().getAllProducts();
    JsonObject payload = new JsonObject();
    payload.addProperty("success", true);
    payload.add("data", gson.toJsonTree(products));
    return responseBuilder.build(request, ActionType.GET_ALL_PRODUCTS, payload);
  }

  String handleSearchProduct(JsonObject request) {
    if (!request.has("keyword") || request.get("keyword").isJsonNull()) {
      return responseBuilder.build(request, ActionType.SEARCH_PRODUCT,
          new BaseDTOs.ErrorResponse(StatusCode.BAD_REQUEST, "Thieu keyword.", ErrorCode.BAD_REQUEST));
    }
    String keyword = request.get("keyword").getAsString();
    List<Item> result = ProductManager.getInstance().searchProductsByKeyword(keyword);

    JsonObject payload = new JsonObject();
    payload.addProperty("success", true);
    payload.addProperty("message", "Tìm thấy " + result.size() + " sản phẩm.");
    payload.add("data", gson.toJsonTree(result));
    return responseBuilder.build(request, ActionType.SEARCH_PRODUCT, payload);
  }

  @FunctionalInterface
  interface ResponseBuilder {
    String build(JsonObject request, String responseType, Object payload);
  }
}
