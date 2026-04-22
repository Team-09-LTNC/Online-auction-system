package com.auction.observer;

import com.auction.model.bid.BidTransaction;
import com.auction.auction.AuctionStatus;

public interface AuctionObserver {
    //gọi khi có người đặt giá mới
    void onNewBid(BidTransaction bid);

    //gọi khi trạng thái phiên đấu giá thay đổi (OPEN -> RUNNING -> FINISHED)
    void onStatusChanged(AuctionStatus newStatus);
}