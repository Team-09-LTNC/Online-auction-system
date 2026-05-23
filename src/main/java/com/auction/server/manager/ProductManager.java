package com.auction.server.manager;

import com.auction.common.model.item.*;
import com.auction.server.db.DatabaseConnection;
import com.auction.server.dao.ItemDao;
import com.auction.server.dao.AuctionDao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Điều phối các nghiệp vụ liên quan đến sản phẩm
 * Khởi tạo đối tượng thông qua Factory Pattern
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
                if (instance == null) instance = new ProductManager();
            }
        }
        return instance;
    }

    /**
     * Factory Method: Sinh ra đúng loại đối tượng con dựa trên phân loại
     */
    public Item taoSanPham(String loai, ItemAttributes thuocTinh) {
        if (thuocTinh == null) return null;

        ItemFactory factory = switch (loai.toUpperCase()) {
            case "ELECTRONICS" -> new ElectronicsFactory();
            case "ART" -> new ArtFactory();
            case "VEHICLE" -> new VehicleFactory();
            case "OTHER" -> new OtherItemFactory();
            default -> null;
        };

        if (factory == null) return null;
        return factory.createItem(thuocTinh);
    }

    /**
     * Kiểm tra tính hợp lệ trước khi cho phép Seller đăng bán
     * NẾU THÀNH CÔNG -> Tự động sinh ra phiên đấu giá và NẠP VÀO LỊCH TRÌNH
     */
    public boolean dangBanSanPham(Item sanPham, LocalDateTime startTime, LocalDateTime endTime) {
        return dangBanSanPham(sanPham, startTime, endTime, null);
    }

    public boolean dangBanSanPham(
            Item sanPham,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Long buyNowPrice
    ) {
        if (sanPham == null || sanPham.getStartingPrice() <= 0) return false;

        // Lưu sản phẩm xuống DB
        int itemId = itemDao.luuSanPham(sanPham);
        if (itemId > 0) {
            sanPham.setId(itemId);

            //Tạo phiên đấu giá
            boolean isAuctionCreated = auctionDao.taoPhienDauGia(
                    itemId,
                    sanPham.getStartingPrice(),
                    startTime,
                    endTime,
                    buyNowPrice
            );

            if (isAuctionCreated) {
                // Báo cho AuctionManager biết có phiên mới để lập lịch đếm ngược!
                List<com.auction.common.model.bid.Auction> dsChoMo = auctionDao.layDanhSachPhienChoMo();
                for (com.auction.common.model.bid.Auction a : dsChoMo) {
                    if (a.getItem().getId() == itemId) {
                        AuctionManager.getInstance().henGioMoPhien(a);
                        break;
                    }
                }
                return true;
            } else {
                itemDao.xoaSanPham(itemId);
                return false;
            }
        }
        return false;
    }

    /**
     * Lấy toàn bộ sản phẩm để hiển thị lên bảng cho người dùng xem
     */
    public List<Item> layTatCaSanPham() {
        return itemDao.layTatCaSanPham();
    }
    /**
     * Lấy sản phẩm theo sellerID
     */
    public List<Item> laySanPhamTheoSellerId(int sellerId) {
        return itemDao.laySanPhamTheoSellerId(sellerId);
    }
    /**
     * Gỡ sản phẩm khỏi hệ thống
     */
    public boolean xoaSanPham(int idSanPham) {
        return itemDao.xoaSanPham(idSanPham);
    }

    /**
     * Truy xuất thông tin chi tiết một món hàng
     */
    public Item laySanPhamTheoId(int idSanPham) {
        return itemDao.laySanPhamTheoId(idSanPham);
    }

    /**
     * Tìm kiếm sản phẩm theo từ khóa (gọi xuống ItemDao)
     */
    public List<Item> timSanPhamTheoTukhoa(String tuKhoa) {
        return itemDao.timSanPhamTheoTukhoa(tuKhoa);
    }

    /**
     * Cập nhật thông tin sản phẩm đã có trong hệ thống
     */
    public boolean capNhatSanPham(Item sanPham) {
        if (sanPham == null || sanPham.getId() <= 0) return false;
        return itemDao.updateSanPham(sanPham);
    }

    /**
     * Chỉ cho phép cập nhật khi phiên của sản phẩm còn ở OPEN và chưa đến giờ mở.
     */
    public boolean capNhatSanPhamDangChoMo(
            Item sanPham,
            int sellerId,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        if (sanPham == null || sanPham.getId() <= 0 || sellerId <= 0 || sanPham.getStartingPrice() <= 0) {
            return false;
        }
        if (startTime == null || endTime == null || startTime.isBefore(LocalDateTime.now()) || !endTime.isAfter(startTime)) {
            return false;
        }

        String sql = "UPDATE items i "
                + "JOIN auctions a ON a.item_id = i.id "
                + "SET i.name = ?, i.description = ?, i.starting_price = ?, "
                + "i.category = ?, i.image_url = ?, a.current_price = ?, "
                + "a.start_time = ?, a.end_time = ? "
                + "WHERE i.id = ? AND i.seller_id = ? "
                + "AND a.status = 'OPEN' AND a.start_time > NOW() "
                + "AND NOT EXISTS (SELECT 1 FROM bid_history b WHERE b.auction_id = a.id)";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sanPham.getName());
            pstmt.setString(2, sanPham.getDescription());
            pstmt.setLong(3, sanPham.getStartingPrice());
            pstmt.setString(4, sanPham.getCategory());
            pstmt.setString(5, sanPham.getImageUrl());
            pstmt.setLong(6, sanPham.getStartingPrice());
            pstmt.setTimestamp(7, java.sql.Timestamp.valueOf(startTime));
            pstmt.setTimestamp(8, java.sql.Timestamp.valueOf(endTime));
            pstmt.setInt(9, sanPham.getId());
            pstmt.setInt(10, sellerId);
            boolean updated = pstmt.executeUpdate() > 0;
            if (updated) {
                com.auction.common.model.bid.Auction auction = auctionDao.layPhienTheoItemId(sanPham.getId());
                if (auction != null) {
                    AuctionManager.getInstance().henGioMoPhien(auction);
                }
            }
            return updated;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Chỉ cho phép xóa đăng bán khi phiên vẫn đang chờ mở.
     */
    public boolean xoaSanPhamDangChoMo(int itemId, int sellerId) {
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
