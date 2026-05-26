package com.auction.server.manager;

import com.auction.common.enums.AuctionStatus;
import com.auction.server.dao.AuctionDao;
import com.auction.server.dao.BidderMoneySellerDao;
import com.auction.server.dao.BidderPenaltyDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

final class AuctionPaymentTimeoutService {
    private static final Logger logger = LoggerFactory.getLogger(AuctionPaymentTimeoutService.class);
    private static final long PAYMENT_TIMEOUT_MINUTES = 24 * 60;

    private final ScheduledExecutorService scheduler;
    private final Map<Integer, ScheduledFuture<?>> paymentTimeoutTasks = new ConcurrentHashMap<>();

    AuctionPaymentTimeoutService(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    void schedulePaymentTimeout(int auctionId, AuctionDao.AuctionNotificationTargets targets) {
        if (targets == null || targets.winnerId == null) {
            return;
        }
        cancelPaymentTimeout(auctionId);
        ScheduledFuture<?> task = scheduler.schedule(
                () -> autoCancelOverduePayment(auctionId, targets),
                PAYMENT_TIMEOUT_MINUTES,
                TimeUnit.MINUTES);
        paymentTimeoutTasks.put(auctionId, task);
        logger.info("Da len lich tu dong huy thanh toan cho phien {} sau {} phut.",
                auctionId, PAYMENT_TIMEOUT_MINUTES);
    }

    void cancelPaymentTimeout(int auctionId) {
        ScheduledFuture<?> task = paymentTimeoutTasks.remove(auctionId);
        if (task != null && !task.isDone()) {
            task.cancel(false);
        }
    }

    private void autoCancelOverduePayment(int auctionId, AuctionDao.AuctionNotificationTargets targets) {
        try {
            if (targets == null || targets.winnerId == null) {
                return;
            }
            BidderMoneySellerDao.PaymentResult ketQua =
                    new BidderMoneySellerDao().settleBuyNow(auctionId, targets.winnerId, false);

            if (!ketQua.success) {
                logger.info("Auto settlement skipped for auction {}: {}", auctionId, ketQua.message);
                if (isInsufficientBalanceMessage(ketQua.message)) {
                    applyLatePaymentPenalty(
                            auctionId,
                            targets.winnerId,
                            targets.sellerId,
                            "Không đủ số dư để thanh toán quá hạn."
                    );
                }
                return;
            }

            AuctionManager.getInstance().updateStatusAfterPayment(auctionId, AuctionStatus.CANCELED);
            String itemName = ketQua.itemName == null || ketQua.itemName.isBlank()
                    ? "sản phẩm"
                    : ketQua.itemName;

            SystemNotificationManager.getInstance().sendPrivateNotification(
                    auctionId,
                    targets.winnerId,
                    "Phiên " + auctionId
                            + " đã quá hạn thanh toán. Hệ thống tự động hủy và trừ phí phạt 10% cho sản phẩm "
                            + itemName + ".",
                    false);

            applyLatePaymentPenalty(auctionId, targets.winnerId, targets.sellerId, "Quá hạn thanh toán phiên đấu giá.");

            if (targets.sellerId > 0) {
                SystemNotificationManager.getInstance().sendPrivateNotification(
                        auctionId,
                        targets.sellerId,
                        "Bidder đã quá hạn thanh toán ở phiên " + auctionId
                                + ". Hệ thống đã tự động hủy và chuyển phí phạt cho bạn.",
                        false);
            }
            logger.info("Auto settlement success for auction {}.", auctionId);
        } catch (Exception e) {
            logger.error("Auto settlement failed for auction {}.", auctionId, e);
        } finally {
            paymentTimeoutTasks.remove(auctionId);
        }
    }

    private boolean isInsufficientBalanceMessage(String message) {
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase();
        return normalized.contains("không đủ") || normalized.contains("khong du");
    }

    private void applyLatePaymentPenalty(int auctionId, int winnerId, int sellerId, String reason) {
        BidderPenaltyDao.SanctionResult sanction =
                new BidderPenaltyDao().recordLatePaymentViolation(winnerId, reason);
        if (sanction.violationCount <= 0) {
            return;
        }

        String bidderMessage;
        if (sanction.permanentLock) {
            UserManager.getInstance().updateAccountStatus(winnerId, "LOCKED");
            bidderMessage = "Bạn đã vi phạm quá hạn thanh toán " + sanction.violationCount
                    + " lần. Tài khoản bị khóa vĩnh viễn, vui lòng liên hệ Admin.";
        } else {
            bidderMessage = "Bạn đã vi phạm quá hạn thanh toán lần " + sanction.violationCount
                    + ". Tạm cấm đấu giá đến " + sanction.lockUntil + ".";
        }

        SystemNotificationManager.getInstance().sendPrivateNotification(
                auctionId,
                winnerId,
                bidderMessage,
                false);

        if (sellerId > 0) {
            SystemNotificationManager.getInstance().sendPrivateNotification(
                    auctionId,
                    sellerId,
                    "Hệ thống đã áp dụng xử phạt bidder vì quá hạn thanh toán.",
                    false
            );
        }
    }
}
