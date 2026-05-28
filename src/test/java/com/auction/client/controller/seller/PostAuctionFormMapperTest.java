package com.auction.client.controller.seller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PostAuctionFormMapperTest {

    @Test
    void parsesMoneyByKeepingOnlyDigits() {
        assertThat(PostAuctionFormMapper.parseMoney("1,250,000 VND")).isEqualTo(1_250_000L);
        assertThat(PostAuctionFormMapper.parseMoney("  500000  ")).isEqualTo(500_000L);
    }

    @Test
    void mapsVietnameseCategoryLabelToServerEnum() {
        assertThat(PostAuctionFormMapper.mapCategoryToEnum("Điện tử")).isEqualTo("ELECTRONICS");
        assertThat(PostAuctionFormMapper.mapCategoryToEnum("Xe cộ")).isEqualTo("VEHICLE");
        assertThat(PostAuctionFormMapper.mapCategoryToEnum("Nghệ thuật")).isEqualTo("ART");
        assertThat(PostAuctionFormMapper.mapCategoryToEnum("Khác")).isEqualTo("OTHER");
        assertThat(PostAuctionFormMapper.mapCategoryToEnum(null)).isEqualTo("OTHER");
    }
}
