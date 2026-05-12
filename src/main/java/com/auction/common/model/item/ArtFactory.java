package com.auction.common.model.item;

public class ArtFactory extends ItemFactory {
    @Override
    public Item createItem(ItemAttributes item) {
        if (item == null) {
            return null;
        }
        return new Art(
                item.getName(),
                item.getDescription(),
                item.getStartingPrice(),
                item.getArtist(),
                item.getYearCreated(),
                item.getMedium()
        );
    }
}