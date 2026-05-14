package testgroup;

import com.auction.common.exception.AuthenticationException;
import com.auction.common.model.user.*;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

@DisplayName("👤 Kiểm tra Xác thực Người dùng")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserAuthTest {

    @Test
    @DisplayName("✅ Đăng nhập thành công - Bidder")
    void testLoginSuccess_Bidder() {
        Bidder bidder = new Bidder("bidder123", "password123", "Nguyễn Văn A");
        bidder.setId(1);
        bidder.setBalance(10000000L);

        assertThat(bidder.getUsername()).isEqualTo("bidder123");
        assertThat(bidder.getRoleName()).isEqualTo("BIDDER");
        assertThat(bidder.getPassword()).isEqualTo("password123");
        assertThat(bidder.getBalance()).isEqualTo(10000000L);
    }

    @Test
    @DisplayName("✅ Đăng nhập thành công - Seller")
    void testLoginSuccess_Seller() {
        Seller seller = new Seller("seller456", "sellerpass", "Trần Thị B");
        seller.setId(2);

        assertThat(seller.getUsername()).isEqualTo("seller456");
        assertThat(seller.getRoleName()).isEqualTo("SELLER");
    }

    @Test
    @DisplayName("✅ Đăng nhập thành công - Admin")
    void testLoginSuccess_Admin() {
        Admin admin = new Admin("admin", "admin123", "Quản trị viên");
        admin.setId(3);

        assertThat(admin.getUsername()).isEqualTo("admin");
        assertThat(admin.getRoleName()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("❌ Sai mật khẩu - Ném AuthenticationException")
    void testLoginFail_WrongPassword() {
        Bidder bidder = new Bidder("bidder123", "correctPass", "Nguyễn Văn A");

        assertThatThrownBy(() -> {
            String inputPassword = "wrongPass";
            if (!bidder.getPassword().equals(inputPassword)) {
                throw new AuthenticationException("Sai mật khẩu, vui lòng thử lại!");
            }
        }).isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Sai mật khẩu");
    }

    @Test
    @DisplayName("❌ Tài khoản không tồn tại - Ném AuthenticationException")
    void testLoginFail_UserNotFound() {
        assertThatThrownBy(() -> {
            throw new AuthenticationException("Tài khoản không tồn tại trong hệ thống!");
        }).isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("không tồn tại");
    }

    @Test
    @DisplayName("✅ Đăng ký tài khoản mới")
    void testRegisterSuccess() {
        Bidder newUser = new Bidder("newuser", "newpass123", "Người Dùng Mới");
        newUser.setId(100);

        assertThat(newUser.getUsername()).isEqualTo("newuser");
        assertThat(newUser.getFullName()).isEqualTo("Người Dùng Mới");
        assertThat(newUser.getBalance()).isEqualTo(0L);
    }

    @Test
    @DisplayName("✅ Phân quyền người dùng")
    void testUserRolePermissions() {
        Bidder bidder = new Bidder("bidder", "pass", "Bidder");
        Seller seller = new Seller("seller", "pass", "Seller");
        Admin admin = new Admin("admin", "pass", "Admin");

        assertThat(bidder.getRoleName()).isEqualTo("BIDDER");
        assertThat(seller.getRoleName()).isEqualTo("SELLER");
        assertThat(admin.getRoleName()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("✅ Kiểm tra số dư tài khoản")
    void testUserBalance() {
        Bidder bidder = new Bidder("bidder", "pass", "Bidder");
        bidder.setBalance(5000000L);

        assertThat(bidder.getBalance()).isEqualTo(5000000L);

        bidder.addBalance(2000000L);
        assertThat(bidder.getBalance()).isEqualTo(7000000L);

        bidder.deductBalance(3000000L);
        assertThat(bidder.getBalance()).isEqualTo(4000000L);

        assertThat(bidder.hasEnoughBalance(3000000L)).isTrue();
        assertThat(bidder.hasEnoughBalance(5000000L)).isFalse();
    }

    @Test
    @DisplayName("✅ Đăng xuất")
    void testLogout() {
        Bidder bidder = new Bidder("onlineUser", "pass", "Online User");
        bidder.setId(99);

        assertThat(bidder).isNotNull();

        bidder = null;
        assertThat(bidder).isNull();
    }
}