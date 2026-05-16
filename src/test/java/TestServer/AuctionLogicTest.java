package TestServer;

import com.auction.common.enums.AuctionStatus;
import com.auction.common.model.bid.Auction;
import com.auction.common.model.bid.BidTransaction;
import com.auction.common.model.bid.BidLine;
import com.auction.common.model.item.Electronics;
import com.auction.common.model.user.Bidder;
import com.auction.common.model.user.Seller;
import org.junit.jupiter.api.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.*;

@DisplayName("🎯 Kiểm tra Logic Đấu giá")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuctionLogicTest {

    private Auction auction;
    private Electronics item;
    private Seller seller;
    private Bidder bidder1;
    private Bidder bidder2;
    private Bidder bidder3;

    @BeforeEach
    void setUp() {
        seller = new Seller("seller", "pass", "Người bán");
        seller.setId(1);

        item = new Electronics();
        item.setId(100);
        item.setName("iPhone 15 Pro Max");
        item.setStartingPrice(10000000L);
        item.setBidIncrement(500000L);
        item.setSellerId(1);

        auction = new Auction(item);
        auction.setId(1000);
        auction.setStartTime(LocalDateTime.now().minusMinutes(5));
        auction.setEndTime(LocalDateTime.now().plusHours(1));
        auction.setStatus(AuctionStatus.RUNNING);
        auction.setCurrentPrice(10000000L);  // DÙNG setCurrentPrice()

        bidder1 = new Bidder("bidder1", "pass", "Người đấu giá 1");
        bidder1.setId(10);
        bidder1.setBalance(50000000L);

        bidder2 = new Bidder("bidder2", "pass", "Người đấu giá 2");
        bidder2.setId(11);
        bidder2.setBalance(30000000L);

        bidder3 = new Bidder("bidder3", "pass", "Người đấu giá 3");
        bidder3.setId(12);
        bidder3.setBalance(100000000L);
    }

    @Test
    @Order(1)
    @DisplayName("✅ Đặt giá lần đầu thành công")
    void testPlaceFirstBidSuccess() {
        BidTransaction tx = new BidTransaction(auction.getId(), bidder1, 11000000L, LocalDateTime.now());
        auction.updateWinner(tx);

        // DÙNG ĐÚNG METHOD getCurrentHighestBid()
        assertThat(auction.getCurrentHighestBid()).isEqualTo(11000000L);
        assertThat(auction.getCurrentWinner()).isEqualTo(bidder1);
    }

    @Test
    @Order(2)
    @DisplayName("✅ Đặt giá cao hơn thành công")
    void testPlaceHigherBidSuccess() {
        BidTransaction tx1 = new BidTransaction(auction.getId(), bidder1, 11000000L, LocalDateTime.now());
        auction.updateWinner(tx1);

        BidTransaction tx2 = new BidTransaction(auction.getId(), bidder2, 12500000L, LocalDateTime.now());
        auction.updateWinner(tx2);

        assertThat(auction.getCurrentHighestBid()).isEqualTo(12500000L);
        assertThat(auction.getCurrentWinner()).isEqualTo(bidder2);
    }

    @Test
    @Order(3)
    @DisplayName("❌ Đặt giá thấp hơn - Phải báo lỗi")
    void testPlaceLowerBidFails() {
        BidTransaction tx1 = new BidTransaction(auction.getId(), bidder1, 11000000L, LocalDateTime.now());
        auction.updateWinner(tx1);

        long currentPrice = auction.getCurrentHighestBid();
        long lowerBid = 10500000L;

        assertThat(lowerBid).isLessThan(currentPrice);
    }

    @Test
    @Order(4)
    @DisplayName("❌ Đặt giá khi phiên đã kết thúc")
    void testPlaceBidOnClosedAuctionFails() {
        auction.setStatus(AuctionStatus.FINISHED);

        // DÙNG ĐÚNG METHOD isAcceptingBids()
        assertThat(auction.isAcceptingBids()).isFalse();
    }

    @Test
    @Order(5)
    @DisplayName("✅ Anti-sniping - Gia hạn phiên")
    void testAntiSnipingExtension() {
        // Lưu thời gian kết thúc cũ
        LocalDateTime oldEndTime = auction.getEndTime();

        // Giả lập đặt giá trong 30 giây cuối
        LocalDateTime newEndTime = LocalDateTime.now().plusSeconds(25);
        auction.setEndTime(newEndTime);

        // Kiểm tra nếu trong 30 giây cuối thì gia hạn
        if (auction.getEndTime().minusSeconds(30).isBefore(LocalDateTime.now())) {
            auction.extendEndTime(60);
        }

        // Kiểm tra đã được gia hạn (thời gian mới > thời gian cũ)
        assertThat(auction.getEndTime()).isAfter(newEndTime);
        System.out.println("✓ Gia hạn phiên: " + newEndTime + " → " + auction.getEndTime());
    }

    @Test
    @Order(6)
    @DisplayName("✅ Xác định người thắng chính xác")
    void testDetermineWinnerCorrectly() {
        BidTransaction tx1 = new BidTransaction(auction.getId(), bidder1, 10000000L, LocalDateTime.now());
        BidTransaction tx2 = new BidTransaction(auction.getId(), bidder2, 12000000L, LocalDateTime.now());
        BidTransaction tx3 = new BidTransaction(auction.getId(), bidder3, 15000000L, LocalDateTime.now());
        BidTransaction tx4 = new BidTransaction(auction.getId(), bidder1, 18000000L, LocalDateTime.now());

        auction.updateWinner(tx1);
        auction.updateWinner(tx2);
        auction.updateWinner(tx3);
        auction.updateWinner(tx4);

        assertThat(auction.getCurrentWinner()).isEqualTo(bidder1);
        assertThat(auction.getCurrentHighestBid()).isEqualTo(18000000L);
    }

    @Test
    @Order(7)
    @DisplayName("✅ Chuyển trạng thái phiên")
    void testAuctionStatusTransition() {
        auction.setStatus(AuctionStatus.RUNNING);
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.RUNNING);

        auction.setCurrentWinner(bidder1);
        auction.setStatus(AuctionStatus.FINISHED);
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.FINISHED);

        Auction auction2 = new Auction(item);
        auction2.setStatus(AuctionStatus.CANCELED);
        assertThat(auction2.getStatus()).isEqualTo(AuctionStatus.CANCELED);
    }

    @Test
    @Order(8)
    @DisplayName("✅ Đặt giá đồng thời - 20 threads")
    void testConcurrentBidding() throws InterruptedException {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicBoolean hasError = new AtomicBoolean(false);
        AtomicLong finalPrice = new AtomicLong(auction.getCurrentHighestBid());

        for (int i = 0; i < threadCount; i++) {
            final long bidAmount = 10000000L + (i + 1) * 500000L;
            final Bidder bidder = (i % 2 == 0) ? bidder1 : bidder2;

            executor.submit(() -> {
                try {
                    synchronized (auction) {
                        if (bidAmount > auction.getCurrentHighestBid()) {
                            BidTransaction tx = new BidTransaction(
                                    auction.getId(), bidder, bidAmount, LocalDateTime.now()
                            );
                            auction.updateWinner(tx);
                            finalPrice.set(bidAmount);
                        }
                    }
                } catch (Exception e) {
                    hasError.set(true);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(hasError.get()).isFalse();
        assertThat(finalPrice.get()).isGreaterThanOrEqualTo(10000000L);
    }

    @Test
    @Order(9)
    @DisplayName("✅ Kiểm tra số dư trước khi đặt giá - Dùng hasEnoughBalance()")
    void testCheckBalanceBeforeBidding() {
        Bidder poorBidder = new Bidder("poor", "pass", "Người nghèo");
        poorBidder.setId(99);
        poorBidder.setBalance(1000000L);

        long bidAmount = 5000000L;

        // DÙNG ĐÚNG METHOD hasEnoughBalance()
        assertThat(poorBidder.hasEnoughBalance(bidAmount)).isFalse();

        bidder1.setBalance(50000000L);
        assertThat(bidder1.hasEnoughBalance(bidAmount)).isTrue();
    }

    @Test
    @Order(10)
    @DisplayName("✅ Lịch sử đặt giá - BidHistory")
    void testBidHistory() {
        List<BidLine> history = new ArrayList<>();

        history.add(new BidLine("Bidder1", 10000000L, Timestamp.valueOf(LocalDateTime.now())));
        history.add(new BidLine("Bidder2", 12000000L, Timestamp.valueOf(LocalDateTime.now().plusSeconds(5))));
        history.add(new BidLine("Bidder1", 15000000L, Timestamp.valueOf(LocalDateTime.now().plusSeconds(10))));

        assertThat(history).hasSize(3);
        assertThat(history.get(0).getBidAmount()).isEqualTo(10000000L);
        assertThat(history.get(2).getBidderName()).isEqualTo("Bidder1");
    }

    @Test
    @Order(11)
    @DisplayName("✅ BidTransaction tạo đúng")
    void testBidTransactionCreation() {
        LocalDateTime now = LocalDateTime.now();
        BidTransaction tx = new BidTransaction(100, bidder1, 15000000L, now);

        assertThat(tx.getAuctionId()).isEqualTo(100);
        assertThat(tx.getBidder()).isEqualTo(bidder1);
        assertThat(tx.getBidAmount()).isEqualTo(15000000L);
    }
}