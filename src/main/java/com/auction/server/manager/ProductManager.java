package com.auction.server.manager;

import com.auction.common.model.item.*;
import com.auction.server.dao.ItemDao;
import com.auction.server.dao.AuctionDao;
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
     * NẾU THÀNH CÔNG -> Tự động sinh ra phiên đấu giá (Auction)
     */
    public boolean dangBanSanPham(Item sanPham, LocalDateTime startTime, LocalDateTime endTime) {
        if (sanPham == null || sanPham.getStartingPrice() <= 0) return false;
        
        int itemId = itemDao.luuSanPham(sanPham);
        if (itemId > 0) {
            sanPham.setId(itemId);
            // Tiếp tục tạo phiên đấu giá cho sản phẩm vừa đăng
            return auctionDao.taoPhienDauGia(itemId, sanPham.getStartingPrice(), startTime, endTime);
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
}