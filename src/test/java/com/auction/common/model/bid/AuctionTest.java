package com.auction.common.model.bid;

import com.auction.auction.AuctionStatus;
import com.auction.common.exception.InvalidBidException;
import com.auction.common.model.item.Art;
import com.auction.common.model.item.Item;
import com.auction.common.model.user.Bidder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionTest {

    private AuctionTestable auction;
    private Bidder bidder1, bidder2, bidder3;

    // Subclass bypass DB — override persistBid để không cần kết nối MySQL khi test
    static class AuctionTestable extends Auction {
        public AuctionTestable(Item item) {
            super(item);
        }

        @Override
        protected boolean persistBid(int auctionId, BidTransaction tx) {
            return true; // giả lập DB luôn thành công
        }
    }

    @BeforeEach
    void setUp() {
        Item item = new Art("Bức tranh A", "Mô tả", 1000.0, "Da Vinci", 1503, "Sơn dầu");
        auction = new AuctionTestable(item);
        bidder1 = new Bidder("user1", "pass", "Nguyễn A");
        bidder2 = new Bidder("user2", "pass", "Trần B");
        bidder3 = new Bidder("user3", "pass", "Lê C");
    }

    // -----------------------------------------------------------------------
    // 1. Trạng thái ban đầu
    // -----------------------------------------------------------------------

    @Test
    void testKhoiTao_statusLaOPEN() {
        assertEquals(AuctionStatus.OPEN, auction.getStatus());
    }

    @Test
    void testKhoiTao_giaCaoNhatBangGiaKhoiDiem() {
        assertEquals(1000.0, auction.getCurrentHighestBid());
    }

    @Test
    void testKhoiTao_chuaCoNguoiThang() {
        assertNull(auction.getCurrentWinner());
    }

    // -----------------------------------------------------------------------
    // 2. Đặt giá hợp lệ
    // -----------------------------------------------------------------------

    @Test
    void testDatGia_hopLe_capNhatGiaVaNguoiDan() throws InvalidBidException {
        auction.startAuction(60);

        boolean result = auction.addValidBid(new BidTransaction(bidder1, 1500.0));

        assertTrue(result);
        assertEquals(1500.0, auction.getCurrentHighestBid());
        assertEquals(bidder1, auction.getCurrentWinner());
    }

    @Test
    void testDatGia_nhieuLuot_nguoiDanCuoiLaNguoiThang() throws InvalidBidException {
        auction.startAuction(60);

        auction.addValidBid(new BidTransaction(bidder1, 1500.0));
        auction.addValidBid(new BidTransaction(bidder2, 2000.0));

        // bidder3 đặt thấp hơn bidder2 → phải throw exception
        assertThrows(InvalidBidException.class, () ->
            auction.addValidBid(new BidTransaction(bidder3, 1800.0))
        );

        // Sau tất cả, bidder2 vẫn dẫn đầu
        assertEquals(2000.0, auction.getCurrentHighestBid());
        assertEquals(bidder2, auction.getCurrentWinner());
    }

    @Test
    void testDatGia_bidder1_roi_bidder2_vuotLen() throws InvalidBidException {
        auction.startAuction(60);

        auction.addValidBid(new BidTransaction(bidder1, 1500.0));
        assertEquals(bidder1, auction.getCurrentWinner());

        auction.addValidBid(new BidTransaction(bidder2, 1600.0));
        assertEquals(bidder2, auction.getCurrentWinner());
        assertEquals(1600.0, auction.getCurrentHighestBid());
    }

    // -----------------------------------------------------------------------
    // 3. Đặt giá không hợp lệ → phải throw InvalidBidException
    // -----------------------------------------------------------------------

    @Test
    void testDatGia_bangGiaHienTai_throwException() {
        auction.startAuction(60);

        assertThrows(InvalidBidException.class, () ->
            auction.addValidBid(new BidTransaction(bidder1, 1000.0))
        );
    }

    @Test
    void testDatGia_thapHonGiaHienTai_throwException() {
        auction.startAuction(60);

        assertThrows(InvalidBidException.class, () ->
            auction.addValidBid(new BidTransaction(bidder1, 500.0))
        );
    }

    @Test
    void testDatGia_khiPhienChuaMo_throwException() {
        // Chưa gọi startAuction() → status = OPEN, không phải RUNNING
        assertThrows(InvalidBidException.class, () ->
            auction.addValidBid(new BidTransaction(bidder1, 2000.0))
        );
    }

    @Test
    void testDatGia_khiPhienDaDong_throwException() throws InterruptedException {
        auction.startAuction(1); // chỉ 1 giây
        Thread.sleep(1500);       // chờ phiên tự đóng

        assertThrows(InvalidBidException.class, () ->
            auction.addValidBid(new BidTransaction(bidder1, 9999.0))
        );
    }

    // -----------------------------------------------------------------------
    // 4. Concurrency — nhiều thread đặt giá cùng lúc
    //    Yêu cầu: không lost update, không 2 người cùng thắng
    // -----------------------------------------------------------------------

    @Test
    void testConcurrency_100Thread_khongLostUpdate() throws InterruptedException {
        auction.startAuction(30);

        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // Random bid amount để thực sự test race condition
        for (int i = 1; i <= threadCount; i++) {
            final double bidAmount = 1000.0 + (Math.random() * 5000.0);
            final Bidder bidder = new Bidder("user" + i, "pass", "Người " + i);

            executor.submit(() -> {
                try {
                    auction.addValidBid(new BidTransaction(bidder, bidAmount));
                    successCount.incrementAndGet();
                } catch (InvalidBidException ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Giá cuối phải cao hơn giá khởi điểm
        assertTrue(auction.getCurrentHighestBid() > 1000.0,
            "Giá cuối phải cao hơn giá khởi điểm");

        // Phải có đúng 1 người thắng
        assertNotNull(auction.getCurrentWinner(), "Phải có người thắng");

        System.out.println("Concurrency: " + successCount.get()
            + " bid thành công / " + threadCount + " thread");
        System.out.println("Giá cao nhất: " + auction.getCurrentHighestBid());
        System.out.println("Người thắng: " + auction.getCurrentWinner().getFullName());
    }

    @Test
    void testConcurrency_bidCungGia_chiMotNguoiThang() throws InterruptedException {
        auction.startAuction(30);

        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final Bidder bidder = new Bidder("u" + i, "p", "Người " + i);
            executor.submit(() -> {
                try {
                    auction.addValidBid(new BidTransaction(bidder, 2000.0));
                    successCount.incrementAndGet();
                } catch (InvalidBidException ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Do synchronized, tối đa 1 người đặt 2000 thành công
        assertTrue(successCount.get() <= 1,
            "Không thể có 2 người cùng đặt 1 mức giá thành công");

        System.out.println("Bid cùng giá: " + successCount.get() + " người thành công");
    }
}
