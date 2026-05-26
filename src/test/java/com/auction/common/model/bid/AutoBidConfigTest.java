package com.auction.common.model.bid;

import com.auction.common.model.user.Bidder;
import java.time.LocalDateTime;
import java.util.PriorityQueue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoBidConfigTest {

    @Test
    void higherMaxBidHasHigherPriority() {
        AutoBidConfig low = new AutoBidConfig(new Bidder("u1", "p", "U1"), 100);
        AutoBidConfig high = new AutoBidConfig(new Bidder("u2", "p", "U2"), 200);

        PriorityQueue<AutoBidConfig> queue = new PriorityQueue<>();
        queue.offer(low);
        queue.offer(high);

        assertEquals(high, queue.poll());
        assertEquals(low, queue.poll());
    }

    @Test
    void earlierRegisterTimeWinsWhenMaxBidEqual() throws InterruptedException {
        AutoBidConfig early = new AutoBidConfig(new Bidder("u1", "p", "U1"), 200);
        Thread.sleep(5);
        AutoBidConfig late = new AutoBidConfig(new Bidder("u2", "p", "U2"), 200);

        PriorityQueue<AutoBidConfig> queue = new PriorityQueue<>();
        queue.offer(late);
        queue.offer(early);

        assertEquals(early, queue.poll());
        assertEquals(late, queue.poll());
    }

    @Test
    void earlierRegisterTimeWinsWhenMaxBidAndStepEqual() {
        LocalDateTime earlyTime = LocalDateTime.of(2026, 5, 26, 10, 0);
        LocalDateTime lateTime = earlyTime.plusMinutes(1);
        AutoBidConfig early = new AutoBidConfig(new Bidder("u1", "p", "U1"), 200, 10, earlyTime);
        AutoBidConfig late = new AutoBidConfig(new Bidder("u2", "p", "U2"), 200, 10, lateTime);

        PriorityQueue<AutoBidConfig> queue = new PriorityQueue<>();
        queue.offer(late);
        queue.offer(early);

        assertEquals(early, queue.poll());
        assertEquals(late, queue.poll());
    }
}
