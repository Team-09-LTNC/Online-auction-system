package com.auction.client.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class AuctionTimeUtil {

    public static class AuctionState {
        public final int countdownSeconds;
        public final String finalStatus;

        public AuctionState(int countdownSeconds, String finalStatus) {
            this.countdownSeconds = countdownSeconds;
            this.finalStatus = finalStatus;
        }
    }

    public static AuctionState calculateState(String startRaw, String endRaw, String serverNow) {

        try {
            LocalDateTime now = (serverNow != null && !serverNow.isBlank())
                    ? parse(serverNow)
                    : LocalDateTime.now();

            LocalDateTime start = parse(startRaw);
            LocalDateTime end = parse(endRaw);

            System.out.println("========== AUCTION DEBUG ==========");
            System.out.println("NOW   = " + now);
            System.out.println("START = " + start);
            System.out.println("END   = " + end);
            System.out.println("SERVER NOW RAW = " + serverNow);
            System.out.println("===================================");

            if (start == null || end == null) {
                return new AuctionState(0, "FINISHED");
            }

            // chưa bắt đầu
            if (now.isBefore(start)) {
                int sec = (int) ChronoUnit.SECONDS.between(now, start);
                return new AuctionState(sec, "OPENING");
            }

            // đang diễn ra
            if (!now.isAfter(end)) {
                int sec = (int) ChronoUnit.SECONDS.between(now, end);
                return new AuctionState(sec, "RUNNING");
            }

            // đã kết thúc
            return new AuctionState(0, "FINISHED");

        } catch (Exception e) {
            return new AuctionState(0, "FINISHED");
        }
    }

    public static LocalDateTime parse(String raw) {
        if (raw == null || raw.isBlank()) return null;

        try {
            // Chuẩn hóa: Thay khoảng trắng bằng T (nếu có)
            String cleanRaw = raw.replace(" ", "T");

            // Xử lý trường hợp chuỗi chỉ có T14:00 (thiếu giây :00)
            if (cleanRaw.length() == 16) { // ví dụ 2026-05-22T14:00
                cleanRaw += ":00";
            }

            return LocalDateTime.parse(cleanRaw, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            System.err.println("LỖI PARSE THỜI GIAN: " + raw + " -> " + e.getMessage());
            return null;
        }
    }

    public static long parseToMillis(String raw) {
        LocalDateTime dt = parse(raw);
        if (dt == null) return 0;
        return dt.atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }
}