package com.auction.server.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.auction.common.model.bid.BidLine;
import java.util.List;
import org.junit.jupiter.api.Test;

class FollowAndBidHistoryDaoIntegrationTest extends DaoIntegrationTestSupport {

    private final FollowDao followDao = new FollowDao();
    private final BidTransactionDao bidTransactionDao = new BidTransactionDao();

    @Test
    void followUnfollowAndCountWorkForAuction() throws Exception {
        AdminTestData.Seed seed = AdminTestData.createAuction("RUNNING", 1_000_000L, 1_000_000L, true);

        try {
            assertThat(followDao.follow(seed.bidderId(), seed.auctionId())).isTrue();
            assertThat(followDao.follow(seed.bidderId(), seed.auctionId())).isFalse();
            assertThat(followDao.isFollowing(seed.bidderId(), seed.auctionId())).isTrue();
            assertThat(followDao.countFollowedAuctions(seed.bidderId())).isEqualTo(1);
            assertThat(followDao.getFollowedAuctionIds(seed.bidderId())).contains(seed.auctionId());

            assertThat(followDao.unfollow(seed.bidderId(), seed.auctionId())).isTrue();
            assertThat(followDao.isFollowing(seed.bidderId(), seed.auctionId())).isFalse();
            assertThat(followDao.countFollowedAuctions(seed.bidderId())).isZero();
        } finally {
            AdminTestData.cleanup(seed);
        }
    }

    @Test
    void saveBidHistoryAndReadAuctionHistoryWork() throws Exception {
        AdminTestData.Seed seed = AdminTestData.createAuction("RUNNING", 1_000_000L, 1_000_000L, true);

        try {
            assertThat(bidTransactionDao.saveBidHistory(seed.auctionId(), seed.bidderId(), 1_100_000L)).isTrue();
            assertThat(bidTransactionDao.saveBidHistory(seed.auctionId(), seed.bidderId(), 1_200_000L)).isTrue();

            List<BidLine> history = bidTransactionDao.getAuctionBidHistory(seed.auctionId());

            assertThat(history).hasSizeGreaterThanOrEqualTo(2);
            assertThat(history)
                    .extracting(BidLine::getBidAmount)
                    .contains(1_100_000L, 1_200_000L);
            assertThat(history)
                    .extracting(BidLine::getBidderName)
                    .contains("Test BIDDER");
        } finally {
            AdminTestData.cleanup(seed);
        }
    }
}
