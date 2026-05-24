package com.auction.server.dao;

import com.auction.server.db.DatabaseConnection;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;

abstract class DaoIntegrationTestSupport {

    @BeforeAll
    static void initDatabase() {
        try (java.sql.Connection ignored = DatabaseConnection.getInstance().getConnection()) {
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "database is not reachable.");
        }
    }
}
