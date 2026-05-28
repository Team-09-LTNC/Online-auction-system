package com.auction.common.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.time.LocalDateTime;

/**
 * Lớp cung cấp đối tượng Gson đã được cấu hình sẵn các bộ chuyển đổi cần thiết.
 * Đảm bảo tính nhất quán dữ liệu giữa client và server.
 */
public class GsonConfig {
    private static volatile Gson instance;

    private GsonConfig() {
        // Constructor private để chặn khởi tạo tự do theo mẫu Singleton
    }

    public static Gson getInstance() {
        if (instance == null) {
            synchronized (GsonConfig.class) {
                if (instance == null) {
                    instance = new GsonBuilder()
                            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                            .create();
                }
            }
        }
        return instance;
    }
}
