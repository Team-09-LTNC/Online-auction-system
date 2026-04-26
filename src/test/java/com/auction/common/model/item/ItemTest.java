package com.auction.common.model.item;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ItemTest {

    // -----------------------------------------------------------------------
    // 1. Kiểm tra tạo đúng loại item qua Factory
    // -----------------------------------------------------------------------

    @Test
    void testArtFactory_taoItemDung() {
        ItemAttributes attr = buildAttr("Mona Lisa", "Tranh nổi tiếng", 1000.0);
        attr.setArtist("Da Vinci");
        attr.setYearCreated(1503);
        attr.setMedium("Sơn dầu");

        Item item = new ArtFactory().createItem(attr);

        assertInstanceOf(Art.class, item, "ArtFactory phải tạo ra đối tượng Art");
        assertEquals("ART", item.getItemCategory());
        assertEquals("Mona Lisa", item.getName());
        assertEquals(1000.0, item.getStartingPrice());
        assertEquals("Da Vinci", ((Art) item).getArtist());
        assertEquals(1503, ((Art) item).getYearCreated());
    }

    @Test
    void testElectronicsFactory_taoItemDung() {
        ItemAttributes attr = buildAttr("iPhone 15", "Điện thoại Apple", 20000000.0);
        attr.setBrand("Apple");
        attr.setWarrantyMonths(12);

        Item item = new ElectronicsFactory().createItem(attr);

        assertInstanceOf(Electronics.class, item);
        assertEquals("ELECTRONICS", item.getItemCategory());
        assertEquals("Apple", ((Electronics) item).getBrand());
        assertEquals(12, ((Electronics) item).getWarrantyMonths());
    }

    @Test
    void testVehicleFactory_taoItemDung() {
        ItemAttributes attr = buildAttr("Camry 2023", "Xe sedan", 900000000.0);
        attr.setMake("Toyota");
        attr.setModel("Camry");
        attr.setYear(2023);

        Item item = new VehicleFactory().createItem(attr);

        assertInstanceOf(Vehicle.class, item);
        assertEquals("VEHICLE", item.getItemCategory());
        assertEquals("Toyota", ((Vehicle) item).getMake());
        assertEquals("Camry", ((Vehicle) item).getModel());
        assertEquals(2023, ((Vehicle) item).getYear());
    }

    // -----------------------------------------------------------------------
    // 2. Kiểm tra dữ liệu
    // -----------------------------------------------------------------------

    @Test
    void testItem_getName_traVeDung() {
        ItemAttributes attr = buildAttr("Test Item", "Mô tả", 500.0);
        attr.setBrand("X");
        attr.setWarrantyMonths(0);

        Item item = new ElectronicsFactory().createItem(attr);

        assertEquals("Test Item", item.getName());
        assertEquals("Mô tả", item.getDescription());
        assertEquals(500.0, item.getStartingPrice());
    }

    @Test
    void testItem_idTuDong_khacNhau() {
        ItemAttributes attr = buildAttr("A", "desc", 100.0);
        attr.setBrand("B");
        attr.setWarrantyMonths(1);

        Item item1 = new ElectronicsFactory().createItem(attr);
        Item item2 = new ElectronicsFactory().createItem(attr);

        assertNotEquals(item1.getId(), item2.getId(),
            "Mỗi item phải có UUID khác nhau");
    }

    @Test
    void testItem_idKhongNull() {
        ItemAttributes attr = buildAttr("A", "desc", 100.0);
        attr.setMake("X"); attr.setModel("Y"); attr.setYear(2020);

        Item item = new VehicleFactory().createItem(attr);

        assertNotNull(item.getId(), "ID không được null");
        assertFalse(item.getId().isBlank(), "ID không được rỗng");
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------
    private ItemAttributes buildAttr(String name, String desc, double price) {
        ItemAttributes attr = new ItemAttributes();
        attr.setName(name);
        attr.setDescription(desc);
        attr.setStartingPrice(price);
        return attr;
    }
}
