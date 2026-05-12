package com.auction.server;

//import com.auction.server.db.DatabaseConnection;
//import com.auction.server.manager.AuctionManager;
//import com.auction.server.manager.ProductManager;
//import com.auction.server.manager.UserManager;
//import com.auction.server.network.ServerManager;

public class ServerApplication {

//    // Cấu hình cổng kết nối đồng bộ với Client (8080)
//    private static final int PORT = 8080;
//
//    public static void main(String[] args) {
//        System.out.println("=== HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN - SERVER ===");
//
//        try {
//            // 1. Khởi tạo kết nối Database (HikariCP)
//            System.out.println("Đang khởi tạo kết nối Cơ sở dữ liệu...");
//            DatabaseConnection.getInstance();
//
//            // 2. Kích hoạt các bộ quản lý (Managers - Singleton)
//            // AuctionManager sẽ tự động khởi động luồng kiểm tra phiên hết hạn
//            System.out.println("Đang kích hoạt các dịch vụ nghiệp vụ...");
//            UserManager.getInstance();
//            ProductManager.getInstance();
//            AuctionManager.getInstance();
//
//            // 3. Khởi tạo và chạy bộ quản lý mạng (ServerManager)
//            System.out.println("Đang mở cổng kết nối mạng...");
//            ServerManager networkManager = new ServerManager(PORT);
//
//            // 4. Bắt đầu lắng nghe kết nối (Phương thức này sẽ chặn luồng main)
//            System.out.println("Server đã sẵn sàng tiếp nhận người dùng tại cổng: " + PORT);
//            networkManager.batDauServer();
//
//        } catch (Exception e) {
//            System.err.println("THẤT BẠI khi khởi động Server: " + e.getMessage());
//            e.printStackTrace();
//            System.exit(1);
//        }
//    }
}