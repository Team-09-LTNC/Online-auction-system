package com.auction.common.model.item;

// Áp dụng Factory Method để khởi tạo các loại sản phẩm
public abstract class ItemFactory {
    public abstract Item createItem(ItemAttributes item);
}