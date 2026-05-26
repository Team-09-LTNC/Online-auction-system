package com.auction.server.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

class AdminDaoWriteActionsIntegrationTest extends DaoIntegrationTestSupport {

    private final AdminDao adminDao = new AdminDao();

    @Test
    void approveUpdateInfoAndDeleteAuctionWork() throws Exception {
        AdminTestData.Seed seed = AdminTestData.createAuction("PENDING", 1_000_000L, 1_000_000L, false);

        try {
            assertThat(adminDao.approveAuction(seed.auctionId(), "OPEN")).isTrue();

            JsonObject openInfo = adminDao.getAuctionInfo(seed.auctionId());
            assertThat(openInfo).isNotNull();
            assertThat(openInfo.get("status").getAsString()).isEqualTo("OPEN");
            assertThat(openInfo.get("start_time").getAsString()).isNotBlank();
            assertThat(openInfo.get("end_time").getAsString()).isNotBlank();

            assertThat(adminDao.updateAuctionStatus(seed.auctionId(), "CANCELED")).isTrue();
            JsonObject canceledInfo = adminDao.getAuctionInfo(seed.auctionId());
            assertThat(canceledInfo.get("status").getAsString()).isEqualTo("CANCELED");

            assertThat(adminDao.deleteAuction(seed.auctionId())).isTrue();
            assertThat(adminDao.getAuctionInfo(seed.auctionId())).isNull();
        } finally {
            AdminTestData.cleanup(seed);
        }
    }
}
