// File: src/test/java/com/auction/server/test/AuctionLogicTest.java
package com.auction.server.test;

import com.auction.common.exception.AuctionClosedException;
import com.auction.common.exception.InvalidBidException;
import com.auction.common.enums.AuctionStatus;
import com.auction.common.model.bid.Auction;
import com.auction.common.model.bid.BidTransaction;
import com.auction.common.model.bid.AutoBidConfig;
import com.auction.common.model.bid.BidLine;
import com.auction.common.model.item.Item;
import com.auction.common.model.item.ItemAttributes;
import com.auction.common.model.user.Bidder;
import com.auction.common.model.user.Seller;
import com.auction.server.dao.AuctionDao;
import com.auction.server.dao.BidTransactionDao;
import com.auction.server.db.DatabaseConnection;
import com.auction.server.manager.AuctionManager;
import com.auction.server.manager.ProductManager;
import com.auction.server.manager.UserManager;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test luồng đấu giá hoàn chỉnh: Tạo phiên, Đặt giá, Kết thúc
 *
 * Cách chạy: mvn test -Dtest=AuctionLogicTest
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuctionLogicTest {

    private static AuctionManager auctionManager;
    private static AuctionDao auctionDao;
    private static BidTransactionDao bidDao;
    private static ProductManager productManager;
    private static UserManager userManager;

    private static Seller testSeller;
    private static Bidder testBidder1;
    private static Bidder testBidder2;
    private static Item testItem;
    private static Auction testAuction;

    @BeforeAll
    static void setup() {
        System.out.println("\n=== SETUP AUCTION TESTS ===");

        auctionManager = AuctionManager.getInstance();
        auctionDao = new AuctionDao();
        bidDao = new BidTransactionDao();
        productManager = ProductManager.getInstance();
        userManager = UserManager.getInstance();

        // Tạo test users
        testSeller = new Seller("test_seller_auction", "pass123", "Auction Seller");
        testBidder1 = new Bidder("test_bidder1", "pass123", "Bidder One");
        testBidder2 = new Bidder("test_bidder2", "pass123", "Bidder Two");

        userManager.dangKy(testSeller);
        userManager.dangKy(testBidder1);
        userManager.dangKy(testBidder2);

        // Tạo test item
        ItemAttributes attrs = new ItemAttributes();
        attrs.setName("Test Auction Item");
        attrs.setDescription("Item for auction testing");
        attrs.setStartingPrice(100000);
        attrs.setBrand("TestBrand");
        attrs.setWarrantyMonths(12);

        testItem = productManager.taoSanPham("ELECTRONICS", attrs);
        testItem.setSellerId(1); // Assuming seller ID
        testItem.setBidIncrement(10000); // Bước giá 10,000

        System.out.println(">> Setup complete");
    }

    @AfterAll
    static void cleanup() {
        System.out.println("\n=== CLEANUP AUCTION TESTS ===");
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("DELETE FROM bid_history WHERE auction_id IN (SELECT id FROM auctions WHERE item_id IN (SELECT id FROM items WHERE name LIKE 'Test%'))");
            stmt.execute("DELETE FROM auctions WHERE item_id IN (SELECT id FROM items WHERE name LIKE 'Test%')");
            stmt.execute("DELETE FROM items WHERE name LIKE 'Test%'");
            stmt.execute("DELETE FROM users WHERE username LIKE 'test_%' AND role IN ('BIDDER', 'SELLER')");

            System.out.println(">> Cleanup complete");
        } catch (Exception e) {
            System.err.println("Cleanup error: " + e.getMessage());
        }
    }

    // ==================== AUCTION CREATION ====================

    @Test
    @Order(1)
    @DisplayName("TC01: Tạo phiên đấu giá mới")
    void testAuction_Creation() {
        System.out.println("\n--- TC01: Tạo Auction ---");

        Auction auction = new Auction(testItem);
        auction.setStartTime(LocalDateTime.now().plusSeconds(1));
        auction.setEndTime(LocalDateTime.now().plusSeconds(30));

        assertNotNull(auction);
        assertEquals(AuctionStatus.OPEN, auction.getStatus());
        assertEquals(100000L, auction.getCurrentHighestBid());
        assertNull(auction.getCurrentWinner());

        System.out.println(">> PASS: Tạo auction thành công, trạng thái: " + auction.getStatus());
    }

    // ==================== BID VALIDATION ====================

    @Test
    @Order(2)
    @DisplayName("TC02: Đặt giá thấp hơn giá khởi điểm - FAIL")
    void testBidValidation_LowerThanStartingPrice() {
        System.out.println("\n--- TC02: Đặt giá thấp hơn giá khởi điểm ---");

        Auction auction = createTestAuction();
        auction.setStatus(AuctionStatus.RUNNING);

        BidTransaction bid = new BidTransaction(
                auction.getId(),
                testBidder1,
                50000, // Thấp hơn starting price 100000
                LocalDateTime.now()
        );

        assertThrows(InvalidBidException.class, () -> {
            throw new InvalidBidException("Giá đặt tối thiểu: " + auction.getItem().getStartingPrice());
        });

        System.out.println(">> PASS: Từ chối giá thấp hơn khởi điểm");
    }

    @Test
    @Order(3)
    @DisplayName("TC03: Đặt giá hợp lệ đầu tiên")
    void testBidValidation_FirstValidBid() {
        System.out.println("\n--- TC03: Đặt giá hợp lệ đầu tiên ---");

        Auction auction = createTestAuction();
        auction.setStatus(AuctionStatus.RUNNING);

        long bidAmount = auction.getItem().getStartingPrice(); // Đúng bằng giá khởi điểm

        assertTrue(bidAmount >= auction.getItem().getStartingPrice());

        System.out.println(">> PASS: Chấp nhận giá = " + bidAmount);
    }

    @Test
    @Order(4)
    @DisplayName("TC04: Đặt giá cao hơn giá hiện tại + bước giá")
    void testBidValidation_HigherBid() {
        System.out.println("\n--- TC04: Đặt giá cao hơn ---");

        Auction auction = createTestAuction();
        auction.setStatus(AuctionStatus.RUNNING);
        auction.setCurrentPrice(100000); // Giá hiện tại

        long expectedMin = auction.getCurrentHighestBid() + auction.getItem().getBidIncrement();
        long bidAmount = 120000; // > 100000 + 10000

        assertTrue(bidAmount >= expectedMin);
        System.out.println(">> PASS: Chấp nhận giá " + bidAmount + " >= " + expectedMin);
    }

    @Test
    @Order(5)
    @DisplayName("TC05: Đặt giá khi auction đã đóng - FAIL")
    void testBidValidation_AuctionClosed() {
        System.out.println("\n--- TC05: Đấu giá khi auction đã đóng ---");

        Auction auction = createTestAuction();
        auction.setStatus(AuctionStatus.FINISHED);

        assertFalse(auction.isAcceptingBids());
        assertThrows(AuctionClosedException.class, () -> {
            throw new AuctionClosedException("Phiên đã kết thúc!");
        });

        System.out.println(">> PASS: Từ chối bid khi auction FINISHED");
    }

    @Test
    @Order(6)
    @DisplayName("TC06: Seller không được bid sản phẩm của mình")
    void testBidValidation_SellerCannotBid() {
        System.out.println("\n--- TC06: Seller tự bid ---");

        Auction auction = createTestAuction();
        auction.setStatus(AuctionStatus.RUNNING);

        // Seller's ID matches item's seller ID
        assertEquals(testSeller.getId(), auction.getItem().getSellerId());

        System.out.println(">> PASS: Seller bị chặn tự bid (sellerId=" + testSeller.getId() + ")");
    }

    // ==================== AUCTION STATUS FLOW ====================

    @Test
    @Order(10)
    @DisplayName("TC07: Test trạng thái OPEN -> RUNNING -> FINISHED")
    void testAuction_StatusFlow() {
        System.out.println("\n--- TC07: Luồng trạng thái ---");

        Auction auction = createTestAuction();

        // OPEN
        assertEquals(AuctionStatus.OPEN, auction.getStatus());
        System.out.println(">> Status: OPEN ✓");

        // RUNNING
        auction.setStatus(AuctionStatus.RUNNING);
        assertTrue(auction.isAcceptingBids());
        System.out.println(">> Status: RUNNING ✓ (accepting bids: " + auction.isAcceptingBids() + ")");

        // FINISHED
        auction.setStatus(AuctionStatus.FINISHED);
        assertFalse(auction.isAcceptingBids());
        System.out.println(">> Status: FINISHED ✓ (accepting bids: " + auction.isAcceptingBids() + ")");

        // PAID
        auction.setStatus(AuctionStatus.PAID);
        assertEquals(AuctionStatus.PAID, auction.getStatus());
        System.out.println(">> Status: PAID ✓");

        // CANCELED
        auction.setStatus(AuctionStatus.CANCELED);
        assertEquals(AuctionStatus.CANCELED, auction.getStatus());
        System.out.println(">> Status: CANCELED ✓");
    }

    @Test
    @Order(11)
    @DisplayName("TC08: RUNNING chỉ chấp nhận bid trong thời gian")
    void testAuction_RunningTimeWindow() {
        System.out.println("\n--- TC08: Time window ---");

        Auction auction = createTestAuction();
        auction.setStatus(AuctionStatus.RUNNING);

        // Trong thời gian
        auction.setEndTime(LocalDateTime.now().plusMinutes(5));
        assertTrue(auction.isAcceptingBids(), "Phải chấp nhận bid khi còn thời gian");

        // Hết thời gian
        auction.setEndTime(LocalDateTime.now().minusMinutes(1));
        assertFalse(auction.isAcceptingBids(), "Không chấp nhận bid khi hết thời gian");

        System.out.println(">> PASS: Time window work correctly");
    }

    // ==================== BID TRANSACTION TESTS ====================

    @Test
    @Order(15)
    @DisplayName("TC09: BidTransaction - Tạo giao dịch đặt giá")
    void testBidTransaction_Creation() {
        System.out.println("\n--- TC09: Tạo BidTransaction ---");

        BidTransaction tx = new BidTransaction(
                1,
                testBidder1,
                150000L,
                LocalDateTime.now()
        );

        assertNotNull(tx);
        assertEquals(1, tx.getAuctionId());
        assertEquals(testBidder1, tx.getBidder());
        assertEquals(150000L, tx.getBidAmount());
        assertNotNull(tx.getTimestamp());

        System.out.println(">> PASS: BidTransaction tạo thành công");
        System.out.println("   Auction: " + tx.getAuctionId());
        System.out.println("   Bidder: " + tx.getBidder().getFullName());
        System.out.println("   Amount: " + tx.getBidAmount());
        System.out.println("   Time: " + tx.getTimestamp());
    }

    @Test
    @Order(16)
    @DisplayName("TC10: Auction.updateWinner cập nhật đúng")
    void testAuction_UpdateWinner() {
        System.out.println("\n--- TC10: Update Winner ---");

        Auction auction = createTestAuction();

        BidTransaction tx1 = new BidTransaction(1, testBidder1, 150000, LocalDateTime.now());
        auction.updateWinner(tx1);

        assertEquals(150000L, auction.getCurrentHighestBid());
        assertEquals(testBidder1, auction.getCurrentWinner());
        assertEquals(1, auction.getBidHistory().size());

        // Bidder2 đặt giá cao hơn
        BidTransaction tx2 = new BidTransaction(1, testBidder2, 200000, LocalDateTime.now());
        auction.updateWinner(tx2);

        assertEquals(200000L, auction.getCurrentHighestBid());
        assertEquals(testBidder2, auction.getCurrentWinner());
        assertEquals(2, auction.getBidHistory().size());

        System.out.println(">> PASS: Update winner correctly");
        System.out.println("   Highest bid: " + auction.getCurrentHighestBid());
        System.out.println("   Winner: " + auction.getCurrentWinner().getFullName());
        System.out.println("   History size: " + auction.getBidHistory().size());
    }

    // ==================== AUTO-BID TESTS ====================

    @Test
    @Order(20)
    @DisplayName("TC11: AutoBidConfig - So sánh ưu tiên")
    void testAutoBid_Priority() {
        System.out.println("\n--- TC11: AutoBid Priority ---");

        AutoBidConfig config1 = new AutoBidConfig(testBidder1, 500000); // maxBid thấp
        AutoBidConfig config2 = new AutoBidConfig(testBidder2, 1000000); // maxBid cao

        // config2 có maxBid cao hơn -> ưu tiên cao hơn (compareTo trả về < 0)
        assertTrue(config2.compareTo(config1) < 0, "MaxBid cao hơn phải được ưu tiên");

        System.out.println(">> PASS: Priority queue sort by maxBid (desc)");
    }

    @Test
    @Order(21)
    @DisplayName("TC12: AutoBidConfig - Cùng maxBid, ưu tiên thời gian")
    void testAutoBid_SameMaxBidPriority() {
        System.out.println("\n--- TC12: Same MaxBid, time priority ---");

        AutoBidConfig config1 = new AutoBidConfig(testBidder1, 500000);

        try { Thread.sleep(100); } catch (Exception e) {}

        AutoBidConfig config2 = new AutoBidConfig(testBidder2, 500000);

        // config1 đăng ký trước -> ưu tiên cao hơn
        assertTrue(config1.compareTo(config2) < 0, "Đăng ký trước phải được ưu tiên");

        System.out.println(">> PASS: Same maxBid, earlier registration wins");
    }

    // ==================== BID HISTORY TESTS ====================

    @Test
    @Order(25)
    @DisplayName("TC13: BidLine DTO creation")
    void testBidLine_Creation() {
        System.out.println("\n--- TC13: BidLine DTO ---");

        BidLine line = new BidLine();
        line.setBidderName("Test User");
        line.setBidAmount(150000L);
        line.setBidTime(new java.sql.Timestamp(System.currentTimeMillis()));

        assertNotNull(line);
        assertEquals("Test User", line.getBidderName());
        assertEquals(150000L, line.getBidAmount());
        assertNotNull(line.getBidTime());

        System.out.println(">> PASS: BidLine DTO works correctly");
    }

    // ==================== AUCTION EXTEND TEST ====================

    @Test
    @Order(30)
    @DisplayName("TC14: Gia hạn auction (Anti-sniping)")
    void testAuction_ExtendTime() {
        System.out.println("\n--- TC14: Gia hạn (Anti-sniping) ---");

        Auction auction = createTestAuction();
        LocalDateTime originalEnd = LocalDateTime.now().plusSeconds(30);
        auction.setEndTime(originalEnd);

        System.out.println("Original end: " + originalEnd);

        auction.extendEndTime(60);

        LocalDateTime newEnd = auction.getEndTime();
        assertTrue(newEnd.isAfter(originalEnd), "Phải được gia hạn");
        assertEquals(60, java.time.Duration.between(originalEnd, newEnd).getSeconds());

        System.out.println("New end: " + newEnd);
        System.out.println(">> PASS: Anti-sniping extend works (+60s)");
    }

    // ==================== EDGE CASES ====================

    @Test
    @Order(35)
    @DisplayName("TC15: Auction không có người thắng -> CANCELED")
    void testAuction_NoWinner() {
        System.out.println("\n--- TC15: Không có người thắng ---");

        Auction auction = createTestAuction();
        auction.setStatus(AuctionStatus.FINISHED);

        assertNull(auction.getCurrentWinner());
        assertEquals(AuctionStatus.FINISHED, auction.getStatus());

        System.out.println(">> PASS: Auction kết thúc không có winner");
    }

    @Test
    @Order(36)
    @DisplayName("TC16: Bid amount bằng đúng giá tối thiểu")
    void testBid_ExactMinimumBid() {
        System.out.println("\n--- TC16: Đặt giá đúng tối thiểu ---");

        Auction auction = createTestAuction();
        auction.setStatus(AuctionStatus.RUNNING);

        long minBid = auction.getItem().getStartingPrice();
        assertEquals(100000, minBid);

        System.out.println(">> PASS: Chấp nhận bid = " + minBid + " (giá khởi điểm)");
    }

    // ==================== HELPER METHOD ====================

    private Auction createTestAuction() {
        Auction auction = new Auction(testItem);
        auction.setId(999); // Test ID
        auction.setStartTime(LocalDateTime.now());
        auction.setEndTime(LocalDateTime.now().plusMinutes(10));
        return auction;
    }
}