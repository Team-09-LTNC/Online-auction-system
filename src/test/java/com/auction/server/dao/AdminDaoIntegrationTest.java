package com.auction.server.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.auction.common.dto.AdminDTOs.InvoiceDTO;
import com.auction.common.dto.AdminDTOs.TransactionDTO;
import com.auction.common.enums.AuctionStatus;
import com.auction.common.model.bid.Auction;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdminDaoIntegrationTest extends DaoIntegrationTestSupport {

    private final AdminDao adminDao = new AdminDao();

    @Test
    void getPendingAuctionsReturnsOnlyPendingAuctions() throws Exception {
        AdminTestData.Seed pending = AdminTestData.createAuction("PENDING", 1_000_000L, 1_000_000L, false);
        AdminTestData.Seed running = AdminTestData.createAuction("RUNNING", 2_000_000L, 2_000_000L, false);

        try {
            List<Auction> auctions = adminDao.getPendingAuctions();

            assertThat(auctions)
                    .filteredOn(auction -> auction.getId() == pending.auctionId())
                    .singleElement()
                    .satisfies(auction -> {
                        assertThat(auction.getStoredStatus()).isEqualTo(AuctionStatus.PENDING);
                        assertThat(auction.getItem().getName()).isEqualTo(pending.itemName());
                        assertThat(auction.getItem().getSellerId()).isEqualTo(pending.sellerId());
                        assertThat(auction.getItem().getItemCategory()).isEqualTo("OTHER");
                        assertThat(auction.getItem().getStartingPrice()).isEqualTo(1_000_000L);
                    });

            assertThat(auctions)
                    .noneMatch(auction -> auction.getId() == running.auctionId());
        } finally {
            AdminTestData.cleanup(pending);
            AdminTestData.cleanup(running);
        }
    }

    @Test
    void getInvoicesReturnsOnlyPaidAuctions() throws Exception {
        AdminTestData.Seed paid = AdminTestData.createAuction("PAID", 1_000_000L, 1_800_000L, true);
        AdminTestData.Seed finished = AdminTestData.createAuction("FINISHED", 1_000_000L, 1_500_000L, true);

        try {
            List<InvoiceDTO> invoices = adminDao.getInvoices();

            assertThat(invoices)
                    .filteredOn(invoice -> invoice.getAuctionId() == paid.auctionId())
                    .singleElement()
                    .satisfies(invoice -> {
                        assertThat(invoice.getItemId()).isEqualTo(paid.itemId());
                        assertThat(invoice.getItemName()).isEqualTo(paid.itemName());
                        assertThat(invoice.getSellerId()).isEqualTo(paid.sellerId());
                        assertThat(invoice.getWinnerId()).isEqualTo(paid.bidderId());
                        assertThat(invoice.getHighestBid()).isEqualTo(1_800_000L);
                    });

            assertThat(invoices)
                    .noneMatch(invoice -> invoice.getAuctionId() == finished.auctionId());
        } finally {
            AdminTestData.cleanup(paid);
            AdminTestData.cleanup(finished);
        }
    }

    @Test
    void getTransactionsReturnsOnlyTerminalAuctions() throws Exception {
        AdminTestData.Seed finished = AdminTestData.createAuction("FINISHED", 1_000_000L, 1_600_000L, true);
        AdminTestData.Seed running = AdminTestData.createAuction("RUNNING", 1_000_000L, 1_200_000L, true);

        try {
            List<TransactionDTO> transactions = adminDao.getTransactions();

            assertThat(transactions)
                    .filteredOn(transaction -> transaction.getAuctionId() == finished.auctionId())
                    .singleElement()
                    .satisfies(transaction -> {
                        assertThat(transaction.getItemId()).isEqualTo(finished.itemId());
                        assertThat(transaction.getStatus()).isEqualTo("FINISHED");
                        assertThat(transaction.getWinnerId()).isEqualTo(finished.bidderId());
                        assertThat(transaction.getFinalPrice()).isEqualTo(1_600_000L);
                    });

            assertThat(transactions)
                    .noneMatch(transaction -> transaction.getAuctionId() == running.auctionId());
        } finally {
            AdminTestData.cleanup(finished);
            AdminTestData.cleanup(running);
        }
    }
}
