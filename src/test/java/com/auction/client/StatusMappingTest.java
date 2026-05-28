package com.auction.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StatusMappingTest {

    // Hàm giả lập việc dịch mã lỗi từ server
    String getErrorMessage(int code) {
        switch (code) {
            case 401: return "Sai tài khoản hoặc mật khẩu!";
            case 409: return "Sản phẩm này đã kết thúc đấu giá!";
            case 500: return "Lỗi hệ thống, thử lại sau!";
            default: return "Lỗi không xác định!";
        }
    }

    @Test
    void testAuthErrorMessage() {
        assertEquals("Sai tài khoản hoặc mật khẩu!", getErrorMessage(401));
    }

    @Test
    void testAuctionEndedMessage() {
        assertEquals("Sản phẩm này đã kết thúc đấu giá!", getErrorMessage(409));
    }
}
