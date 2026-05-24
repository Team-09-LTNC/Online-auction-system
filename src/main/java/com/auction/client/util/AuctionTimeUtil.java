package com.auction.client.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class AuctionTimeUtil {

    private static final DateTimeFormatter MULTI_FORMATTER =
            DateTimeFormatter.ofPattern(
                    "[yyyy-MM-dd HH:mm:ss.SSSSSS]" +
                            "[yyyy-MM-dd HH:mm:ss.SSS]" +
                            "[yyyy-MM-dd HH:mm:ss.S]" +
                            "[yyyy-MM-dd HH:mm:ss]" +
                            "[yyyy-MM-dd HH:mm]" +
                            "[yyyy-MM-dd'T'HH:mm:ss.SSSSSS]" +
                            "[yyyy-MM-dd'T'HH:mm:ss.SSS]" +
                            "[yyyy-MM-dd'T'HH:mm:ss.S]" +
                            "[yyyy-MM-dd'T'HH:mm:ss]" +
                            "[yyyy-MM-dd'T'HH:mm]"
            );

    public static class AuctionState {
        public final int countdownSeconds;
        public final String finalStatus;

        public AuctionState(int countdownSeconds, String finalStatus) {
            this.countdownSeconds = Math.max(countdownSeconds, 0);
            this.finalStatus = finalStatus;
        }
    }

    public static AuctionState calculateState(
            String startRaw,
            String endRaw,
            String serverNow
    ) {
        try {
            LocalDateTime now =
                    serverNow != null && !serverNow.isBlank()
                            ? parse(serverNow)
                            : LocalDateTime.now();

            LocalDateTime start = parse(startRaw);
            LocalDateTime end = parse(endRaw);

            if (now == null || start == null || end == null) {
                return new AuctionState(0, "FINISHED");
            }

            if (now.isBefore(start)) {
                int sec = (int) ChronoUnit.SECONDS.between(now, start);
                return new AuctionState(sec, "OPEN");
            }

            if (now.isBefore(end)) {
                int sec = (int) ChronoUnit.SECONDS.between(now, end);
                return new AuctionState(sec, "RUNNING");
            }

            return new AuctionState(0, "FINISHED");

        } catch (Exception e) {
            return new AuctionState(0, "FINISHED");
        }
    }

    public static LocalDateTime parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String normalized = raw.trim().replace(" ", "T");

        try {
            return LocalDateTime.parse(
                    normalized,
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME
            );
        } catch (Exception ignored) {
        }

        try {
            return LocalDateTime.parse(
                    normalized,
                    MULTI_FORMATTER
            );
        } catch (Exception e) {
            System.err.println(
                    "Lỗi parse thời gian: " + raw + " -> " + e.getMessage()
            );
            return null;
        }
    }

    public static long parseToMillis(String raw) {
        LocalDateTime dt = parse(raw);

        if (dt == null) {
            return 0;
        }

        return dt.atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }
}