package com.auction.client.controller.seller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class MyProductsHelperTest {

    @Test
    void parsesDateAndHourMinuteText() {
        LocalDate date = LocalDate.of(2026, 5, 28);

        assertThat(MyProductsHelper.parseDateTime(date, "09:30"))
                .isEqualTo(LocalDateTime.of(2026, 5, 28, 9, 30));
        assertThat(MyProductsHelper.parseDateTime(date, ""))
                .isEqualTo(LocalDateTime.of(2026, 5, 28, 0, 0));
        assertThat(MyProductsHelper.parseDateTime(null, "09:30")).isNull();
        assertThat(MyProductsHelper.parseDateTime(date, "9h30")).isNull();
    }

    @Test
    void resolvesStoredAndTimeBasedDisplayStatus() {
        assertThat(MyProductsHelper.resolveDisplayStatus("PAID", "RUNNING")).isEqualTo("PAID");
        assertThat(MyProductsHelper.resolveDisplayStatus("OPEN", "UPCOMING")).isEqualTo("UPCOMING");
        assertThat(MyProductsHelper.resolveDisplayStatus("RUNNING", "LIVE")).isEqualTo("LIVE");
        assertThat(MyProductsHelper.resolveDisplayStatus("REJECTED", "LIVE")).isEqualTo("REJECTED");
        assertThat(MyProductsHelper.resolveDisplayStatus(null, "UPCOMING")).isEqualTo("UPCOMING");
    }
}
