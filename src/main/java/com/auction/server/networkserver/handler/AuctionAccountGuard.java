package com.auction.server.networkserver.handler;

import com.auction.common.model.user.User;
import com.auction.server.dao.AuctionDao;
import com.auction.server.dao.BidderPenaltyDao;
import com.auction.server.dao.UserDao;

final class AuctionAccountGuard {
    private final AuctionDao auctionDao;
    private final UserDao userDao = new UserDao();

    AuctionAccountGuard(AuctionDao auctionDao) {
        this.auctionDao = auctionDao;
    }

    boolean isAuctionSeller(int auctionId, int userId) {
        return AuctionControllerUtil.isAuctionSeller(auctionDao, auctionId, userId);
    }

    boolean isAccountLocked(User nguoiDung) {
        if (nguoiDung == null) {
            return false;
        }
        if ("LOCKED".equalsIgnoreCase(nguoiDung.getStatus())) {
            return true;
        }
        BidderPenaltyDao.LockInfo lockInfo = new BidderPenaltyDao().getTemporaryLockInfo(nguoiDung.getId());
        if (lockInfo.locked) {
            return true;
        }
        return userDao.findByUsername(nguoiDung.getUsername())
                .map(user -> "LOCKED".equalsIgnoreCase(user.getStatus()))
                .orElse(false);
    }
}
