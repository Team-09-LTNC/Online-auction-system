package com.auction.server;

import com.auction.server.networkserver.ServerManager;
import com.auction.server.db.UpdateDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

/**
 * ServerApplication: Điểm khởi chạy (Entry Point) của toàn bộ hệ thống Server.
 * Nhiệm vụ chính: Cấu hình cổng kết nối và kích hoạt ServerManager.
 */
public class ServerApplication {
    private static final Logger logger = LoggerFactory.getLogger(ServerApplication.class);

    //  giá trị mặc định của server,
//  nếu muốn test xem chạy oke không ae cứ sửa địa chỉ thành localhost máy mình trong application.properties trước để test
//  và không cần sửa đây, code  dưới đọc từ file properties, đây chỉ mặc định khi hệ thống chạy tốt
    private static int PORT = 8080;

    // --- KHỐI STATIC: ĐỌC CẤU HÌNH PORT ---
    static {
        try (InputStream input = ServerApplication.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                Properties props = new Properties();
                props.load(input);
                PORT = Integer.parseInt(props.getProperty("server.port", "8080"));
                logger.info("Đã nạp cấu hình cổng từ application.properties: {}", PORT);
            } else {
                logger.warn("Không tìm thấy application.properties, dùng cổng mặc định 8080");
            }
        } catch (Exception e) {
            logger.error("Lỗi khi đọc file cấu hình, dùng cổng mặc định 8080", e);
        }
    }
    // ----------------------------------------

    public static int getPort() {
        return PORT;
    }

    public static void main(String[] args) {
        UpdateDatabase.main(new String[0]);

        logger.info("==================================================");
        logger.info("   HỆ THỐNG MÁY CHỦ ĐẤU GIÁ ĐANG KHỞI ĐỘNG...   ");
        logger.info("   Đang lắng nghe tại cổng: {}                  ", PORT);
        logger.info("==================================================");

        // Khởi tạo và ủy quyền toàn bộ việc quản lý mạng cho ServerManager
        ServerManager quanLyMayChu = new ServerManager(PORT);
        quanLyMayChu.batDauServer();
    }
}
