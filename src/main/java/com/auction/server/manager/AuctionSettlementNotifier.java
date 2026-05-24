package com.auction.server.manager;

import com.auction.server.dao.AuctionDao;

final class AuctionSettlementNotifier {
    private AuctionSettlementNotifier() {
    }

    static void guiThongBaoKetThucPhien(AuctionDao.AuctionNotificationTargets targets) {
        if (targets == null || targets.winnerId == null) {
            return;
        }
        String itemName = targets.itemName == null ? "san pham" : targets.itemName;
        String winnerName = targets.winnerName == null ? "nguoi thang phien" : targets.winnerName;

        SystemNotificationManager.getInstance().guiThongBaoRieng(
                targets.auctionId,
                targets.winnerId,
                taoNoiDungThongBaoThanhToan(itemName, targets.auctionId),
                true
        );
        SystemNotificationManager.getInstance().guiThongBaoRieng(
                targets.auctionId,
                targets.sellerId,
                taoNoiDungThongBaoSeller(itemName, targets.auctionId, winnerName),
                false
        );
    }

    static void guiThongBaoQuaHanThanhToan(int auctionId, int winnerId, int sellerId, String itemName) {
        String safeItemName = (itemName == null || itemName.isBlank()) ? "san pham" : itemName;

        SystemNotificationManager.getInstance().guiThongBaoRieng(
                auctionId,
                winnerId,
                "Phiên " + auctionId + " đã quá hạn thanh toán. Hệ thống tự động hủy và trừ phí phạt 10% cho sản phẩm " + safeItemName + ".",
                false
        );

        if (sellerId > 0) {
            SystemNotificationManager.getInstance().guiThongBaoRieng(
                    auctionId,
                    sellerId,
                    "Bidder đã quá hạn thanh toán ở phiên " + auctionId + ". Hệ thống đã tự động hủy và chuyển phí phạt cho bạn.",
                    false
            );
        }
    }

    private static String taoNoiDungThongBaoThanhToan(String itemName, int auctionId) {
        return "Chúc mừng bạn đã chiến thắng phiên đấu giá " + itemName
                + " của phiên ID " + auctionId + ".\n"
                + "Xác nhận thanh toán để chính thức sở hữu sản phẩm.\n\n"
                + "Nếu hủy thanh toán, bạn sẽ chịu phạt 10% tiền đặt giá.";
    }

    private static String taoNoiDungThongBaoSeller(String itemName, int auctionId, String winnerName) {
        return "Chúc mừng sản phẩm " + itemName + " phiên " + auctionId
                + " đã được bán thành công, người chiến thắng là " + winnerName + ".";
    }
}
