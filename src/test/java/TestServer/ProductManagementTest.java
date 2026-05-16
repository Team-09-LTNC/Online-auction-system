package TestServer;

import com.auction.common.model.item.*;
import com.auction.common.model.user.Seller;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("📦 Kiểm tra Quản lý Sản phẩm")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductManagementTest {

    private Seller seller;
    private List<Item> productList;

    @BeforeEach
    void setUp() {
        seller = new Seller("seller", "pass", "Người bán");
        seller.setId(1);
        productList = new ArrayList<>();
    }

    @Test
    @Order(1)
    @DisplayName("✅ Tạo sản phẩm Electronics")
    void testCreateElectronics() {
        Electronics laptop = new Electronics();
        laptop.setId(1);
        laptop.setName("MacBook Pro M3");
        laptop.setDescription("Laptop cao cấp 2024");
        laptop.setStartingPrice(25000000L);
        laptop.setBidIncrement(1000000L);
        laptop.setSellerId(seller.getId());
        laptop.setCategory("ELECTRONICS");
        laptop.setBrand("Apple");
        laptop.setWarrantyMonths(12);

        assertThat(laptop.getName()).isEqualTo("MacBook Pro M3");
        assertThat(laptop.getStartingPrice()).isEqualTo(25000000L);
        assertThat(laptop.getItemCategory()).isEqualTo("ELECTRONICS");
        assertThat(laptop.getBrand()).isEqualTo("Apple");

        productList.add(laptop);
    }

    @Test
    @Order(2)
    @DisplayName("✅ Tạo sản phẩm Art")
    void testCreateArt() {
        Art painting = new Art();
        painting.setId(2);
        painting.setName("Mona Lisa");
        painting.setDescription("Tranh sơn dầu");
        painting.setStartingPrice(100000000L);
        painting.setBidIncrement(5000000L);
        painting.setSellerId(seller.getId());
        painting.setCategory("ART");
        painting.setArtist("Leonardo da Vinci");
        painting.setYearCreated(1503);

        assertThat(painting.getName()).isEqualTo("Mona Lisa");
        assertThat(painting.getArtist()).isEqualTo("Leonardo da Vinci");
        assertThat(painting.getItemCategory()).isEqualTo("ART");

        productList.add(painting);
    }

    @Test
    @Order(3)
    @DisplayName("✅ Tạo sản phẩm Vehicle")
    void testCreateVehicle() {
        Vehicle car = new Vehicle();
        car.setId(3);
        car.setName("Toyota Camry");
        car.setDescription("Xe sedan");
        car.setStartingPrice(800000000L);
        car.setBidIncrement(10000000L);
        car.setSellerId(seller.getId());
        car.setCategory("VEHICLE");
        car.setMake("Toyota");
        car.setModel("Camry");
        car.setYear(2024);

        assertThat(car.getName()).isEqualTo("Toyota Camry");
        assertThat(car.getMake()).isEqualTo("Toyota");
        assertThat(car.getItemCategory()).isEqualTo("VEHICLE");

        productList.add(car);
    }

    @Test
    @Order(4)
    @DisplayName("✅ Tạo sản phẩm Other")
    void testCreateOther() {
        OtherItem other = new OtherItem();
        other.setId(4);
        other.setName("Đồng hồ Rolex");
        other.setDescription("Đồng hồ cao cấp");
        other.setStartingPrice(50000000L);
        other.setBidIncrement(2000000L);
        other.setSellerId(seller.getId());
        other.setCategory("OTHER");
        other.setCustomCategoryName("Watch");

        assertThat(other.getName()).isEqualTo("Đồng hồ Rolex");
        assertThat(other.getItemCategory()).isEqualTo("OTHER");
        assertThat(other.getCustomCategoryName()).isEqualTo("Watch");

        productList.add(other);
    }

    @Test
    @Order(5)
    @DisplayName("✅ Lấy tất cả sản phẩm")
    void testGetAllProducts() {
        productList.add(new Electronics());
        productList.add(new Art());
        productList.add(new Vehicle());

        assertThat(productList).hasSize(3);
    }

    @Test
    @Order(6)
    @DisplayName("✅ Tìm sản phẩm theo ID")
    void testGetProductById() {
        Electronics laptop = new Electronics();
        laptop.setId(100);
        laptop.setName("iPhone 15");
        productList.add(laptop);

        Item found = productList.stream()
                .filter(p -> p.getId() == 100)
                .findFirst()
                .orElse(null);

        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("iPhone 15");
    }

    @Test
    @Order(7)
    @DisplayName("✅ Cập nhật sản phẩm")
    void testUpdateProduct() {
        Electronics laptop = new Electronics();
        laptop.setId(1);
        laptop.setName("Old Name");
        laptop.setStartingPrice(10000000L);
        productList.add(laptop);

        laptop.setName("New Name");
        laptop.setStartingPrice(15000000L);

        assertThat(laptop.getName()).isEqualTo("New Name");
        assertThat(laptop.getStartingPrice()).isEqualTo(15000000L);
    }

    @Test
    @Order(8)
    @DisplayName("✅ Xóa sản phẩm")
    void testDeleteProduct() {
        Electronics laptop = new Electronics();
        laptop.setId(1);
        productList.add(laptop);

        assertThat(productList).hasSize(1);

        productList.removeIf(p -> p.getId() == 1);

        assertThat(productList).isEmpty();
    }

    @Test
    @Order(9)
    @DisplayName("✅ Factory Pattern - Tạo Electronics qua Factory")
    void testFactoryCreateElectronics() {
        ItemAttributes attrs = new ItemAttributes();
        attrs.setName("Factory Laptop");
        attrs.setDescription("Created by Factory");
        attrs.setStartingPrice(20000000L);
        attrs.setBrand("Dell");
        attrs.setWarrantyMonths(24);

        ElectronicsFactory factory = new ElectronicsFactory();
        Item item = factory.createItem(attrs);

        assertThat(item).isInstanceOf(Electronics.class);
        assertThat(item.getName()).isEqualTo("Factory Laptop");
        assertThat(((Electronics) item).getBrand()).isEqualTo("Dell");
    }

    @Test
    @Order(10)
    @DisplayName("✅ Factory Pattern - Tạo Art qua Factory")
    void testFactoryCreateArt() {
        ItemAttributes attrs = new ItemAttributes();
        attrs.setName("Factory Painting");
        attrs.setDescription("Created by Factory");
        attrs.setStartingPrice(50000000L);
        attrs.setArtist("Van Gogh");
        attrs.setYearCreated(1889);

        ArtFactory factory = new ArtFactory();
        Item item = factory.createItem(attrs);

        assertThat(item).isInstanceOf(Art.class);
        assertThat(((Art) item).getArtist()).isEqualTo("Van Gogh");
    }

    @Test
    @Order(11)
    @DisplayName("❌ Giá khởi điểm <= 0 - Không hợp lệ")
    void testInvalidStartingPrice() {
        Electronics invalid = new Electronics();
        invalid.setStartingPrice(0L);

        boolean isValid = invalid.getStartingPrice() > 0;
        assertThat(isValid).isFalse();
    }
}