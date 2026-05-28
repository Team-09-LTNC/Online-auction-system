package com.auction.server;

import com.auction.common.util.NetworkConfig;
import com.auction.server.networkserver.ServerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ứng dụng server: Điểm khởi chạy của toàn bộ hệ thống server.
 * Nhiệm vụ chính: Cấu hình cổng kết nối và kích hoạt bộ quản lý server.
 */
public class ServerApp {
    private static final Logger logger = LoggerFactory.getLogger(ServerApp.class);

    // Giá trị mặc định của server,
// Nếu muốn kiểm thử chạy ổn không thì sửa địa chỉ thành localhost máy mình trong application.properties trước để kiểm thử
// Và không cần sửa ở đây, code dưới đọc từ file cấu hình, đây chỉ là mặc định khi hệ thống chạy tốt
    private static int PORT = 8080;

    // CẤU HÌNH CỔNG 
    static {
        PORT = NetworkConfig.getServerPort();
        logger.info("Đã nạp cấu hình cổng server: {}", PORT);
    }

    public static int getPort() {
        return PORT;
    }

    public static void main(String[] args) {
        applyNetworkArgs(args);
        logger.info("==================================================");
        logger.info("   HỆ THỐNG MÁY CHỦ ĐẤU GIÁ ĐANG KHỞI ĐỘNG...   ");
        logger.info("   Đang lắng nghe tại cổng: {}                  ", PORT);
        logger.info("==================================================");

        ServerManager quanLyMayChu = new ServerManager(PORT);
        quanLyMayChu.startServer();
    }

    private static void applyNetworkArgs(String[] args) {
        if (args == null) {
            return;
        }
        for (String arg : args) {
            if (arg.startsWith("--server-port=")) {
                PORT = parsePort(arg.substring("--server-port=".length()), PORT);
            } else if (arg.startsWith("--server.port=")) {
                PORT = parsePort(arg.substring("--server.port=".length()), PORT);
            }
        }
    }

    private static int parsePort(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
