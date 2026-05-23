package com.auction.server.dao;

import com.auction.server.db.DatabaseConnection;
import com.auction.server.db.SetupDatabase;
import com.auction.server.db.UpdateDatabase;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;

abstract class DaoIntegrationTestSupport {

    @BeforeAll
    static void initDatabase() {
        try (java.sql.Connection ignored = DatabaseConnection.getInstance().getConnection()) {
            // DB reachable: continue
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Skip DB integration tests: database is not reachable.");
        }
        SetupDatabase.main(new String[0]);
        UpdateDatabase.main(new String[0]);
    }
}
