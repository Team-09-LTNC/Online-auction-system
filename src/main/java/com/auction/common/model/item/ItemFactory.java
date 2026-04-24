package com.auction.common.model.item;

// FACTORY METHOD
// Tạo một "Nhà máy" để tạo ra các nhà máy tương tự cho từng loại sản phẩm cụ thể
public abstract class ItemFactory {
    // Method createItem(Attributes item);
    abstract Item createItem(ItemAttributes item);

}
