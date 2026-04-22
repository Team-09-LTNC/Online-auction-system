-- File này để anh em trong nhóm copy ném vào MySQL chạy nhé!
CREATE DATABASE IF NOT EXISTS online_auction_system;

USE online_auction_system;

CREATE TABLE IF NOT EXISTS bid_transaction (
    id INT AUTO_INCREMENT PRIMARY KEY,
    auction_id VARCHAR(255) NOT NULL,
    bidder_id VARCHAR(255) NOT NULL,
    bid_amount DOUBLE NOT NULL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);