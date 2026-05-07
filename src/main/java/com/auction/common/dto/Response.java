package com.auction.common.dto;

// Lớp cha đại diện cho dữ liệu phản hồi từ Server về Client.
// Sẽ được Gson chuyển thành chuỗi JSON khi truyền qua Socket.
public abstract class Response {

    private boolean success; // Trạng thái xử lý của Server
    private String message;  // Thông báo từ Server dùng để hiển thị trực tiếp lên giao diện (UI)

    // Khởi tạo trạng thái và thông báo phản hồi
    public Response(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    // Client gọi hàm này để biết logic tiếp theo nên làm gì (chuyển màn hình hay báo lỗi)
    public boolean isSuccess() {
        return success;
    }

    // Client gọi hàm này để lấy chuỗi text hiển thị lên màn hinhf client
    public String getMessage() {
        return message;
    }
}