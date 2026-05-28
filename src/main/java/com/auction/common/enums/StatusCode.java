// Phân loại cấp độ mạng/giao thức
package com.auction.common.enums;

public class StatusCode {

    // --- Thành công ---
    public static final int OK           = 200; // Xử lý thành công chung
    public static final int CREATED      = 201; // Tạo mới dữ liệu thành công (VD: Đăng ký, thêm sản phẩm)

    // --- Lỗi do người dùng (client) ---
    public static final int BAD_REQUEST  = 400; // Dữ liệu gửi lên bị sai hoặc vi phạm logic (VD: Đặt giá quá thấp)
    public static final int UNAUTHORIZED = 401; // Chưa đăng nhập
    public static final int FORBIDDEN    = 403; // Đã đăng nhập nhưng không có quyền (VD: Sai vai trò)
    public static final int NOT_FOUND    = 404; // Không tìm thấy dữ liệu (VD: Sản phẩm đã bị xóa)

    // --- Lỗi do hệ thống (server) ---
    public static final int SERVER_ERROR = 500; // Lỗi code trên server hoặc lỗi cơ sở dữ liệu
    public static final int UNAVAILABLE  = 503; // Server đang quá tải, không thể xử lý
}
