package com.auction.server.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.auction.common.model.bid.AutoBidConfig;
import java.util.List;
import org.junit.jupiter.api.Test;

class AutoBidDaoIntegrationTest extends DaoIntegrationTestSupport {

    private final AutoBidDao autoBidDao = new AutoBidDao();

    @Test
    void saveUpdateReadAndRemoveAutoBidConfigWork() throws Exception {
        AdminTestData.Seed seed = AdminTestData.createAuction("RUNNING", 1_000_000L, 1_000_000L, true);

        try {
            assertThat(autoBidDao.saveOrUpdateAutoBid(
                    seed.auctionId(), seed.bidderId(), 2_000_000L, 100_000L)).isTrue();
            assertThat(autoBidDao.getMaxAutoBid(seed.auctionId(), seed.bidderId())).isEqualTo(2_000_000L);
            assertThat(autoBidDao.getAutoBidStep(seed.auctionId(), seed.bidderId())).isEqualTo(100_000L);

            assertThat(autoBidDao.saveOrUpdateAutoBid(
                    seed.auctionId(), seed.bidderId(), 2_500_000L, 200_000L)).isTrue();
            assertThat(autoBidDao.getMaxAutoBid(seed.auctionId(), seed.bidderId())).isEqualTo(2_500_000L);
            assertThat(autoBidDao.getAutoBidStep(seed.auctionId(), seed.bidderId())).isEqualTo(200_000L);

            List<AutoBidConfig> configs = autoBidDao.getAuctionAutoBids(seed.auctionId());
            assertThat(configs)
                    .singleElement()
                    .satisfies(config -> {
                        assertThat(config.getBidder().getId()).isEqualTo(seed.bidderId());
                        assertThat(config.getMaxBid()).isEqualTo(2_500_000L);
                        assertThat(config.getBidStep()).isEqualTo(200_000L);
                    });

            assertThat(autoBidDao.removeAutoBid(seed.auctionId(), seed.bidderId())).isTrue();
            assertThat(autoBidDao.getMaxAutoBid(seed.auctionId(), seed.bidderId())).isEqualTo(-1L);
            assertThat(autoBidDao.getAuctionAutoBids(seed.auctionId())).isEmpty();
        } finally {
            AdminTestData.cleanup(seed);
        }
    }
}
