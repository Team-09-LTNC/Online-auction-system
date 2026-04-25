package com.auction.common.model.item;

/**
 * FACTORY METHOD Pattern.
 * BUG FIX: createItem() trước là package-private (không có modifier).
 * ItemDao nằm ở package server.dao -> không gọi được -> compile error.
 * Sửa thành public.
 */
public abstract class ItemFactory {
    public abstract Item createItem(ItemAttributes item);
}
