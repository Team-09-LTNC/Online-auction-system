package com.auction.model.item;

public class ArtFactory extends ItemFactory {

    @Override
    Item createItem(ItemAttributes item) {

        return new Art(item.getName(), item.getDescription(), item.getStartingPrice(), item.getArtist(),
                item.getYearCreated(), item.getMedium());

    }

}
