package com.auction.common.model.user;

import com.auction.common.exception.AuthenticationException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UserTest {

    // -----------------------------------------------------------------------
    // 1. Tạo đúng loại User và role
    // -----------------------------------------------------------------------

    @Test
    void testBidder_roleNameDung() {
        User user = new Bidder("bidder1", "pass123", "Nguyễn A");
        assertEquals("BIDDER", user.getRoleName());
    }

    @Test
    void testSeller_roleNameDung() {
        User user = new Seller("seller1", "pass123", "Trần B");
        assertEquals("SELLER", user.getRoleName());
    }

    @Test
    void testAdmin_roleNameDung() {
        User user = new Admin("admin", "admin123", "Admin");
        assertEquals("ADMIN", user.getRoleName());
    }

    @Test
    void testUser_getUsername_traVeDung() {
        User user = new Bidder("myuser", "mypass", "Họ Tên");
        assertEquals("myuser", user.getUsername());
        assertEquals("Họ Tên", user.getFullName());
        assertEquals("mypass", user.getPassword());
    }

    @Test
    void testUser_idTuDong_khacNhau() {
        User u1 = new Bidder("a", "p", "A");
        User u2 = new Bidder("b", "p", "B");
        assertNotEquals(u1.getId(), u2.getId(),
            "Mỗi user phải có UUID khác nhau");
    }

    @Test
    void testUser_idKhongNull() {
        User user = new Seller("s1", "p", "S");
        assertNotNull(user.getId());
        assertFalse(user.getId().isBlank());
    }

    // -----------------------------------------------------------------------
    // 2. Logic authenticate (copy từ AuctionManager để test độc lập, không cần DB)
    // -----------------------------------------------------------------------

    // Helper simulate logic AuctionManager.authenticate()
    private User authenticate(User storedUser, String inputPassword, String inputRole)
            throws AuthenticationException {
        if (storedUser == null)
            throw new AuthenticationException("Tài khoản không tồn tại!");
        if (!storedUser.getPassword().equals(inputPassword)
                || !storedUser.getRoleName().equalsIgnoreCase(inputRole))
            throw new AuthenticationException("Tên đăng nhập hoặc mật khẩu chưa chính xác!");
        return storedUser;
    }

    @Test
    void testAuthenticate_thanhCong() throws AuthenticationException {
        User stored = new Bidder("user1", "abc123", "Nguyễn A");
        User result = authenticate(stored, "abc123", "BIDDER");

        assertNotNull(result);
        assertEquals("user1", result.getUsername());
    }

    @Test
    void testAuthenticate_matKhauSai_throwException() {
        User stored = new Bidder("user1", "abc123", "Nguyễn A");

        assertThrows(AuthenticationException.class, () ->
            authenticate(stored, "wrongpass", "BIDDER")
        );
    }

    @Test
    void testAuthenticate_saiRole_throwException() {
        User stored = new Bidder("user1", "abc123", "Nguyễn A");
        // Đúng mật khẩu nhưng sai role
        assertThrows(AuthenticationException.class, () ->
            authenticate(stored, "abc123", "SELLER")
        );
    }

    @Test
    void testAuthenticate_userNull_throwException() {
        assertThrows(AuthenticationException.class, () ->
            authenticate(null, "anypass", "BIDDER")
        );
    }

    @Test
    void testAuthenticate_roleKhongPhanBietHoaThuong() throws AuthenticationException {
        User stored = new Bidder("user1", "abc123", "A");

        // "bidder", "Bidder", "BIDDER" đều hợp lệ
        assertDoesNotThrow(() -> authenticate(stored, "abc123", "bidder"));
        assertDoesNotThrow(() -> authenticate(stored, "abc123", "Bidder"));
        assertDoesNotThrow(() -> authenticate(stored, "abc123", "BIDDER"));
    }

    @Test
    void testAuthenticate_adminDangNhap_thanhCong() throws AuthenticationException {
        User stored = new Admin("admin", "admin123", "Admin");
        User result = authenticate(stored, "admin123", "ADMIN");

        assertNotNull(result);
        assertInstanceOf(Admin.class, result);
    }
}
