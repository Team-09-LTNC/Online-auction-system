package com.auction.model.item;

public class ArtFactory extends ItemFactory {
    // Override method của lớp cha cho phù hợp với ArtFactory
    @Override
    Item createItem(ItemAttributes item) {

        return new Art(item.getName(), item.getDescription(), item.getStartingPrice(), item.getArtist(),
                item.getYearCreated(), item.getMedium());

    }

}
