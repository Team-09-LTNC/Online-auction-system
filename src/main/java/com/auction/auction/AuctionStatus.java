package com.auction.auction;

// Các trạng thái của 1 phiên đấu giá
public enum AuctionStatus {
    OPEN,       // khởi tao phiên đấu giá
    RUNNING,    // đang diễn ra, được phép đặt giá
    FINISHED,   // đã hết giờ
    PAID,       // đã thanh toán
    CANCELED    // bị hủy
}