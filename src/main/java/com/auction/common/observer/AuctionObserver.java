package com.auction.common.observer;

import com.auction.common.model.bid.BidTransaction;
import com.auction.common.enums.AuctionStatus;

public interface AuctionObserver {
    /** Gọi khi có người đặt giá mới */
    void onNewBid(BidTransaction transaction);

    /** Gọi khi trạng thái phiên đấu giá thay đổi (OPEN -> RUNNING -> FINISHED -> PAID/CANCELED) */
    void onStatusChanged(AuctionStatus newStatus);

    /** Gọi khi có thông báo mới */
    void onChatMessage(String senderName, String message, boolean isSystem);
}