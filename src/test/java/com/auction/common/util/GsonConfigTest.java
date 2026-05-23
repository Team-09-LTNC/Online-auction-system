package com.auction.common.util;

import com.google.gson.Gson;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GsonConfigTest {

    @Test
    void getInstanceReturnsSingleton() {
        Gson first = GsonConfig.getInstance();
        Gson second = GsonConfig.getInstance();
        assertSame(first, second);
    }

    @Test
    void localDateTimeRoundTripWorks() {
        Gson gson = GsonConfig.getInstance();
        LocalDateTime value = LocalDateTime.of(2026, 5, 23, 12, 34, 56);

        String json = gson.toJson(value);
        LocalDateTime parsed = gson.fromJson(json, LocalDateTime.class);

        assertEquals(value, parsed);
    }

    @Test
    void invalidDateThrowsParseException() {
        Gson gson = GsonConfig.getInstance();
        assertThrows(DateTimeParseException.class,
                () -> gson.fromJson("\"not-a-date\"", LocalDateTime.class));
    }
}
