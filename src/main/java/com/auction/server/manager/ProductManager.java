package com.auction.server.manager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import com.auction.common.model.item.ArtFactory;
import com.auction.common.model.item.ElectronicsFactory;
import com.auction.common.model.item.Item;
import com.auction.common.model.item.ItemAttributes;
import com.auction.common.model.item.ItemFactory;
import com.auction.common.model.item.OtherItemFactory;
import com.auction.common.model.item.VehicleFactory;
import com.auction.server.dao.AuctionDao;
import com.auction.server.dao.ItemDao;
import com.auction.server.db.DatabaseConnection;

/**
 * Điều phối các nghiệp vụ liên quan đến sản phẩm
 * Khởi tạo đối tượng thông qua mẫu Factory
 */
public class ProductManager {
    private static volatile ProductManager instance;
    private final ItemDao itemDao;
    private final AuctionDao auctionDao;

    private ProductManager() {
        this.itemDao = new ItemDao();
        this.auctionDao = new AuctionDao();
    }

    public static ProductManager getInstance() {
        if (instance == null) {
            synchronized (ProductManager.class) {
                if (instance == null) {
                    instance = new ProductManager();
                }
            }
        }
        return instance;
    }

    /**
     * Phương thức Factory: Sinh ra đúng loại đối tượng con dựa trên phân loại
     */
    public Item createProduct(String loai, ItemAttributes thuocTinh) {
        if (thuocTinh == null) {
            return null;
        }

        ItemFactory factory = switch (loai.toUpperCase()) {
            case "ELECTRONICS" -> new ElectronicsFactory();
            case "ART" -> new ArtFactory();
            case "VEHICLE" -> new VehicleFactory();
            case "OTHER" -> new OtherItemFactory();
            default -> null;
        };

        if (factory == null) {
            return null;
        }
        return factory.createItem(thuocTinh);
    }

    /**
     * Kiểm tra tính hợp lệ trước khi cho phép người bán đăng bán
     */
    public boolean listProductForAuction(Item sanPham, LocalDateTime startTime, LocalDateTime endTime) {
        return listProductForAuction(sanPham, startTime, endTime, null);
    }

    public boolean listProductForAuction(
            Item sanPham,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Long buyNowPrice) {
        return listProductForAuction(sanPham, startTime, endTime, buyNowPrice, false);
    }

    public boolean listProductForAuction(
            Item sanPham,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Long buyNowPrice,
            boolean antiSnipingEnabled) {
        if (sanPham == null || sanPham.getStartingPrice() <= 0) {
            return false;
        }

        // Lưu sản phẩm xuống DB
        int itemId = itemDao.saveProduct(sanPham);
        if (itemId > 0) {
            sanPham.setId(itemId);

            // Tạo phiên đấu giá
            boolean isAuctionCreated = auctionDao.createAuctionSession(
                    itemId,
                    sanPham.getStartingPrice(),
                    startTime,
                    endTime,
                    buyNowPrice,
                    antiSnipingEnabled);

            if (isAuctionCreated) {
                // Báo cho AuctionManager biết có phiên mới để lập lịch đếm ngược!
                List<com.auction.common.model.bid.Auction> dsChoMo = auctionDao.getPendingAuctionSessions();
                for (com.auction.common.model.bid.Auction a : dsChoMo) {
                    if (a.getItem().getId() == itemId) {
                        AuctionManager.getInstance().scheduleAuctionStart(a);
                        break;
                    }
                }
                return true;
            } else {
                itemDao.deleteProduct(itemId);
                return false;
            }
        }
        return false;
    }

    /**
     * Lấy toàn bộ sản phẩm để hiển thị lên bảng cho người dùng xem
     */
    public List<Item> getAllProducts() {
        return itemDao.getAllProducts();
    }

    /**
     * Lấy sản phẩm theo ID người bán
     */
    public List<Item> getProductsBySellerId(int sellerId) {
        return itemDao.getProductsBySellerId(sellerId);
    }

    /**
     * Gỡ sản phẩm khỏi hệ thống
     */
    public boolean deleteProduct(int idSanPham) {
        return itemDao.deleteProduct(idSanPham);
    }

    /**
     * Truy xuất thông tin chi tiết một món hàng
     */
    public Item getProductById(int idSanPham) {
        return itemDao.getProductById(idSanPham);
    }

    /**
     * Tìm kiếm sản phẩm theo từ khóa (gọi xuống ItemDao)
     */
    public List<Item> searchProductsByKeyword(String tuKhoa) {
        return itemDao.searchProductsByKeyword(tuKhoa);
    }

    /**
     * Cập nhật thông tin sản phẩm đã có trong hệ thống
     */
    public boolean updateProduct(Item sanPham) {
        if (sanPham == null || sanPham.getId() <= 0) {
            return false;
        }
        return itemDao.updateProduct(sanPham);
    }

    /**
     * Chỉ cho phép cập nhật khi phiên của sản phẩm còn ở OPEN và chưa có giá đặt.
     */
    public boolean updatePendingProduct(
            Item sanPham,
            int sellerId,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        if (sanPham == null || sanPham.getId() <= 0 || sellerId <= 0 || sanPham.getStartingPrice() <= 0) {
            return false;
        }
        if (startTime == null || endTime == null || !startTime.isAfter(LocalDateTime.now())
                || !endTime.isAfter(startTime)) {
            return false;
        }

        String sqlKiemTra = "SELECT a.id "
                + "FROM items i "
                + "JOIN auctions a ON a.item_id = i.id "
                + "WHERE i.id = ? AND i.seller_id = ? "
                + "AND a.status = 'OPEN' "
                + "AND NOT EXISTS (SELECT 1 FROM bid_history b WHERE b.auction_id = a.id)";

        String sqlCapNhatSanPham = "UPDATE items "
                + "SET name = ?, description = ?, starting_price = ?, category = ?, image_url = ? "
                + "WHERE id = ? AND seller_id = ?";

        String sqlCapNhatThoiGian = "UPDATE auctions "
                + "SET current_price = ?, start_time = ?, end_time = ? "
                + "WHERE id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            int auctionId;
            try (PreparedStatement pstmt = conn.prepareStatement(sqlKiemTra)) {
                pstmt.setInt(1, sanPham.getId());
                pstmt.setInt(2, sellerId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                    auctionId = rs.getInt("id");
                }
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sqlCapNhatSanPham)) {
                pstmt.setString(1, sanPham.getName());
                pstmt.setString(2, sanPham.getDescription());
                pstmt.setLong(3, sanPham.getStartingPrice());
                pstmt.setString(4, sanPham.getCategory());
                pstmt.setString(5, sanPham.getImageUrl());
                pstmt.setInt(6, sanPham.getId());
                pstmt.setInt(7, sellerId);
                pstmt.executeUpdate();
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sqlCapNhatThoiGian)) {
                pstmt.setLong(1, sanPham.getStartingPrice());
                pstmt.setTimestamp(2, java.sql.Timestamp.valueOf(startTime));
                pstmt.setTimestamp(3, java.sql.Timestamp.valueOf(endTime));
                pstmt.setInt(4, auctionId);
                pstmt.executeUpdate();
            }

            conn.commit();

            com.auction.common.model.bid.Auction auction = auctionDao.getAuctionByItemId(sanPham.getId());
            if (auction != null) {
                AuctionManager.getInstance().scheduleAuctionStart(auction);
            }
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    /**
     * Chỉ cho phép xóa đăng bán khi phiên vẫn đang chờ mở.
     */
    public boolean deletePendingProduct(int itemId, int sellerId) {
        if (itemId <= 0 || sellerId <= 0) {
            return false;
        }

        String sql = "DELETE i FROM items i "
                + "JOIN auctions a ON a.item_id = i.id "
                + "WHERE i.id = ? AND i.seller_id = ? "
                + "AND a.status = 'OPEN' AND a.start_time > NOW() "
                + "AND NOT EXISTS (SELECT 1 FROM bid_history b WHERE b.auction_id = a.id)";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, itemId);
            pstmt.setInt(2, sellerId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }
}
