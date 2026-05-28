package com.auction.common.model.bid;

import com.auction.common.model.user.User;
import java.time.LocalDateTime;

/**
 * Cấu hình đấu giá tự động của một người dùng.
 * Cài đặt Comparable để sử dụng cho PriorityQueue.
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
        this(bidder, maxBid, bidStep, LocalDateTime.now());
    }

    public AutoBidConfig(User bidder, long maxBid, long bidStep, LocalDateTime registerTime) {
        this.bidder = bidder;
        this.maxBid = maxBid;
        this.bidStep = bidStep;
        this.registerTime = registerTime == null ? LocalDateTime.now() : registerTime;
    }

    public User getBidder() { return bidder; }
    public long getMaxBid() { return maxBid; }
    public long getBidStep() { return bidStep; }
    public LocalDateTime getRegisterTime() { return registerTime; }

    @Override
    public int compareTo(AutoBidConfig other) {
        // 1. Ưu tiên người có giá tự động tối đa cao hơn
        int bidCompare = Long.compare(other.maxBid, this.maxBid);
        if (bidCompare != 0) return bidCompare;

        // 2. Nếu giá tự động tối đa bằng nhau, ưu tiên người đăng ký trước (thời gian đăng ký nhỏ hơn)
        return this.registerTime.compareTo(other.registerTime);
    }
}
