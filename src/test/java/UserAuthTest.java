// File: src/test/java/com/auction/server/test/UserAuthTest.java
import com.auction.common.dto.AuthDTOs;
import com.auction.common.dto.BaseDTOs;
import com.auction.common.enums.ErrorCode;
import com.auction.common.enums.StatusCode;
import com.auction.common.exception.AuthenticationException;
import com.auction.common.model.user.Bidder;
import com.auction.common.model.user.Seller;
import com.auction.common.model.user.Admin;
import com.auction.common.model.user.User;
import com.auction.server.dao.UserDao;
import com.auction.server.db.DatabaseConnection;
import com.auction.server.manager.UserManager;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test các chức năng: Đăng ký, Đăng nhập, Đăng xuất
 * Test UserManager và UserDao
 *
 * Cách chạy: mvn test -Dtest=UserAuthTest
 *            hoặc chạy từng method trong IDE
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserAuthTest {

    private static UserManager userManager;
    private static UserDao userDao;

    // Test data
    private static final String TEST_USERNAME = "test_bidder_01";
    private static final String TEST_PASSWORD = "Test@123456";
    private static final String TEST_FULLNAME = "Test Bidder One";

    @BeforeAll
    static void setupDatabase() {
        System.out.println("\n=== SETUP DATABASE FOR USER AUTH TESTS ===");
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {

            // Clean test data
            stmt.execute("DELETE FROM users WHERE username LIKE 'test_%'");
            System.out.println(">> Cleaned old test users");

        } catch (Exception e) {
            System.err.println("Setup error: " + e.getMessage());
        }

        userManager = UserManager.getInstance();
        userDao = new UserDao();
    }

    @AfterAll
    static void cleanupDatabase() {
        System.out.println("\n=== CLEANUP TEST DATA ===");
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("DELETE FROM users WHERE username LIKE 'test_%'");
            System.out.println(">> Cleaned all test users");

        } catch (Exception e) {
            System.err.println("Cleanup error: " + e.getMessage());
        }
    }

    // ==================== REGISTRATION TESTS ====================

    @Test
    @Order(1)
    @DisplayName("TC01: Đăng ký thành công với Bidder")
    void testRegisterBidder_Success() {
        System.out.println("\n--- TC01: Đăng ký Bidder ---");

        Bidder newUser = new Bidder(TEST_USERNAME, TEST_PASSWORD, TEST_FULLNAME);
        boolean result = userManager.dangKy(newUser);

        assertTrue(result, "Đăng ký phải thành công");

        // Verify trong database
        Optional<User> dbUser = userDao.timTheoTenDangNhap(TEST_USERNAME);
        assertTrue(dbUser.isPresent(), "User phải tồn tại trong DB");
        assertEquals(TEST_USERNAME, dbUser.get().getUsername());
        assertEquals("BIDDER", dbUser.get().getRoleName());
        assertEquals(0, dbUser.get().getBalance(), "Số dư mặc định là 0");

        System.out.println(">> PASS: Đăng ký Bidder thành công");
    }

    @Test
    @Order(2)
    @DisplayName("TC02: Đăng ký thất bại - Username đã tồn tại")
    void testRegister_DuplicateUsername() {
        System.out.println("\n--- TC02: Đăng ký trùng username ---");

        Bidder duplicateUser = new Bidder(TEST_USERNAME, "OtherPass", "Other Name");
        boolean result = userManager.dangKy(duplicateUser);

        assertFalse(result, "Đăng ký phải thất bại do trùng username");

        System.out.println(">> PASS: Từ chối đăng ký trùng username");
    }

    @Test
    @Order(3)
    @DisplayName("TC03: Đăng ký thành công với Seller")
    void testRegisterSeller_Success() {
        System.out.println("\n--- TC03: Đăng ký Seller ---");

        Seller seller = new Seller("test_seller_01", "Seller@123", "Test Seller");
        boolean result = userManager.dangKy(seller);

        assertTrue(result, "Đăng ký Seller phải thành công");

        Optional<User> dbUser = userDao.timTheoTenDangNhap("test_seller_01");
        assertTrue(dbUser.isPresent());
        assertEquals("SELLER", dbUser.get().getRoleName());

        System.out.println(">> PASS: Đăng ký Seller thành công");
    }

    @Test
    @Order(4)
    @DisplayName("TC04: Đăng ký thành công với Admin")
    void testRegisterAdmin_Success() {
        System.out.println("\n--- TC04: Đăng ký Admin ---");

        Admin admin = new Admin("test_admin_01", "Admin@123", "Test Admin");
        boolean result = userManager.dangKy(admin);

        assertTrue(result, "Đăng ký Admin phải thành công");

        System.out.println(">> PASS: Đăng ký Admin thành công");
    }

    // ==================== LOGIN TESTS ====================

    @Test
    @Order(5)
    @DisplayName("TC05: Đăng nhập thành công")
    void testLogin_Success() {
        System.out.println("\n--- TC05: Đăng nhập thành công ---");

        assertDoesNotThrow(() -> {
            User user = userManager.dangNhap(TEST_USERNAME, TEST_PASSWORD);

            assertNotNull(user, "User không được null");
            assertEquals(TEST_USERNAME, user.getUsername());
            assertEquals(TEST_FULLNAME, user.getFullName());
            assertEquals("BIDDER", user.getRoleName());

            // Kiểm tra user đã online
            User onlineUser = userManager.layNguoiDungOnline(user.getId());
            assertNotNull(onlineUser, "User phải trong danh sách online");
        });

        System.out.println(">> PASS: Đăng nhập thành công");
    }

    @Test
    @Order(6)
    @DisplayName("TC06: Đăng nhập thất bại - Username không tồn tại")
    void testLogin_WrongUsername() {
        System.out.println("\n--- TC06: Đăng nhập sai username ---");

        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> userManager.dangNhap("nonexistent_user", "any_password")
        );

        assertTrue(exception.getMessage().contains("không tồn tại"));
        System.out.println(">> PASS: Từ chối đăng nhập với username không tồn tại");
    }

    @Test
    @Order(7)
    @DisplayName("TC07: Đăng nhập thất bại - Sai mật khẩu")
    void testLogin_WrongPassword() {
        System.out.println("\n--- TC07: Đăng nhập sai password ---");

        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> userManager.dangNhap(TEST_USERNAME, "wrong_password")
        );

        assertTrue(exception.getMessage().contains("Sai mật khẩu"));
        System.out.println(">> PASS: Từ chối đăng nhập với sai mật khẩu");
    }

    @Test
    @Order(8)
    @DisplayName("TC08: Đăng nhập với tài khoản Seller")
    void testLogin_SellerRole() {
        System.out.println("\n--- TC08: Đăng nhập Seller ---");

        assertDoesNotThrow(() -> {
            User user = userManager.dangNhap("test_seller_01", "Seller@123");
            assertEquals("SELLER", user.getRoleName());
        });

        System.out.println(">> PASS: Đăng nhập Seller thành công");
    }

    // ==================== LOGOUT TESTS ====================

    @Test
    @Order(9)
    @DisplayName("TC09: Đăng xuất thành công")
    void testLogout_Success() {
        System.out.println("\n--- TC09: Đăng xuất ---");

        // Login first
        assertDoesNotThrow(() -> {
            User user = userManager.dangNhap(TEST_USERNAME, TEST_PASSWORD);

            // Logout
            userManager.dangXuat(user.getId());

            // Verify user is offline
            User offlineUser = userManager.layNguoiDungOnline(user.getId());
            assertNull(offlineUser, "User phải bị xóa khỏi danh sách online");
        });

        System.out.println(">> PASS: Đăng xuất thành công");
    }

    // ==================== USER DAO TESTS ====================

    @Test
    @Order(10)
    @DisplayName("TC10: Tìm user theo ID (DAOTest)")
    void testDao_FindByUsername() {
        System.out.println("\n--- TC10: DAO tìm user ---");

        Optional<User> userOpt = userDao.timTheoTenDangNhap(TEST_USERNAME);

        assertTrue(userOpt.isPresent());
        User user = userOpt.get();
        assertTrue(user.getId() > 0, "ID phải > 0");
        assertEquals(TEST_USERNAME, user.getUsername());
        assertEquals(TEST_FULLNAME, user.getFullName());

        System.out.println(">> PASS: Tìm user trong DB thành công, ID=" + user.getId());
    }

    @Test
    @Order(11)
    @DisplayName("TC11: Cập nhật số dư (DAOTest)")
    void testDao_UpdateBalance() {
        System.out.println("\n--- TC11: Cập nhật số dư ---");

        Optional<User> userOpt = userDao.timTheoTenDangNhap(TEST_USERNAME);
        assertTrue(userOpt.isPresent());

        int userId = userOpt.get().getId();
        long newBalance = 5000000L;

        boolean updated = userDao.capNhatSoDu(userId, newBalance);
        assertTrue(updated, "Cập nhật số dư phải thành công");

        // Verify
        Optional<User> updatedUser = userDao.timTheoTenDangNhap(TEST_USERNAME);
        assertEquals(newBalance, updatedUser.get().getBalance());

        System.out.println(">> PASS: Cập nhật số dư thành công: " + newBalance);
    }

    // ==================== BOUNDARY/EDGE CASES ====================

    @Test
    @Order(12)
    @DisplayName("TC12: Đăng ký với username đặc biệt")
    void testRegister_SpecialCharacters() {
        System.out.println("\n--- TC12: Username đặc biệt ---");

        Bidder specialUser = new Bidder("test_user_@123", "Pass@123", "Special Char User");
        boolean result = userManager.dangKy(specialUser);

        assertTrue(result, "Cho phép ký tự đặc biệt trong username");

        System.out.println(">> PASS: Chấp nhận username có ký tự đặc biệt");
    }

    @Test
    @Order(13)
    @DisplayName("TC13: Đăng ký với tên dài")
    void testRegister_LongFullName() {
        System.out.println("\n--- TC13: FullName dài ---");

        String longName = "Nguyễn Văn A Với Một Cái Tên Rất Dài Trong Hệ Thống Đấu Giá Trực Tuyến";
        Bidder user = new Bidder("test_longname", "Pass@123", longName);
        boolean result = userManager.dangKy(user);

        assertTrue(result, "Cho phép fullname dài");

        Optional<User> savedUser = userDao.timTheoTenDangNhap("test_longname");
        assertEquals(longName, savedUser.get().getFullName());

        System.out.println(">> PASS: Lưu thành công fullname dài");
    }

    // ==================== PERFORMANCE TEST ====================

    @Test
    @Order(14)
    @DisplayName("TC14: Performance - Đăng ký 50 users")
    void testPerformance_MultipleRegistrations() {
        System.out.println("\n--- TC14: Performance Test (50 users) ---");

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 50; i++) {
            Bidder user = new Bidder(
                    "test_perf_" + i,
                    "Pass@123",
                    "Performance User " + i
            );
            boolean result = userManager.dangKy(user);
            assertTrue(result, "Đăng ký user thứ " + i + " phải thành công");
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println(">> 50 users registered in " + duration + "ms");
        assertTrue(duration < 10000, "Đăng ký 50 users phải dưới 10 giây");
    }
}