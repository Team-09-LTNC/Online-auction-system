package com.auction.common.enums;

/**
 * Định nghĩa các mã lỗi nghiệp vụ chuẩn xác để Server trả về Client.
 * Giúp Client dễ dàng dùng switch-case để hiển thị thông báo lỗi phù hợp lên UI.
 */

// Phân loại cấp độ nghiệp vụ
public class ErrorCode {

    // --- LỖI XÁC THỰC (AUTH) ---
    public static final String UNAUTHORIZED = "ERR_UNAUTHORIZED";             // Chưa đăng nhập hoặc token/session hết hạn
    public static final String INVALID_CREDENTIALS = "ERR_INVALID_CREDS";     // Sai username hoặc password
    public static final String USERNAME_ALREADY_EXISTS = "ERR_USER_EXISTS";   // Đăng ký trùng tên

    // --- LỖI PHÂN QUYỀN & REQUEST
    public static final String FORBIDDEN = "ERR_FORBIDDEN";                   // Không có quyền thao tác (Ví dụ: Sửa sản phẩm của người khác)
    public static final String BAD_REQUEST = "ERR_BAD_REQUEST";               // Lỗi định dạng dữ liệu gửi lên (Thiếu trường, sai kiểu)

    // --- LỖI NGHIỆP VỤ ĐẤU GIÁ (AUCTION) ---
    public static final String INVALID_BID_AMOUNT = "ERR_INVALID_BID";        // Giá đặt thấp hơn hoặc bằng giá hiện tại
    public static final String AUCTION_CLOSED = "ERR_AUCTION_CLOSED";         // Đấu giá khi phiên đã đóng
    public static final String BIDDER_NOT_JOINED = "ERR_NOT_JOINED";          // Chưa tham gia phòng mà đòi đặt giá

    // --- LỖI CONCURRENCY (ĐỒNG THỜI) ---
    public static final String CONCURRENT_CONFLICT = "ERR_RACE_CONDITION";    // Trùng đột do nhiều người đặt cùng mili-giây (Lost update)

    // --- LỖI DỮ LIỆU CHUNG (DATA) ---
    public static final String ITEM_NOT_FOUND = "ERR_ITEM_NOT_FOUND";         // Sản phẩm không tồn tại hoặc đã bị xóa
    public static final String AUCTION_NOT_FOUND = "ERR_AUCTION_NOT_FOUND";   // Phiên đấu giá không tồn tại

    // --- LỖI HỆ THỐNG ---
    public static final String INTERNAL_SERVER_ERROR = "ERR_SERVER_FAULT";    // Lỗi chung (Database connection lost, NullPointer...)
}