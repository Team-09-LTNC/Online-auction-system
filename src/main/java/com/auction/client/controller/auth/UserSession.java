package com.auction.client.controller.auth;

/**
 * 🔥 LỚP QUẢN LÝ PHIÊN ĐĂNG NHẬP (USER SESSION) CHUẨN KIẾN TRÚC
 * Đã sửa giá trị mặc định thành rỗng để tránh việc app tự ý nhận bừa vai trò BIDDER khi chưa đăng nhập.
 */
public class UserSession {
    private static String currentRole = "";      // 🔥 SỬA THÀNH RỖNG: Chưa login thì chưa có quyền gì cả!
    private static String username = "Khách";    // Mặc định ban đầu khi chưa xác thực
    private static int userId = 0;

    public static String getCurrentRole() {
        return currentRole;
    }

    public static void setCurrentRole(String role) {
        currentRole = role;
    }

    public static String getUsername() {
        return username;
    }

    public static void setUsername(String name) {
        username = name;
    }

    public static int getUserId() {
        return userId;
    }

    public static void setUserId(int id) {
        userId = id;
    }

    /**
     * Hàm dọn rác khi Đăng xuất: Đưa mọi thứ về trạng thái nguyên bản an toàn
     */
    public static void clear() {
        currentRole = "";      // 🔥 Reset về rỗng để Sidebar quét lại hiện đủ phân quyền
        username = "Khách";
        userId = 0;
    }
}