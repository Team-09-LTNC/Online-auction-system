package com.auction.client.manager;

import static org.assertj.core.api.Assertions.assertThat;

import com.auction.common.dto.AdminDTOs.PendingAuctionDTO;
import com.auction.common.dto.AdminDTOs.TransactionDTO;
import com.auction.common.dto.AdminDTOs.UserSummaryDTO;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdminResponseMapperTest {
  @Test
  void mapsUserListWithDefaultStatus() {
    JsonObject user = new JsonObject();
    user.addProperty("username", "seller01");
    user.addProperty("fullname", "Seller One");

    JsonArray data = new JsonArray();
    data.add(user);

    List<UserSummaryDTO> users = AdminResponseMapper.toUsers(data);

    assertThat(users).hasSize(1);
    assertThat(users.getFirst().getUsername()).isEqualTo("seller01");
    assertThat(users.getFirst().getFullname()).isEqualTo("Seller One");
    assertThat(users.getFirst().getStatus()).isEqualTo("ACTIVE");
  }

  @Test
  void mapsPendingAuctionsWithSafeDefaults() {
    JsonObject auction = new JsonObject();
    auction.addProperty("id", 11);
    auction.addProperty("itemName", "Vintage Camera");
    auction.addProperty("sellerId", 7);
    auction.addProperty("startingPrice", 1_500_000L);
    auction.addProperty("startTime", "2026-05-27T10:00:00");
    auction.addProperty("endTime", "2026-05-27T11:00:00");

    JsonArray data = new JsonArray();
    data.add(auction);

    List<PendingAuctionDTO> auctions = AdminResponseMapper.toPendingAuctions(data);

    assertThat(auctions).hasSize(1);
    assertThat(auctions.getFirst().getId()).isEqualTo(11);
    assertThat(auctions.getFirst().getItemName()).isEqualTo("Vintage Camera");
    assertThat(auctions.getFirst().getCategory()).isEmpty();
    assertThat(auctions.getFirst().getImageUrl()).isNull();
  }

  @Test
  void mapsTransactionsForAdminReport() {
    JsonObject transaction = new JsonObject();
    transaction.addProperty("auctionId", 20);
    transaction.addProperty("itemId", 30);
    transaction.addProperty("itemName", "Laptop");
    transaction.addProperty("startTime", "2026-05-27T09:00:00");
    transaction.addProperty("endTime", "2026-05-27T10:00:00");
    transaction.addProperty("status", "FINISHED");
    transaction.addProperty("winnerId", 99);
    transaction.addProperty("finalPrice", 5_000_000L);

    JsonArray data = new JsonArray();
    data.add(transaction);

    List<TransactionDTO> transactions = AdminResponseMapper.toTransactions(data);

    assertThat(transactions).hasSize(1);
    assertThat(transactions.getFirst().getAuctionId()).isEqualTo(20);
    assertThat(transactions.getFirst().getWinnerId()).isEqualTo(99);
    assertThat(transactions.getFirst().getFinalPrice()).isEqualTo(5_000_000L);
  }
}
