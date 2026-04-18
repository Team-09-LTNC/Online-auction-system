package com.auction.model.item;

public class ElectronicsFactory extends ItemFactory {
    @Override
    Item createItem(ItemAttributes item) {
        return new Electronics(item.getName(), item.getDescription(), item.getStartingPrice(),
                item.getBrand(), item.getWarrantyMonths());

    }
}
