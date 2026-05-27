package com.auction.server.dao;

import com.auction.server.db.DatabaseConnection;
import java.sql.Connection;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeAll;

public abstract class DaoIntegrationTestSupport {

    @BeforeAll
    static void initDatabase() {
        System.setProperty(
                "db.config.file",
                System.getProperty("auction.dao.test.config", "application-test-h2.properties"));

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            ensureRequiredSchema(stmt);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot initialize DAO test database.", e);
        }
    }

    private static void ensureRequiredSchema(Statement stmt) throws Exception {
        stmt.execute("CREATE TABLE IF NOT EXISTS users ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "username VARCHAR(50) NOT NULL UNIQUE, "
                + "password VARCHAR(255) NOT NULL, "
                + "full_name VARCHAR(100) NOT NULL, "
                + "role VARCHAR(20) NOT NULL, "
                + "balance BIGINT DEFAULT 0, "
                + "status VARCHAR(20) DEFAULT 'ACTIVE', "
                + "lock_until DATETIME NULL"
                + ") ENGINE=InnoDB");

        stmt.execute("CREATE TABLE IF NOT EXISTS items ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "seller_id INT NOT NULL, "
                + "name VARCHAR(255) NOT NULL, "
                + "description TEXT, "
                + "category VARCHAR(50), "
                + "starting_price BIGINT NOT NULL, "
                + "bid_increment BIGINT NOT NULL DEFAULT 100000, "
                + "image_url VARCHAR(500), "
                + "FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE"
                + ") ENGINE=InnoDB");

        stmt.execute("CREATE TABLE IF NOT EXISTS auctions ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "item_id INT NOT NULL, "
                + "current_price BIGINT NOT NULL DEFAULT 0, "
                + "buy_now_price BIGINT DEFAULT NULL, "
                + "highest_bidder_id INT NULL, "
                + "start_time DATETIME NOT NULL, "
                + "end_time DATETIME NOT NULL, "
                + "status VARCHAR(20) DEFAULT 'OPEN', "
                + "anti_sniping_enabled BOOLEAN NOT NULL DEFAULT FALSE, "
                + "FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE, "
                + "FOREIGN KEY (highest_bidder_id) REFERENCES users(id) ON DELETE SET NULL"
                + ") ENGINE=InnoDB");

        stmt.execute("CREATE TABLE IF NOT EXISTS bid_history ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "auction_id INT NOT NULL, "
                + "bidder_id INT NOT NULL, "
                + "bid_amount BIGINT NOT NULL, "
                + "bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE, "
                + "FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE"
                + ") ENGINE=InnoDB");

        stmt.execute("CREATE TABLE IF NOT EXISTS auto_bid_settings ("
                + "auction_id INT NOT NULL, "
                + "bidder_id INT NOT NULL, "
                + "max_auto_bid BIGINT NOT NULL, "
                + "bid_step BIGINT NOT NULL DEFAULT 100000, "
                + "register_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                + "PRIMARY KEY (auction_id, bidder_id), "
                + "FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE, "
                + "FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE"
                + ") ENGINE=InnoDB");

        stmt.execute("CREATE TABLE IF NOT EXISTS wallet_transactions ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "user_id INT NOT NULL, "
                + "transaction_type VARCHAR(50) NOT NULL, "
                + "amount BIGINT NOT NULL, "
                + "description VARCHAR(255), "
                + "transaction_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE"
                + ") ENGINE=InnoDB");
    }
}
