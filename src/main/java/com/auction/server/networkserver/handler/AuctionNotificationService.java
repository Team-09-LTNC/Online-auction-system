package com.auction.server.networkserver.handler;

import com.auction.server.manager.SystemNotificationManager;

import java.text.NumberFormat;
import java.util.Locale;

final class AuctionNotificationService {
    private AuctionNotificationService() {
    }

    static void sendBuyNowNotification(int auctionId, int bidderId, int sellerId, String tenPhien, String tenNguoiThang) {
        SystemNotificationManager.getInstance().sendPrivateNotification(
                auctionId,
                bidderId,
                buildPaymentNotificationContent(tenPhien),
                true
        );

        if (sellerId > 0) {
            SystemNotificationManager.getInstance().sendPrivateNotification(
                    auctionId,
                    sellerId,
                    buildSellerNotificationContent(tenPhien, auctionId, tenNguoiThang),
                    false
            );
        }
    }

    static void sendPostSettlementNotification(
            int auctionId,
            int bidderId,
            int sellerId,
            String tenPhien,
            long soTien,
            boolean thanhToan
    ) {
        String soTienText = formatMoney(soTien);
        SystemNotificationManager notificationManager = SystemNotificationManager.getInstance();

        if (sellerId > 0) {
            String sellerStatusMessage = thanhToan
                    ? "Phiên đấu giá " + tenPhien + " đã được thanh toán thành công bởi bidder."
                    : "Phiên đấu giá " + tenPhien + " đã bị bidder hủy thanh toán.";
            String sellerBalanceMessage = thanhToan
                    ? "Số dư ví của bạn đã tăng " + soTienText + " từ phiên " + tenPhien + "."
                    : "Số dư ví của bạn đã tăng " + soTienText + " từ phí phạt hủy thanh toán của phiên " + tenPhien + ".";
            notificationManager.sendPrivateNotification(auctionId, sellerId, sellerStatusMessage, false);
            notificationManager.sendPrivateNotification(auctionId, sellerId, sellerBalanceMessage, false);
        }

        String bidderBalanceMessage = thanhToan
                ? "Số dư ví của bạn đã giảm " + soTienText + " để thanh toán phiên " + tenPhien + "."
                : "Số dư ví của bạn đã giảm " + soTienText + " do hủy thanh toán phiên " + tenPhien + ".";
        notificationManager.sendPrivateNotification(auctionId, bidderId, bidderBalanceMessage, false);
    }

    private static String formatMoney(long amount) {
        NumberFormat numberFormat = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
        return numberFormat.format(amount) + " VND";
    }

    private static String buildPaymentNotificationContent(String tenPhien) {
        return "Chúc mừng bạn đã chiến thắng phiên đấu giá " + tenPhien + ".\n"
                + "Xác nhận thanh toán để chính thức sở hữu sản phẩm.\n\n"
                + "Nếu hủy thanh toán, bạn sẽ chịu phạt 10% tiền đặt giá.";
    }

    private static String buildSellerNotificationContent(String tenPhien, int auctionId, String tenNguoiThang) {
        return "Chúc mừng sản phẩm " + tenPhien + " phiên " + auctionId
                + " đã được bán thành công, người chiến thắng là " + tenNguoiThang + ".";
    }
}
