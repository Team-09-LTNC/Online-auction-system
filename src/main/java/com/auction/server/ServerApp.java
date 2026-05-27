package com.auction.server;

import com.auction.common.util.NetworkConfig;
import com.auction.server.networkserver.ServerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ServerApplication: Điểm khởi chạy (Entry Point) của toàn bộ hệ thống Server.
 * Nhiệm vụ chính: Cấu hình cổng kết nối và kích hoạt ServerManager.
 */
public class ServerApp {
    private static final Logger logger = LoggerFactory.getLogger(ServerApp.class);

    //  giá trị mặc định của server,
//  nếu muốn test xem chạy oke không ae cứ sửa địa chỉ thành localhost máy mình trong application.properties trước để test
//  và không cần sửa đây, code  dưới đọc từ file properties, đây chỉ mặc định khi hệ thống chạy tốt
    private static int PORT = 8080;

    // CẤU HÌNH PORT ---
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
