package com.auction.server.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Script khoi tao database mot lan voi schema moi nhat cua he thong.
 */
public class SetupDatabase {
    public static void main(String[] args) {
        System.out.println(">>> BAT DAU QUY TRINH THIET LAP DATABASE...");

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {

            if (isDatabaseInitialized(conn)) {
                System.out.println(">>> Database da co bang he thong. Bo qua setup de giu nguyen du lieu hien tai.");
                return;
            }

            createLatestSchema(stmt);
            seedInitialData(stmt);

            System.out.println();
            System.out.println("==================================================");
            System.out.println(">>> THIET LAP DATABASE THANH CONG!");
            System.out.println("==================================================");
        } catch (Exception e) {
            System.err.println();
            System.err.println(">>> [LOI] Khong the thiet lap database:");
            e.printStackTrace();
        }
    }

    private static boolean isDatabaseInitialized(Connection conn) throws Exception {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getTables(null, null, "users", null)) {
            return rs.next();
        }
    }

    private static void createLatestSchema(Statement stmt) throws Exception {
        System.out.println(">>> Dang tao schema moi nhat neu chua ton tai...");

        stmt.execute("CREATE TABLE IF NOT EXISTS users ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "username VARCHAR(50) NOT NULL UNIQUE, "
                + "password VARCHAR(255) NOT NULL, "
                + "full_name VARCHAR(100) NOT NULL, "
                + "role ENUM('ADMIN', 'SELLER', 'BIDDER') NOT NULL, "
                + "balance BIGINT DEFAULT 0, "
                + "status VARCHAR(20) DEFAULT 'ACTIVE', "
                + "lock_until DATETIME NULL, "
                + "KEY idx_user_status (status)"
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
                + "image_thumb_url VARCHAR(500), "
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
                + "status ENUM('OPEN', 'RUNNING', 'FINISHED', 'PAID', 'CANCELED') DEFAULT 'OPEN', "
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

        stmt.execute("CREATE TABLE IF NOT EXISTS follows ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "user_id INT NOT NULL, "
                + "auction_id INT NOT NULL, "
                + "UNIQUE KEY unique_follow (user_id, auction_id), "
                + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, "
                + "FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE"
                + ") ENGINE=InnoDB");

        stmt.execute("CREATE TABLE IF NOT EXISTS chat_messages ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "auction_id INT NULL, "
                + "sender_id INT NOT NULL, "
                + "recipient_id INT NULL, "
                + "message TEXT NOT NULL, "
                + "payment_required BOOLEAN NOT NULL DEFAULT FALSE, "
                + "is_read BOOLEAN NOT NULL DEFAULT FALSE, "
                + "send_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "sent_at_local VARCHAR(19) NULL, "
                + "FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE, "
                + "FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE, "
                + "FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE, "
                + "KEY idx_chat_recipient_time (recipient_id, send_time)"
                + ") ENGINE=InnoDB");

        stmt.execute("CREATE TABLE IF NOT EXISTS wallet_transactions ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "user_id INT NOT NULL, "
                + "transaction_type ENUM('DEPOSIT', 'WITHDRAW', 'PAYMENT_SENT', 'PAYMENT_RECEIVED') NOT NULL, "
                + "amount BIGINT NOT NULL, "
                + "description VARCHAR(255), "
                + "transaction_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE"
                + ") ENGINE=InnoDB");

        stmt.execute("CREATE TABLE IF NOT EXISTS auto_bid_settings ("
                + "auction_id INT NOT NULL, "
                + "bidder_id INT NOT NULL, "
                + "max_auto_bid BIGINT NOT NULL, "
                + "bid_step BIGINT NOT NULL DEFAULT 0, "
                + "register_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                + "PRIMARY KEY (auction_id, bidder_id), "
                + "FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE, "
                + "FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE"
                + ") ENGINE=InnoDB");
        stmt.execute("ALTER TABLE auto_bid_settings ADD COLUMN IF NOT EXISTS bid_step BIGINT NOT NULL DEFAULT 0");

        stmt.execute("CREATE TABLE IF NOT EXISTS bidder_penalties ("
                + "bidder_id INT PRIMARY KEY, "
                + "violation_count INT NOT NULL DEFAULT 0, "
                + "lock_until DATETIME NULL, "
                + "last_reason VARCHAR(255) NULL, "
                + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE"
                + ") ENGINE=InnoDB");
    }

    private static void seedInitialData(Statement stmt) throws Exception {
        System.out.println(">>> Dang nap du lieu mau ban dau...");

        stmt.execute("INSERT IGNORE INTO users "
                + "(id, username, password, full_name, role, balance, status) VALUES "
                + "(1, 'admin', 'admin', 'Quan tri vien', 'ADMIN', 0, 'ACTIVE'), "
                + "(2, 'seller1', '123456', 'Nguoi Ban So 1', 'SELLER', 5000000, 'ACTIVE'), "
                + "(3, 'bidder1', '123456', 'Nguoi Mua So 1', 'BIDDER', 150000000, 'ACTIVE'), "
                + "(4, 'bidder2', '123456', 'Nguoi Mua So 2', 'BIDDER', 200000000, 'ACTIVE')");

        stmt.execute("INSERT IGNORE INTO items "
                + "(id, seller_id, name, description, category, starting_price, bid_increment, image_url) VALUES "
                + "(1, 2, 'Mercedes-Benz S450 2023', 'Xe sang luot 5000km, mau den noi that kem.', "
                + "'VEHICLE', 3000000000, 10000000, "
                + "'https://images.unsplash.com/photo-1618843479313-40f8afb4b4d8?w=800'), "
                + "(2, 2, 'Porsche 911 GT3 RS', 'Sieu xe the thao nhap khau nguyen chiec tu Duc.', "
                + "'VEHICLE', 8500000000, 50000000, "
                + "'https://images.unsplash.com/photo-1503376713295-8bc2584400f9?w=800'), "
                + "(3, 2, 'BMW S1000RR 2024', 'Ca map sieu phan khoi, ODO 1000km.', "
                + "'VEHICLE', 750000000, 5000000, "
                + "'https://images.unsplash.com/photo-1558981403-c5f9899a28bc?w=800'), "
                + "(4, 2, 'MacBook Pro M3 Max 16inch', 'Ban max option 128GB RAM, 4TB SSD. Likenew 99%.', "
                + "'ELECTRONICS', 120000000, 1000000, "
                + "'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800'), "
                + "(5, 2, 'Sony A7 IV Camera & Lens', "
                + "'May anh mirrorless chuyen nghiep kem ong kinh 24-70mm f/2.8.', "
                + "'ELECTRONICS', 65000000, 500000, "
                + "'https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=800'), "
                + "(6, 2, 'iPhone 15 Pro Max 1TB', "
                + "'Mau Titan Tu Nhien, pin 100%, bao hanh Apple Care+ 2025.', "
                + "'ELECTRONICS', 35000000, 500000, "
                + "'https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=800'), "
                + "(7, 2, 'Tranh Son Dau: Dem Day Sao', "
                + "'Ban sao chep cao cap, kich thuoc 100x150cm, co khung go soi.', "
                + "'ART', 15000000, 500000, "
                + "'https://images.unsplash.com/photo-1579783902614-a3fb3927b6a5?w=800'), "
                + "(8, 2, 'Tuong Thach Cao Co Dien', "
                + "'Tuong dieu khac lay cam hung tu thoi ky Phuc Hung.', "
                + "'ART', 25000000, 1000000, "
                + "'https://images.unsplash.com/photo-1544531586-fde5298cdd40?w=800'), "
                + "(9, 2, 'Binh Gom Co Men Ran', "
                + "'Do co the ky 19, bao quan hoan hao khong ti vet.', "
                + "'ART', 150000000, 5000000, "
                + "'https://images.unsplash.com/photo-1610701596007-11502861dcfa?w=800')");

        stmt.execute("INSERT IGNORE INTO auctions "
                + "(id, item_id, current_price, buy_now_price, status, start_time, end_time) VALUES "
                + "(1, 1, 3000000000, 3500000000, 'RUNNING', "
                + "DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 2 HOUR)), "
                + "(4, 4, 125000000, 150000000, 'RUNNING', "
                + "DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 5 HOUR)), "
                + "(7, 7, 18000000, NULL, 'RUNNING', "
                + "DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_ADD(NOW(), INTERVAL 12 HOUR)), "
                + "(2, 2, 8500000000, 9500000000, 'OPEN', "
                + "DATE_ADD(NOW(), INTERVAL 30 MINUTE), DATE_ADD(NOW(), INTERVAL 24 HOUR)), "
                + "(5, 5, 65000000, NULL, 'OPEN', "
                + "DATE_ADD(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 48 HOUR)), "
                + "(8, 8, 25000000, 35000000, 'OPEN', "
                + "DATE_ADD(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 72 HOUR)), "
                + "(3, 3, 820000000, 900000000, 'FINISHED', "
                + "DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)), "
                + "(6, 6, 38500000, 45000000, 'FINISHED', "
                + "DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)), "
                + "(9, 9, 210000000, NULL, 'FINISHED', "
                + "DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY))");
    }
}
