package com.auction.server.network.handler;

import com.auction.common.dto.BaseDTOs;
import com.auction.common.dto.ItemDTOs;
import com.auction.common.enums.ActionType;
import com.auction.common.enums.ErrorCode;
import com.auction.common.enums.StatusCode;
import com.auction.common.model.item.Item;
import com.auction.common.model.item.ItemAttributes;
import com.auction.common.model.user.User;
import com.auction.server.manager.ProductManager;
import com.auction.server.network.ClientHandler;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.List;

/**
 * ProductController: Nhóm các chức năng quản lý sản phẩm. (nhóm PRODUCT trong ActionType)
 * Nhiệm vụ chính: Tiếp nhận các yêu cầu CRUD sản phẩm, kiểm tra quyền hạn của Seller
 * và điều phối logic thông qua ProductManager.
 */
public class ProductController implements RequestHandler {
    private final Gson gson = new Gson();

    @Override
    public String xuLy(JsonObject yeuCau, ClientHandler client) {
        String loaiYeuCau = yeuCau.get("type").getAsString();

        switch (loaiYeuCau) {
            case ActionType.CREATE_PRODUCT:
                return xuLyThemSanPham(yeuCau, client);
            case ActionType.GET_ALL_PRODUCTS:
                return xuLyLayTatCaSanPham();
            case ActionType.DELETE_PRODUCT:
                return xuLyXoaSanPham(yeuCau, client);
            case ActionType.SEARCH_PRODUCT:
                return xuLyTimKiemSanPham(yeuCau);
            case ActionType.GET_PRODUCT_BY_ID:
                return xuLyLaySanPhamTheoId(yeuCau);
            case ActionType.UPDATE_PRODUCT:
                return xuLyCapNhatSanPham(yeuCau, client);
            default:
                return null;
        }
    }

    /**
     * Xử lý yêu cầu đăng bán sản phẩm mới từ Seller.
     */
    private String xuLyThemSanPham(JsonObject yeuCau, ClientHandler client) {
        ItemDTOs.CreateItemRequest request = gson.fromJson(yeuCau, ItemDTOs.CreateItemRequest.class);
        User nguoiDung = client.layNguoiDungHienTai();

        // Kiểm tra quyền hạn: Chỉ SELLER mới được tạo sản phẩm
        if (nguoiDung == null || !"SELLER".equals(nguoiDung.getRoleName())) {
            return gson.toJson(new BaseDTOs.ErrorResponse(
                    StatusCode.FORBIDDEN,
                    "Chỉ Người bán (Seller) mới có quyền đăng sản phẩm!",
                    ErrorCode.UNAUTHORIZED));
        }

        // Chuyển đổi DTO sang ItemAttributes để dùng cho Factory Pattern
        ItemAttributes thuocTinh = new ItemAttributes();
        thuocTinh.setName(request.getName());
        thuocTinh.setDescription(request.getDescription());
        thuocTinh.setStartingPrice(request.getStartingPrice());

        // Sử dụng ProductManager để tạo đúng loại đối tượng Item (Electronics, Art, v.v.)
        Item sanPhamMoi = ProductManager.getInstance().taoSanPham(request.getCategory(), thuocTinh);
        if (sanPhamMoi == null) {
            return gson.toJson(new BaseDTOs.ErrorResponse(
                    StatusCode.BAD_REQUEST,
                    "Loại sản phẩm không hợp lệ!",
                    ErrorCode.ITEM_NOT_FOUND));
        }

        sanPhamMoi.setSellerId(nguoiDung.getId());

        // Lưu sản phẩm vào cơ sở dữ liệu
        boolean thanhCong = ProductManager.getInstance().dangBanSanPham(sanPhamMoi);

        if (thanhCong) {
            return gson.toJson(new ItemDTOs.CreateItemResponse(true, "Đăng sản phẩm thành công!", sanPhamMoi.getId()));
        } else {
            return gson.toJson(new BaseDTOs.ErrorResponse(
                    StatusCode.SERVER_ERROR,
                    "Lỗi hệ thống khi lưu sản phẩm.",
                    ErrorCode.INTERNAL_SERVER_ERROR));
        }
    }

    /**
     * Lấy danh sách toàn bộ sản phẩm hiện có trong hệ thống.
     */
    private String xuLyLayTatCaSanPham() {
        List<Item> danhSach = ProductManager.getInstance().layTatCaSanPham();
        // Bạn có thể tạo thêm một ItemListResponse DTO nếu muốn chuẩn hóa hơn
        JsonObject phanHoi = new JsonObject();
        phanHoi.addProperty("type", ActionType.GET_ALL_PRODUCTS);
        phanHoi.addProperty("success", true);
        phanHoi.add("data", gson.toJsonTree(danhSach));
        return gson.toJson(phanHoi);
    }

    /**
     * Xử lý yêu cầu xóa sản phẩm (Yêu cầu id sản phẩm từ Client).
     */
    private String xuLyXoaSanPham(JsonObject yeuCau, ClientHandler client) {
        int idSanPham = yeuCau.get("itemId").getAsInt();
        User nguoiDung = client.layNguoiDungHienTai();

        // Kiểm tra quyền (Thường chỉ Admin hoặc chính Seller đó mới được xóa)
        if (nguoiDung == null) {
            return gson.toJson(new BaseDTOs.ErrorResponse(StatusCode.UNAUTHORIZED, "Vui lòng đăng nhập!", ErrorCode.UNAUTHORIZED));
        }

        boolean thanhCong = ProductManager.getInstance().xoaSanPham(idSanPham);
        if (thanhCong) {
            return gson.toJson(new BaseDTOs.Response(ActionType.DELETE_PRODUCT, StatusCode.OK, true, "Xóa sản phẩm thành công!") {});
        } else {
            return gson.toJson(new BaseDTOs.ErrorResponse(StatusCode.NOT_FOUND, "Không tìm thấy sản phẩm để xóa.", ErrorCode.ITEM_NOT_FOUND));
        }
    }

    /**
     * Tìm kiếm sản phẩm theo từ khóa.
     */
    private String xuLyTimKiemSanPham(JsonObject yeuCau) {
        String tuKhoa = yeuCau.get("keyword").getAsString();

        // Gọi qua ProductManager để lấy danh sách từ Database
        List<Item> ketQuaTimKiem = ProductManager.getInstance().timSanPhamTheoTukhoa(tuKhoa);

        JsonObject phanHoi = new JsonObject();
        phanHoi.addProperty("type", ActionType.SEARCH_PRODUCT);
        phanHoi.addProperty("success", true);
        phanHoi.addProperty("message", "Tìm thấy " + ketQuaTimKiem.size() + " sản phẩm.");

        // Đẩy danh sách kết quả vào Json để gửi về Client
        phanHoi.add("data", gson.toJsonTree(ketQuaTimKiem));

        return gson.toJson(phanHoi);
    }

    /**
     *  Lâấy sản phẩm theo ID
     */
    private String xuLyLaySanPhamTheoId(JsonObject yeuCau) {
        try {
            // 1. Trích xuất ID sản phẩm từ yêu cầu của Client
            int idSanPham = yeuCau.get("itemId").getAsInt();

            // 2. Gọi ProductManager để lấy dữ liệu từ Database
            Item sanPham = ProductManager.getInstance().laySanPhamTheoId(idSanPham);

            // 3. Kiểm tra kết quả và trả về phản hồi tương ứng
            if (sanPham != null) {
                JsonObject phanHoi = new JsonObject();
                phanHoi.addProperty("type", ActionType.GET_PRODUCT_BY_ID);
                phanHoi.addProperty("success", true);
                phanHoi.addProperty("message", "Lấy thông tin sản phẩm thành công.");
                phanHoi.add("data", gson.toJsonTree(sanPham));
                return gson.toJson(phanHoi);
            } else {
                // Trả về lỗi nếu không tìm thấy sản phẩm trong hệ thống
                return gson.toJson(new BaseDTOs.ErrorResponse(
                        StatusCode.NOT_FOUND,
                        "Không tìm thấy sản phẩm với ID: " + idSanPham,
                        ErrorCode.ITEM_NOT_FOUND));
            }
        } catch (Exception e) {
            // Xử lý các lỗi ngoại lệ phát sinh (ví dụ: sai định dạng ID)
            return gson.toJson(new BaseDTOs.ErrorResponse(
                    StatusCode.SERVER_ERROR,
                    "Lỗi hệ thống khi truy xuất sản phẩm.",
                    ErrorCode.INTERNAL_SERVER_ERROR));
        }
    }

    /**
     * Xử lý yêu cầu cập nhật thông tin sản phẩm từ Seller.
     * Đảm bảo tính bảo mật: Chỉ chủ sở hữu mới được phép chỉnh sửa.
     */
    private String xuLyCapNhatSanPham(JsonObject yeuCau, ClientHandler client) {
        try {
            User nguoiDung = client.layNguoiDungHienTai();

            // 1. Kiểm tra xác thực (Chỉ Seller mới được dùng chức năng này)
            if (nguoiDung == null || !"SELLER".equals(nguoiDung.getRoleName())) {
                return gson.toJson(new BaseDTOs.ErrorResponse(
                        StatusCode.FORBIDDEN,
                        "Chỉ Người bán (Seller) mới có quyền cập nhật sản phẩm!",
                        ErrorCode.UNAUTHORIZED));
            }

            // 2. Lấy ID sản phẩm cần sửa
            int idSanPham = yeuCau.get("itemId").getAsInt();

            // 3. Lấy sản phẩm hiện tại từ Database lên để kiểm tra
            Item sanPhamHienTai = ProductManager.getInstance().laySanPhamTheoId(idSanPham);
            if (sanPhamHienTai == null) {
                return gson.toJson(new BaseDTOs.ErrorResponse(
                        StatusCode.NOT_FOUND,
                        "Không tìm thấy sản phẩm cần sửa!",
                        ErrorCode.ITEM_NOT_FOUND));
            }

            // KIỂM TRA BẢO MẬT: Seller đang đăng nhập có phải là người tạo ra sản phẩm này không?
            if (sanPhamHienTai.getSellerId() != nguoiDung.getId()) {
                return gson.toJson(new BaseDTOs.ErrorResponse(
                        StatusCode.FORBIDDEN,
                        "Bạn không có quyền chỉnh sửa sản phẩm của người khác!",
                        ErrorCode.FORBIDDEN));
            }

            // 4. Cập nhật các trường dữ liệu (Kiểm tra nếu Client có gửi trường đó lên thì mới sửa)
            if (yeuCau.has("name")) sanPhamHienTai.setName(yeuCau.get("name").getAsString());
            if (yeuCau.has("description")) sanPhamHienTai.setDescription(yeuCau.get("description").getAsString());
            if (yeuCau.has("startingPrice")) sanPhamHienTai.setStartingPrice(yeuCau.get("startingPrice").getAsLong());
            if (yeuCau.has("category")) sanPhamHienTai.setCategory(yeuCau.get("category").getAsString());
            if (yeuCau.has("imageUrl")) sanPhamHienTai.setImageUrl(yeuCau.get("imageUrl").getAsString());

            // 5. Lưu sự thay đổi xuống Database
            boolean thanhCong = ProductManager.getInstance().capNhatSanPham(sanPhamHienTai);

            if (thanhCong) {
                return gson.toJson(new BaseDTOs.Response(
                        ActionType.UPDATE_PRODUCT, StatusCode.OK, true, "Cập nhật sản phẩm thành công!") {});
            } else {
                return gson.toJson(new BaseDTOs.ErrorResponse(
                        StatusCode.SERVER_ERROR, "Lỗi khi lưu dữ liệu vào CSDL.", ErrorCode.INTERNAL_SERVER_ERROR));
            }
        } catch (Exception e) {
            return gson.toJson(new BaseDTOs.ErrorResponse(
                    StatusCode.BAD_REQUEST, "Lỗi định dạng dữ liệu gửi lên.", ErrorCode.BAD_REQUEST));
        }
    }
}
