package com.auction.server.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.auction.server.dao.BidderMoneySellerDao.PaymentResult;
import com.auction.server.db.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.Test;

class BidderMoneySellerDaoIntegrationTest extends DaoIntegrationTestSupport {

    private static final long INITIAL_BALANCE = 10_000_000L;

    private final BidderMoneySellerDao paymentDao = new BidderMoneySellerDao();

    @Test
    void settleBuyNowTransfersMoneyLogsWalletHistoryAndMarksAuctionPaid() throws Exception {
        AdminTestData.Seed seed = AdminTestData.createAuction("FINISHED", 1_000_000L, 1_700_000L, true);

        try {
            PaymentResult result = paymentDao.settleBuyNow(seed.auctionId(), seed.bidderId(), true);

            assertThat(result.success).isTrue();
            assertThat(result.auctionStatus).isEqualTo("PAID");
            assertThat(result.amount).isEqualTo(seed.currentPrice());
            assertThat(result.bidderId).isEqualTo(seed.bidderId());
            assertThat(result.sellerId).isEqualTo(seed.sellerId());

            assertThat(balanceOf(seed.bidderId())).isEqualTo(INITIAL_BALANCE - seed.currentPrice());
            assertThat(balanceOf(seed.sellerId())).isEqualTo(INITIAL_BALANCE + seed.currentPrice());
            assertThat(auctionStatus(seed.auctionId())).isEqualTo("PAID");
            assertThat(walletTransactionCount(seed.bidderId())).isEqualTo(1);
            assertThat(walletTransactionCount(seed.sellerId())).isEqualTo(1);
        } finally {
            AdminTestData.cleanup(seed);
        }
    }

    @Test
    void cancelSettlementChargesTenPercentAndDoesNotNeedPenaltyBanWhenBalanceIsEnough() throws Exception {
        AdminTestData.Seed seed = AdminTestData.createAuction("FINISHED", 1_000_000L, 1_700_000L, true);

        try {
            long expectedPenalty = 170_000L;

            PaymentResult result = paymentDao.settleBuyNow(seed.auctionId(), seed.bidderId(), false);

            assertThat(result.success).isTrue();
            assertThat(result.auctionStatus).isEqualTo("CANCELED");
            assertThat(result.amount).isEqualTo(expectedPenalty);
            assertThat(result.bidderTransactionType).isEqualTo("PAYMENT_SENT");
            assertThat(result.sellerTransactionType).isEqualTo("PAYMENT_RECEIVED");

            assertThat(balanceOf(seed.bidderId())).isEqualTo(INITIAL_BALANCE - expectedPenalty);
            assertThat(balanceOf(seed.sellerId())).isEqualTo(INITIAL_BALANCE + expectedPenalty);
            assertThat(auctionStatus(seed.auctionId())).isEqualTo("CANCELED");
            assertThat(walletTransactionCount(seed.bidderId())).isEqualTo(1);
            assertThat(walletTransactionCount(seed.sellerId())).isEqualTo(1);
        } finally {
            AdminTestData.cleanup(seed);
        }
    }

    @Test
    void cancelSettlementRejectsWhenBidderCannotCoverTenPercentPenalty() throws Exception {
        AdminTestData.Seed seed = AdminTestData.createAuction("FINISHED", 1_000_000L, 1_700_000L, true);

        try {
            updateBalance(seed.bidderId(), 169_999L);

            PaymentResult result = paymentDao.settleBuyNow(seed.auctionId(), seed.bidderId(), false);

            assertThat(result.success).isFalse();
            assertThat(result.message).contains("không đủ");
            assertThat(result.amount).isEqualTo(170_000L);
            assertThat(result.bidderId).isEqualTo(seed.bidderId());
            assertThat(result.sellerId).isEqualTo(seed.sellerId());
            assertThat(result.itemName).isNotBlank();
            assertThat(balanceOf(seed.bidderId())).isEqualTo(169_999L);
            assertThat(balanceOf(seed.sellerId())).isEqualTo(INITIAL_BALANCE);
            assertThat(auctionStatus(seed.auctionId())).isEqualTo("FINISHED");
            assertThat(walletTransactionCount(seed.bidderId())).isZero();
            assertThat(walletTransactionCount(seed.sellerId())).isZero();
            assertThat(penaltyCount(seed.bidderId())).isZero();
        } finally {
            AdminTestData.cleanup(seed);
        }
    }

    @Test
    void settleBuyNowRejectsNonWinningBidderWithoutChangingAuction() throws Exception {
        AdminTestData.Seed seed = AdminTestData.createAuction("FINISHED", 1_000_000L, 1_700_000L, true);

        try {
            PaymentResult result = paymentDao.settleBuyNow(seed.auctionId(), seed.sellerId(), true);

            assertThat(result.success).isFalse();
            assertThat(result.message).contains("bidder thắng");
            assertThat(auctionStatus(seed.auctionId())).isEqualTo("FINISHED");
            assertThat(balanceOf(seed.bidderId())).isEqualTo(INITIAL_BALANCE);
            assertThat(balanceOf(seed.sellerId())).isEqualTo(INITIAL_BALANCE);
        } finally {
            AdminTestData.cleanup(seed);
        }
    }

    private long balanceOf(int userId) throws Exception {
        return queryLong("SELECT balance FROM users WHERE id = ?", userId);
    }

    private int walletTransactionCount(int userId) throws Exception {
        return (int) queryLong("SELECT COUNT(*) FROM wallet_transactions WHERE user_id = ?", userId);
    }

    private int penaltyCount(int userId) throws Exception {
        new BidderPenaltyDao().getTemporaryLockInfo(userId);
        return (int) queryLong("SELECT COUNT(*) FROM bidder_penalties WHERE bidder_id = ?", userId);
    }

    private void updateBalance(int userId, long balance) throws Exception {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE users SET balance = ? WHERE id = ?")) {
            ps.setLong(1, balance);
            ps.setInt(2, userId);
            assertThat(ps.executeUpdate()).isEqualTo(1);
        }
    }

    private String auctionStatus(int auctionId) throws Exception {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT status FROM auctions WHERE id = ?")) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                return rs.getString("status");
            }
        }
    }

    private long queryLong(String sql, int id) throws Exception {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                return rs.getLong(1);
            }
        }
    }
}
