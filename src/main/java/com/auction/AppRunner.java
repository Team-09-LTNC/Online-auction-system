package com.auction;

import com.auction.server.ServerApplication;
import com.auction.client.networkclient.Launcher;

public class AppRunner {
    public static void main(String[] args) {
        // 1. Chạy ServerApplication trên một luồng (Thread) riêng biệt
        Thread serverThread = new Thread(() -> {
            try {
                System.out.println("=== ĐANG KHỞI ĐỘNG SERVER ===");
                ServerApplication.main(args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Cài đặt Daemon = true để khi bạn tắt Client (tắt app), Server cũng tự động tắt theo
        serverThread.setDaemon(true);
        serverThread.start();

        // 2. Dừng luồng chính khoảng 1.5 giây để đảm bảo Server đã mở cổng 8080 thành công
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 3. Khởi chạy giao diện Client
        System.out.println("=== ĐANG KHỞI ĐỘNG CLIENT ===");
        Launcher.main(args);
    }
}