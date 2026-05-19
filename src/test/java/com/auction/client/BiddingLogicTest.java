package com.auction.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BiddingLogicTest {

    // Hàm giả lập logic: Giá mới phải > Giá hiện tại + Bước giá
    boolean checkBid(double currentPrice, double bidAmount, double increment) {
        return bidAmount >= (currentPrice + increment);
    }

    @Test
    void testValidBid() {
        // Case 1: Đặt giá đúng quy định (100 + 10, đặt 120 -> OK)
        assertTrue(checkBid(100.0, 120.0, 10.0), "Giá 120 phải hợp lệ khi giá hiện tại 100 và bước giá 10");
    }

    @Test
    void testInvalidBidTooLow() {
        // Case 2: Đặt giá quá thấp (100 + 10, đặt 105 -> Fail)
        assertFalse(checkBid(100.0, 105.0, 10.0), "Giá 105 phải bị từ chối vì chưa đủ bước giá");
    }

}
