package com.auction.common.model.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemFactoryTest {

    @Test
    void factoriesReturnNullWhenAttributesNull() {
        assertNull(new ArtFactory().createItem(null));
        assertNull(new ElectronicsFactory().createItem(null));
        assertNull(new VehicleFactory().createItem(null));
        assertNull(new OtherItemFactory().createItem(null));
    }

    @Test
    void artFactoryMapsAllFields() {
        ItemAttributes attrs = new ItemAttributes();
        attrs.setName("Mona Lisa");
        attrs.setDescription("Painting");
        attrs.setStartingPrice(1_000L);
        attrs.setArtist("Da Vinci");
        attrs.setYearCreated(1503);
        attrs.setMedium("Oil");

        Item item = new ArtFactory().createItem(attrs);

        assertInstanceOf(Art.class, item);
        Art art = (Art) item;
        assertEquals("ART", art.getItemCategory());
        assertEquals("Mona Lisa", art.getName());
        assertEquals("Da Vinci", art.getArtist());
        assertEquals(1503, art.getYearCreated());
        assertEquals("Oil", art.getMedium());
    }

    @Test
    void electronicsVehicleAndOtherFactoriesMapFields() {
        ItemAttributes eAttrs = new ItemAttributes();
        eAttrs.setName("iPhone");
        eAttrs.setDescription("Phone");
        eAttrs.setStartingPrice(500L);
        eAttrs.setBrand("Apple");
        eAttrs.setWarrantyMonths(24);
        Electronics e = (Electronics) new ElectronicsFactory().createItem(eAttrs);
        assertEquals("ELECTRONICS", e.getItemCategory());
        assertEquals("Apple", e.getBrand());
        assertEquals(24, e.getWarrantyMonths());

        ItemAttributes vAttrs = new ItemAttributes();
        vAttrs.setName("Camry");
        vAttrs.setDescription("Car");
        vAttrs.setStartingPrice(1_500L);
        vAttrs.setMake("Toyota");
        vAttrs.setModel("Camry");
        vAttrs.setYear(2025);
        Vehicle v = (Vehicle) new VehicleFactory().createItem(vAttrs);
        assertEquals("VEHICLE", v.getItemCategory());
        assertEquals("Toyota", v.getMake());
        assertEquals("Camry", v.getModel());
        assertEquals(2025, v.getYear());

        ItemAttributes oAttrs = new ItemAttributes();
        oAttrs.setName("Watch");
        oAttrs.setDescription("Luxury");
        oAttrs.setStartingPrice(800L);
        oAttrs.setCustomCategoryName("Fashion");
        OtherItem o = (OtherItem) new OtherItemFactory().createItem(oAttrs);
        assertEquals("OTHER", o.getItemCategory());
        assertEquals("Fashion", o.getCustomCategoryName());
    }
}
