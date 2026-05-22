package com.auction.common.enums;

// Các trạng thái của 1 phiên đấu giá
public enum AuctionStatus {
    OPEN, // khởi tao phiên đấu giá
    RUNNING, // đang diễn ra, được phép đặt giá
    FINISHED, // đã hết giờ
    PAID, // đã thanh toán
    CANCELED, // bị hủy
    PENDING, // chờ admin duyệt
    REJECTED // bị admin từ chối
}