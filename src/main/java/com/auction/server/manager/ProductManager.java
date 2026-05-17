package com.auction.server.manager;

import com.auction.common.model.item.*;
import com.auction.server.dao.ItemDao;
import java.util.List;

/**
 * Điều phối các nghiệp vụ liên quan đến sản phẩm
 * Khởi tạo đối tượng thông qua Factory Pattern
 */
public class ProductManager {
    private static volatile ProductManager instance;
    private final ItemDao itemDao;

    private ProductManager() {
        this.itemDao = new ItemDao();
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
     */
    public boolean dangBanSanPham(Item sanPham) {
        if (sanPham == null || sanPham.getStartingPrice() <= 0) return false;
        return itemDao.luuSanPham(sanPham);
    }

    /**
     * Lấy toàn bộ sản phẩm để hiển thị lên bảng cho người dùng xem
     */
    public List<Item> layTatCaSanPham() {
        return itemDao.layTatCaSanPham();
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