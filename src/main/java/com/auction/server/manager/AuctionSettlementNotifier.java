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
                "Phien " + auctionId + " da qua han thanh toan. He thong tu dong huy va tru phi phat 10% cho san pham " + safeItemName + ".",
                false
        );

        if (sellerId > 0) {
            SystemNotificationManager.getInstance().guiThongBaoRieng(
                    auctionId,
                    sellerId,
                    "Bidder da qua han thanh toan o phien " + auctionId + ". He thong da tu dong huy va chuyen phi phat cho ban.",
                    false
            );
        }
    }

    private static String taoNoiDungThongBaoThanhToan(String itemName, int auctionId) {
        return "Chuc mung ban da chien thang phien dau gia " + itemName
                + " cua phien ID " + auctionId + ".\n"
                + "Xac nhan thanh toan de chinh thuc so huu san pham.\n\n"
                + "Neu huy thanh toan, ban se chiu phat 10% tien dat gia.";
    }

    private static String taoNoiDungThongBaoSeller(String itemName, int auctionId, String winnerName) {
        return "Chuc mung san pham " + itemName + " phien " + auctionId
                + " da duoc ban thanh cong, nguoi chien thang la " + winnerName + ".";
    }
}
