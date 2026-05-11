
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Chạy tất cả các bài test
 *
 * Cách chạy: mvn test
 */
@Suite
@SuiteDisplayName("Complete Backend Test Suite")
@SelectClasses({
        UserAuthTest.class,
        ProductManagementTest.class,
        com.auction.server.test.AuctionLogicTest.class
})
public class TestSummary {
    // Class này chỉ để gom các test lại với nhau
    // Không cần code gì bên trong
}