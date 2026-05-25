package com.auction.server.manager;

import com.auction.common.model.item.Art;
import com.auction.common.model.item.Electronics;
import com.auction.common.model.item.Item;
import com.auction.common.model.item.ItemAttributes;
import com.auction.common.model.item.OtherItem;
import com.auction.common.model.item.Vehicle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProductManagerTest {

    @Test
    void createProductReturnsExpectedSubtypeByCategory() {
        ProductManager manager = ProductManager.getInstance();
        ItemAttributes attrs = new ItemAttributes();
        attrs.setName("Item");
        attrs.setDescription("desc");
        attrs.setStartingPrice(1000L);

        Item e = manager.createProduct("ELECTRONICS", attrs);
        Item a = manager.createProduct("ART", attrs);
        Item v = manager.createProduct("VEHICLE", attrs);
        Item o = manager.createProduct("OTHER", attrs);

        assertNotNull(e);
        assertNotNull(a);
        assertNotNull(v);
        assertNotNull(o);
        assertInstanceOf(Electronics.class, e);
        assertInstanceOf(Art.class, a);
        assertInstanceOf(Vehicle.class, v);
        assertInstanceOf(OtherItem.class, o);
    }

    @Test
    void createProductReturnsNullWhenInputInvalid() {
        ProductManager manager = ProductManager.getInstance();
        ItemAttributes attrs = new ItemAttributes();
        attrs.setName("Item");
        attrs.setDescription("desc");
        attrs.setStartingPrice(1000L);

        assertNull(manager.createProduct("UNKNOWN", attrs));
        assertNull(manager.createProduct("ELECTRONICS", null));
    }
}
