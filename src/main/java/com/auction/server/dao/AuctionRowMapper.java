package com.auction.server.dao;

import com.auction.common.enums.AuctionStatus;
import com.auction.common.model.bid.Auction;
import com.auction.common.model.item.Art;
import com.auction.common.model.item.Electronics;
import com.auction.common.model.item.Item;
import com.auction.common.model.item.OtherItem;
import com.auction.common.model.item.Vehicle;
import org.slf4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Locale;

final class AuctionRowMapper {

    private AuctionRowMapper() {
    }

    static Auction mapResultSetToAuction(ResultSet rs, Logger logger) throws SQLException {
        String rawCategory = rs.getString("category");
        String loai = rawCategory == null || rawCategory.trim().isEmpty()
                ? "OTHER"
                : rawCategory.trim().toUpperCase(Locale.ROOT);
        Item item = taoItemTheoLoai(loai);

        item.setId(rs.getInt("item_id"));
        item.setName(rs.getString("name"));
        try {
            item.setDescription(rs.getString("description"));
        } catch (Exception ignored) {
        }
        item.setStartingPrice(rs.getLong("starting_price"));
        item.setBidIncrement(rs.getLong("bid_increment"));
        item.setSellerId(rs.getInt("seller_id"));
        item.setCategory(loai);
        try {
            item.setImageUrl(rs.getString("image_url"));
        } catch (Exception ignored) {
        }

        Auction phien = new Auction(item);
        phien.setId(rs.getInt("id"));
        phien.setStatus(parseAuctionStatus(rs.getString("status"), logger));
        phien.setStartTime(rs.getObject("start_time", LocalDateTime.class));
        phien.setEndTime(rs.getObject("end_time", LocalDateTime.class));
        phien.setCurrentPrice(rs.getLong("current_price"));

        long buyNowPrice = rs.getLong("buy_now_price");
        if (!rs.wasNull()) {
            phien.setBuyNowPrice(buyNowPrice);
        }
        phien.setAntiSnipingEnabled(rs.getBoolean("anti_sniping_enabled"));
        return phien;
    }

    private static Item taoItemTheoLoai(String loai) {
        return switch (loai) {
            case "ELECTRONICS" -> new Electronics();
            case "ART" -> new Art();
            case "VEHICLE" -> new Vehicle();
            default -> new OtherItem();
        };
    }

    private static AuctionStatus parseAuctionStatus(String rawStatus, Logger logger) {
        if (rawStatus == null || rawStatus.trim().isEmpty()) {
            return AuctionStatus.OPEN;
        }
        String normalizedStatus = rawStatus.trim().toUpperCase(Locale.ROOT);
        if ("CANCELLED".equals(normalizedStatus) || "REJECTED".equals(normalizedStatus)) {
            return AuctionStatus.CANCELED;
        }
        if ("PENDING".equals(normalizedStatus)) {
            return AuctionStatus.OPEN;
        }
        try {
            return AuctionStatus.valueOf(normalizedStatus);
        } catch (IllegalArgumentException e) {
            logger.warn("Trạng thái phiên đấu giá không hợp lệ trong DB: '{}'. Dùng OPEN.", rawStatus);
            return AuctionStatus.OPEN;
        }
    }
}
