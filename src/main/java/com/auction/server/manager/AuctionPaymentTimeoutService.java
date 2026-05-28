package com.auction.server.manager;

import com.auction.common.enums.AuctionStatus;
import com.auction.server.dao.AuctionDao;
import com.auction.server.dao.BidderMoneySellerDao;
import com.auction.server.dao.BidderPenaltyDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

final class AuctionPaymentTimeoutService {
    private static final Logger logger = LoggerFactory.getLogger(AuctionPaymentTimeoutService.class);
    private static final long PAYMENT_TIMEOUT_MINUTES = 24 * 60;
    private static final long OVERDUE_SWEEP_INITIAL_DELAY_SECONDS = 5;
    private static final long OVERDUE_SWEEP_INTERVAL_MINUTES = 1;

    private final AuctionDao auctionDao;
    private final ScheduledExecutorService scheduler;
    private final Map<Integer, ScheduledFuture<?>> paymentTimeoutTasks = new ConcurrentHashMap<>();
    private final Set<Integer> processingAuctions = ConcurrentHashMap.newKeySet();

    AuctionPaymentTimeoutService(ScheduledExecutorService scheduler, AuctionDao auctionDao) {
        this.scheduler = scheduler;
        this.auctionDao = auctionDao;
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

    void startOverduePaymentSweep() {
        scheduler.scheduleWithFixedDelay(
                this::processOverduePayments,
                OVERDUE_SWEEP_INITIAL_DELAY_SECONDS,
                TimeUnit.MINUTES.toSeconds(OVERDUE_SWEEP_INTERVAL_MINUTES),
                TimeUnit.SECONDS
        );
    }

    private void processOverduePayments() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(PAYMENT_TIMEOUT_MINUTES);
            List<AuctionDao.AuctionNotificationTargets> overdueTargets =
                    auctionDao.getOverduePaymentTargets(cutoff);
            for (AuctionDao.AuctionNotificationTargets targets : overdueTargets) {
                if (targets != null) {
                    autoCancelOverduePayment(targets.auctionId, targets);
                }
            }
        } catch (Exception e) {
            logger.error("Overdue payment sweep failed.", e);
        }
    }

    private void autoCancelOverduePayment(int auctionId, AuctionDao.AuctionNotificationTargets targets) {
        if (!processingAuctions.add(auctionId)) {
            return;
        }
        try {
            if (targets == null || targets.winnerId == null) {
                return;
            }
            BidderMoneySellerDao.PaymentResult ketQua =
                    new BidderMoneySellerDao().settleBuyNow(auctionId, targets.winnerId, false);

            if (!ketQua.success) {
                logger.info("Auto settlement skipped for auction {}: {}", auctionId, ketQua.message);
                if (isInsufficientBalanceMessage(ketQua.message)) {
                    int winnerId = ketQua.bidderId > 0 ? ketQua.bidderId : targets.winnerId;
                    int sellerId = ketQua.sellerId > 0 ? ketQua.sellerId : targets.sellerId;
                    String itemName = safeItemName(ketQua.itemName != null ? ketQua.itemName : targets.itemName);
                    cancelFinishedAuctionAfterUnpaidPenalty(auctionId);
                    notifyInsufficientPenaltyBalance(
                            auctionId,
                            winnerId,
                            sellerId,
                            itemName,
                            ketQua.amount
                    );
                    applyLatePaymentPenalty(
                            auctionId,
                            winnerId,
                            sellerId,
                            "Không đủ số dư để thanh toán quá hạn."
                    );
                }
                return;
            }

            AuctionManager.getInstance().updateStatusAfterPayment(auctionId, AuctionStatus.CANCELED);
            String itemName = safeItemName(ketQua.itemName != null ? ketQua.itemName : targets.itemName);

            SystemNotificationManager.getInstance().sendPrivateNotification(
                    auctionId,
                    targets.winnerId,
                    "Phiên " + auctionId
                            + " đã quá hạn thanh toán. Hệ thống tự động hủy và trừ phí phạt 10% cho sản phẩm "
                            + itemName + ". Bạn không bị khóa vì ví đủ để xử lý phí phạt.",
                    false);

            sendPenaltyBalanceNotifications(
                    auctionId,
                    targets.winnerId,
                    targets.sellerId,
                    itemName,
                    ketQua.amount
            );

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
            processingAuctions.remove(auctionId);
        }
    }

    private void cancelFinishedAuctionAfterUnpaidPenalty(int auctionId) {
        if (auctionDao.updateStatusIfCurrent(auctionId, AuctionStatus.FINISHED.name(), AuctionStatus.CANCELED.name())) {
            AuctionManager.getInstance().updateStatusAfterPayment(auctionId, AuctionStatus.CANCELED);
        }
    }

    private boolean isInsufficientBalanceMessage(String message) {
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase();
        return normalized.contains("không đủ") || normalized.contains("khong du");
    }

    private void notifyInsufficientPenaltyBalance(
            int auctionId,
            int winnerId,
            int sellerId,
            String itemName,
            long penaltyAmount
    ) {
        String amountText = formatMoney(penaltyAmount);
        SystemNotificationManager.getInstance().sendPrivateNotification(
                auctionId,
                winnerId,
                "Phiên " + auctionId + " đã quá hạn thanh toán nhưng ví không đủ để trừ phí phạt 10% ("
                        + amountText + ") cho sản phẩm " + itemName + ". Hệ thống đã hủy phiên và áp dụng xử phạt.",
                false
        );

        if (sellerId > 0) {
            SystemNotificationManager.getInstance().sendPrivateNotification(
                    auctionId,
                    sellerId,
                    "Bidder quá hạn thanh toán phiên " + auctionId
                            + " nhưng ví không đủ để chuyển phí phạt. Hệ thống đã hủy phiên và áp dụng xử phạt.",
                    false
            );
        }
    }

    private void sendPenaltyBalanceNotifications(
            int auctionId,
            int winnerId,
            int sellerId,
            String itemName,
            long penaltyAmount
    ) {
        String amountText = formatMoney(penaltyAmount);
        SystemNotificationManager.getInstance().sendPrivateNotification(
                auctionId,
                winnerId,
                "Biến động số dư\n🔻 PAYMENT_SENT: " + amountText
                        + " - Phạt quá hạn thanh toán sản phẩm " + itemName + " của phiên ID " + auctionId,
                false
        );

        if (sellerId > 0) {
            SystemNotificationManager.getInstance().sendPrivateNotification(
                    auctionId,
                    sellerId,
                    "Biến động số dư\n🔹 PAYMENT_RECEIVED: " + amountText
                            + " - Nhận phí phạt quá hạn thanh toán sản phẩm " + itemName
                            + " của phiên ID " + auctionId,
                    false
            );
        }
    }

    private String safeItemName(String itemName) {
        return itemName == null || itemName.isBlank() ? "sản phẩm" : itemName;
    }

    private String formatMoney(long amount) {
        NumberFormat numberFormat = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
        return numberFormat.format(amount) + " VND";
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
