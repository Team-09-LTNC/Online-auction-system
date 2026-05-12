package com.auction.common.model.item;

public class ElectronicsFactory extends ItemFactory {
    @Override
    public Item createItem(ItemAttributes item) {
        if (item == null) {
            return null;
        }
        return new Electronics(
                item.getName(),
                item.getDescription(),
                item.getStartingPrice(),
                item.getBrand(),
                item.getWarrantyMonths()
        );
    }
}