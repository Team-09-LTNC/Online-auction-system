package com.auction.server.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.auction.common.enums.ActionType;
import com.auction.server.networkserver.handler.AdminController;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class AdminControllerReadActionsTest extends DaoIntegrationTestSupport {

    private final AdminController controller = new AdminController();

    @Test
    void getPendingAuctionsIncludesRequestIdAndAuctionData() throws Exception {
        AdminTestData.Seed pending = AdminTestData.createAuction("PENDING", 1_000_000L, 1_000_000L, false);

        try {
            JsonObject response = send(ActionType.ADMIN_GET_PENDING_AUCTIONS, "req-admin-pending");
            JsonObject row = findById(response.getAsJsonArray("data"), "id", pending.auctionId());

            assertThat(response.get("type").getAsString()).isEqualTo("ADMIN_GET_PENDING_AUCTIONS_RESPONSE");
            assertThat(response.get("success").getAsBoolean()).isTrue();
            assertThat(response.get("requestId").getAsString()).isEqualTo("req-admin-pending");
            assertThat(row).isNotNull();
            assertThat(row.get("itemName").getAsString()).isEqualTo(pending.itemName());
            assertThat(row.get("sellerId").getAsInt()).isEqualTo(pending.sellerId());
        } finally {
            AdminTestData.cleanup(pending);
        }
    }

    @Test
    void getInvoicesCalculatesRevenueAndKeepsRequestId() throws Exception {
        AdminTestData.Seed paid = AdminTestData.createAuction("PAID", 1_000_000L, 1_900_000L, true);

        try {
            JsonObject response = send(ActionType.ADMIN_GET_INVOICES, "req-admin-invoices");
            JsonObject row = findById(response.getAsJsonArray("data"), "auctionId", paid.auctionId());

            assertThat(response.get("type").getAsString()).isEqualTo("ADMIN_GET_INVOICES_RESPONSE");
            assertThat(response.get("success").getAsBoolean()).isTrue();
            assertThat(response.get("requestId").getAsString()).isEqualTo("req-admin-invoices");
            assertThat(response.get("tongDoanhThu").getAsLong()).isGreaterThanOrEqualTo(paid.currentPrice());
            assertThat(row).isNotNull();
            assertThat(row.get("highestBid").getAsLong()).isEqualTo(paid.currentPrice());
            assertThat(row.get("winnerId").getAsInt()).isEqualTo(paid.bidderId());
        } finally {
            AdminTestData.cleanup(paid);
        }
    }

    @Test
    void getTransactionsReturnsTerminalAuctionRows() throws Exception {
        AdminTestData.Seed canceled = AdminTestData.createAuction("CANCELED", 1_000_000L, 1_000_000L, false);

        try {
            JsonObject response = send(ActionType.ADMIN_GET_TRANSACTIONS, "req-admin-transactions");
            JsonObject row = findById(response.getAsJsonArray("data"), "auctionId", canceled.auctionId());

            assertThat(response.get("type").getAsString()).isEqualTo("ADMIN_GET_TRANSACTIONS_RESPONSE");
            assertThat(response.get("success").getAsBoolean()).isTrue();
            assertThat(response.get("requestId").getAsString()).isEqualTo("req-admin-transactions");
            assertThat(row).isNotNull();
            assertThat(row.get("status").getAsString()).isEqualTo("CANCELED");
            assertThat(row.get("finalPrice").getAsLong()).isEqualTo(canceled.currentPrice());
        } finally {
            AdminTestData.cleanup(canceled);
        }
    }

    private JsonObject send(String actionType, String requestId) {
        JsonObject request = new JsonObject();
        request.addProperty("type", actionType);
        request.addProperty("requestId", requestId);
        return JsonParser.parseString(controller.handleRequest(request, null)).getAsJsonObject();
    }

    private JsonObject findById(JsonArray rows, String idField, int id) {
        for (var row : rows) {
            JsonObject object = row.getAsJsonObject();
            if (object.has(idField) && object.get(idField).getAsInt() == id) {
                return object;
            }
        }
        return null;
    }
}
