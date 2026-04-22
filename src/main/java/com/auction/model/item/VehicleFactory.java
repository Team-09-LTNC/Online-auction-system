package com.auction.model.item;

public class VehicleFactory extends ItemFactory {
    // Override method của lớp cha cho phù hợp với VehicleFactory
    @Override
    Item createItem(ItemAttributes item) {
        return new Vehicle(item.getName(), item.getDescription(), item.getStartingPrice(),
                item.getMake(), item.getModel(), item.getYear());

    }

}
