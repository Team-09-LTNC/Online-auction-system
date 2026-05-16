package TestServer;

import org.junit.jupiter.api.*;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
        UserAuthTest.class,
        AuctionLogicTest.class,
        ProductManagementTest.class
})
@DisplayName("🏃 Test Suite - Toàn bộ kiểm tra")  // ← Thêm @DisplayName
public class TestSuiteRunner {

    @BeforeAll
    static void setup() {
        System.out.println("\n========================================");
        System.out.println("🚀 BẮT ĐẦU KIỂM TRA HỆ THỐNG ĐẤU GIÁ");
        System.out.println("========================================\n");
    }

    @AfterAll
    static void teardown() {
        System.out.println("\n========================================");
        System.out.println("✅ HOÀN THÀNH KIỂM TRA");
        System.out.println("========================================\n");
    }
}