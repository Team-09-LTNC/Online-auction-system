package com.auction.client.controller.seller;

import java.time.LocalDate;

record MyProductEditRequest(
    String name,
    String description,
    String priceText,
    String category,
    String imageUrl,
    LocalDate startDate,
    String startTimeText,
    LocalDate endDate,
    String endTimeText
) {
}
