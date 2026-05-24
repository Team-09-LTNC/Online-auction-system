package com.auction.common.model.bid;

import com.auction.common.model.user.User;
import java.time.LocalDateTime;

/**
 * Cấu hình đấu giá tự động của một người dùng.
 * Implements Comparable để sử dụng cho PriorityQueue.
 */
public class AutoBidConfig implements Comparable<AutoBidConfig> {
    private final User bidder;
    private final long maxBid;
    private final long bidStep;
    private final LocalDateTime registerTime;

    public AutoBidConfig(User bidder, long maxBid) {
        this(bidder, maxBid, 0);
    }

    public AutoBidConfig(User bidder, long maxBid, long bidStep) {
        this.bidder = bidder;
        this.maxBid = maxBid;
        this.bidStep = bidStep;
        this.registerTime = LocalDateTime.now();
    }

    public User getBidder() { return bidder; }
    public long getMaxBid() { return maxBid; }
    public long getBidStep() { return bidStep; }
    public LocalDateTime getRegisterTime() { return registerTime; }

    @Override
    public int compareTo(AutoBidConfig other) {
        // 1. Ưu tiên người có MaxBid cao hơn
        int bidCompare = Long.compare(other.maxBid, this.maxBid);
        if (bidCompare != 0) return bidCompare;

        // 2. Nếu MaxBid bằng nhau, ưu tiên người đăng ký trước (registerTime nhỏ hơn)
        return this.registerTime.compareTo(other.registerTime);
    }
}
