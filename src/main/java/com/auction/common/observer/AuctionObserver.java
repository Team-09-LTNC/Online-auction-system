package com.auction.common.observer;

import com.auction.common.model.bid.BidTransaction;
import com.auction.common.model.AuctionStatus;

public interface AuctionObserver {
    //gọi khi có người đặt giá mới
    void onNewBid(BidTransaction giaodich);

    //gọi khi trạng thái phiên đấu giá thay đổi (OPEN -> RUNNING -> FINISHED)
    void onStatusChanged(AuctionStatus newStatus);

}