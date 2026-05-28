package com.auction.common.model.item;

public class VehicleFactory extends ItemFactory {

    @Override
    public Item createItem(ItemAttributes item) {
        // Kiểm tra lỗi nếu null
        if (item == null) {
            return null;
        }

        // Khởi tạo và trả về đối tượng Vehicle cụ thể
        return new Vehicle(
                item.getName(),
                item.getDescription(),
                item.getStartingPrice(),
                item.getMake(),
                item.getModel(),
                item.getYear()
        );
    }
}
