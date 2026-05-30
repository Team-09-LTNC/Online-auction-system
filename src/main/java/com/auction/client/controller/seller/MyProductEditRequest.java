package com.auction.client.controller.seller;

import java.io.File;
import java.time.LocalDate;

record MyProductEditRequest(
    String name,
    String description,
    String priceText,
    String category,
    String imageUrl,
    String imageThumbUrl,
    File imageFile,
    LocalDate startDate,
    String startTimeText,
    LocalDate endDate,
    String endTimeText
) {
}
