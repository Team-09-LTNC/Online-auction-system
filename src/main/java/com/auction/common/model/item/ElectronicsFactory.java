package com.auction.common.model.item;

public class ElectronicsFactory extends ItemFactory {
    // Override method của lớp cha cho phù hợp với ElectronicsFactory
    @Override
    Item createItem(ItemAttributes item) {
        return new Electronics(item.getName(), item.getDescription(), item.getStartingPrice(),
                item.getBrand(), item.getWarrantyMonths());

    }
}
