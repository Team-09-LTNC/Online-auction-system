package com.auction.common.model.item;

public class OtherItemFactory extends ItemFactory {
    @Override
    public Item createItem(ItemAttributes item) {
        if (item == null) return null;
        return new OtherItem(
                item.getName(),
                item.getDescription(),
                item.getStartingPrice(),
                item.getCustomCategoryName()
        );
    }
}