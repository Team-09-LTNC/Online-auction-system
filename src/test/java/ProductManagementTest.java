// File: src/test/java/com/auction/server/test/ProductManagementTest.java

import com.auction.common.dto.ItemDTOs;
import com.auction.common.model.item.*;
import com.auction.server.dao.ItemDao;
import com.auction.server.db.DatabaseConnection;
import com.auction.server.manager.ProductManager;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test các chức năng: CRUD sản phẩm, Factory Pattern cho Item
 *
 * Cách chạy: mvn test -Dtest=ProductManagementTest
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProductManagementTest {

    private static ProductManager productManager;
    private static ItemDao itemDao;
    private static int testSellerId = 1; // Giả sử đã có seller

    @BeforeAll
    static void setup() {
        System.out.println("\n=== SETUP PRODUCT TESTS ===");
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {

            // Clean test data
            stmt.execute("DELETE FROM items WHERE name LIKE 'Test_%' OR name LIKE 'Test %'");
            System.out.println(">> Cleaned old test items");

            // Ensure seller exists
            stmt.execute("INSERT IGNORE INTO users (id, username, password, full_name, role, balance) " +
                    "VALUES (999, 'test_seller_product', 'pass', 'Product Test Seller', 'SELLER', 1000000)");

        } catch (Exception e) {
            System.err.println("Setup error: " + e.getMessage());
        }

        productManager = ProductManager.getInstance();
        itemDao = new ItemDao();
    }

    @AfterAll
    static void cleanup() {
        System.out.println("\n=== CLEANUP PRODUCT TESTS ===");
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("DELETE FROM items WHERE name LIKE 'Test_%' OR name LIKE 'Test %'");
            stmt.execute("DELETE FROM users WHERE username = 'test_seller_product'");
            System.out.println(">> Cleaned test products");

        } catch (Exception e) {
            System.err.println("Cleanup error: " + e.getMessage());
        }
    }

    // ==================== FACTORY PATTERN TESTS ====================

    @Test
    @Order(1)
    @DisplayName("TC01: Factory tạo Electronics")
    void testFactory_CreateElectronics() {
        System.out.println("\n--- TC01: Factory - Electronics ---");

        ItemAttributes attrs = new ItemAttributes();
        attrs.setName("Test Electronics");
        attrs.setDescription("Test Description");
        attrs.setStartingPrice(1000000);
        attrs.setBrand("Samsung");
        attrs.setWarrantyMonths(24);

        Item item = productManager.taoSanPham("ELECTRONICS", attrs);

        assertNotNull(item, "Item không được null");
        assertTrue(item instanceof Electronics, "Phải là Electronics");
        assertEquals("ELECTRONICS", ((Electronics)item).getItemCategory());
        assertEquals("Samsung", ((Electronics)item).getBrand());
        assertEquals(24, ((Electronics)item).getWarrantyMonths());

        System.out.println(">> PASS: Factory tạo Electronics đúng");
    }

    @Test
    @Order(2)
    @DisplayName("TC02: Factory tạo Art")
    void testFactory_CreateArt() {
        System.out.println("\n--- TC02: Factory - Art ---");

        ItemAttributes attrs = new ItemAttributes();
        attrs.setName("Test Art");
        attrs.setDescription("A beautiful painting");
        attrs.setStartingPrice(5000000);
        attrs.setArtist("Van Gogh");
        attrs.setYearCreated(1889);
        attrs.setMedium("Oil on canvas");

        Item item = productManager.taoSanPham("ART", attrs);

        assertNotNull(item);
        assertTrue(item instanceof Art);
        assertEquals("Van Gogh", ((Art)item).getArtist());
        assertEquals(1889, ((Art)item).getYearCreated());

        System.out.println(">> PASS: Factory tạo Art đúng");
    }

    @Test
    @Order(3)
    @DisplayName("TC03: Factory tạo Vehicle")
    void testFactory_CreateVehicle() {
        System.out.println("\n--- TC03: Factory - Vehicle ---");

        ItemAttributes attrs = new ItemAttributes();
        attrs.setName("Test Vehicle");
        attrs.setDescription("Luxury car");
        attrs.setStartingPrice(500000000);
        attrs.setMake("Toyota");
        attrs.setModel("Camry");
        attrs.setYear(2024);

        Item item = productManager.taoSanPham("VEHICLE", attrs);

        assertNotNull(item);
        assertTrue(item instanceof Vehicle);
        assertEquals("Toyota", ((Vehicle)item).getMake());
        assertEquals("Camry", ((Vehicle)item).getModel());

        System.out.println(">> PASS: Factory tạo Vehicle đúng");
    }

    @Test
    @Order(4)
    @DisplayName("TC04: Factory tạo OtherItem")
    void testFactory_CreateOtherItem() {
        System.out.println("\n--- TC04: Factory - OtherItem ---");

        ItemAttributes attrs = new ItemAttributes();
        attrs.setName("Test Other");
        attrs.setDescription("Custom item");
        attrs.setStartingPrice(500000);
        attrs.setCustomCategoryName("Thời trang");

        Item item = productManager.taoSanPham("OTHER", attrs);

        assertNotNull(item);
        assertTrue(item instanceof OtherItem);
        assertEquals("Thời trang", ((OtherItem)item).getCustomCategoryName());

        System.out.println(">> PASS: Factory tạo OtherItem đúng");
    }

    @Test
    @Order(5)
    @DisplayName("TC05: Factory với category không hợp lệ")
    void testFactory_InvalidCategory() {
        System.out.println("\n--- TC05: Factory - Invalid Category ---");

        ItemAttributes attrs = new ItemAttributes();
        attrs.setName("Test Invalid");
        attrs.setStartingPrice(100000);

        Item item = productManager.taoSanPham("INVALID_TYPE", attrs);

        assertNull(item, "Phải trả về null cho category không hợp lệ");

        System.out.println(">> PASS: Từ chối category không hợp lệ");
    }

    @Test
    @Order(6)
    @DisplayName("TC06: Factory với attributes null")
    void testFactory_NullAttributes() {
        System.out.println("\n--- TC06: Factory - Null Attributes ---");

        Item item = productManager.taoSanPham("ELECTRONICS", null);
        assertNull(item, "Phải trả về null khi attributes null");

        System.out.println(">> PASS: Xử lý null attributes");
    }

    // ==================== CRUD OPERATIONS ====================

    @Test
    @Order(10)
    @DisplayName("TC07: Thêm sản phẩm mới vào database")
    void testCRUD_CreateItem() {
        System.out.println("\n--- TC07: Thêm sản phẩm ---");

        ItemAttributes attrs = new ItemAttributes();
        attrs.setName("Test Product For Sale");
        attrs.setDescription("This is a test product");
        attrs.setStartingPrice(1000000);
        attrs.setBrand("Apple");
        attrs.setWarrantyMonths(12);

        Electronics item = (Electronics) productManager.taoSanPham("ELECTRONICS", attrs);
        item.setSellerId(testSellerId);
        item.setCategory("ELECTRONICS");
        item.setImageUrl("http://test.com/image.jpg");

        boolean result = productManager.dangBanSanPham(item);
        assertTrue(result, "Phải thêm sản phẩm thành công");

        System.out.println(">> PASS: Thêm sản phẩm vào DB thành công");
    }

    @Test
    @Order(11)
    @DisplayName("TC08: Lấy tất cả sản phẩm")
    void testCRUD_GetAllItems() {
        System.out.println("\n--- TC08: Lấy tất cả sản phẩm ---");

        List<Item> items = productManager.layTatCaSanPham();

        assertNotNull(items);
        assertTrue(items.size() > 0, "Phải có ít nhất 1 sản phẩm");

        // Kiểm tra các sản phẩm test của chúng ta
        boolean foundTestProduct = items.stream()
                .anyMatch(item -> item.getName().startsWith("Test"));

        assertTrue(foundTestProduct, "Phải tìm thấy sản phẩm test");

        System.out.println(">> PASS: Tìm thấy " + items.size() + " sản phẩm");
    }

    @Test
    @Order(12)
    @DisplayName("TC09: Tìm sản phẩm theo ID")
    void testCRUD_GetItemById() {
        System.out.println("\n--- TC09: Tìm sản phẩm theo ID ---");

        // Lấy tất cả, tìm ID của item test đầu tiên
        List<Item> items = itemDao.layTatCaSanPham();
        Item testItem = items.stream()
                .filter(item -> item.getName().equals("Test Product For Sale"))
                .findFirst()
                .orElse(null);

        assertNotNull(testItem, "Phải có sản phẩm test trong DB");

        int itemId = testItem.getId();
        Item foundItem = productManager.laySanPhamTheoId(itemId);

        assertNotNull(foundItem);
        assertEquals(itemId, foundItem.getId());
        assertEquals("Test Product For Sale", foundItem.getName());

        System.out.println(">> PASS: Tìm thấy sản phẩm ID=" + itemId);
    }

    @Test
    @Order(13)
    @DisplayName("TC10: Tìm kiếm sản phẩm (Search)")
    void testCRUD_SearchItem() {
        System.out.println("\n--- TC10: Tìm kiếm sản phẩm ---");

        List<Item> results = itemDao.searchItem("For Sale");

        assertNotNull(results);
        assertTrue(results.size() > 0, "Phải tìm thấy ít nhất 1 kết quả");

        boolean found = results.stream()
                .anyMatch(item -> item.getName().contains("For Sale"));
        assertTrue(found);

        System.out.println(">> PASS: Tìm thấy " + results.size() + " kết quả");
    }

    @Test
    @Order(14)
    @DisplayName("TC11: Cập nhật sản phẩm")
    void testCRUD_UpdateItem() {
        System.out.println("\n--- TC11: Cập nhật sản phẩm ---");

        // Tìm item test
        List<Item> items = itemDao.layTatCaSanPham();
        Item testItem = items.stream()
                .filter(item -> item.getName().equals("Test Product For Sale"))
                .findFirst()
                .orElse(null);
        assertNotNull(testItem);

        // Update
        testItem.setName("Test Product Updated");
        testItem.setDescription("Updated description");
        testItem.setStartingPrice(2000000);

        boolean updated = itemDao.updateSanPham(testItem);
        assertTrue(updated, "Cập nhật phải thành công");

        // Verify
        Item updatedItem = itemDao.laySanPhamTheoId(testItem.getId());
        assertEquals("Test Product Updated", updatedItem.getName());
        assertEquals(2000000L, updatedItem.getStartingPrice());

        System.out.println(">> PASS: Cập nhật sản phẩm thành công");
    }

    @Test
    @Order(15)
    @DisplayName("TC12: Xóa sản phẩm")
    void testCRUD_DeleteItem() {
        System.out.println("\n--- TC12: Xóa sản phẩm ---");

        // Tìm item đã update
        List<Item> items = itemDao.layTatCaSanPham();
        Item testItem = items.stream()
                .filter(item -> item.getName().equals("Test Product Updated"))
                .findFirst()
                .orElse(null);

        if (testItem != null) {
            boolean deleted = productManager.xoaSanPham(testItem.getId());
            assertTrue(deleted, "Xóa phải thành công");

            // Verify deleted
            Item deletedItem = itemDao.laySanPhamTheoId(testItem.getId());
            assertNull(deletedItem, "Sản phẩm đã bị xóa phải trả về null");

            System.out.println(">> PASS: Xóa sản phẩm thành công");
        } else {
            System.out.println(">> SKIP: Không tìm thấy sản phẩm để xóa");
        }
    }

    // ==================== VALIDATION TESTS ====================

    @Test
    @Order(20)
    @DisplayName("TC13: Không cho phép giá khởi điểm <= 0")
    void testValidation_InvalidPrice() {
        System.out.println("\n--- TC13: Validation - Giá khởi điểm <= 0 ---");

        Electronics item = new Electronics();
        item.setName("Invalid Price Item");
        item.setStartingPrice(0); // Invalid

        boolean result = productManager.dangBanSanPham(item);
        assertFalse(result, "Phải từ chối giá khởi điểm <= 0");

        System.out.println(">> PASS: Từ chối giá khởi điểm không hợp lệ");
    }

    @Test
    @Order(21)
    @DisplayName("TC14: Tìm sản phẩm không tồn tại")
    void testEdgeCase_ItemNotFound() {
        System.out.println("\n--- TC14: Edge Case - Item không tồn tại ---");

        Item item = productManager.laySanPhamTheoId(999999);
        assertNull(item, "Phải trả về null cho ID không tồn tại");

        System.out.println(">> PASS: Xử lý đúng khi item không tồn tại");
    }
}