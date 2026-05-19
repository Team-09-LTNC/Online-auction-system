package com.auction.server.networkserver.handler;

import com.auction.common.dto.BaseDTOs;
import com.auction.common.dto.ItemDTOs;
import com.auction.common.enums.ActionType;
import com.auction.common.enums.ErrorCode;
import com.auction.common.enums.StatusCode;
import com.auction.common.model.item.Item;
import com.auction.common.model.item.ItemAttributes;
import com.auction.common.model.user.User;
import com.auction.server.manager.ProductManager;
import com.auction.server.networkserver.ClientHandler;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ProductController implements RequestHandler {
    private final Gson gson = new Gson();
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ProductController.class);

    @Override
    public String xuLy(JsonObject yeuCau, ClientHandler client) {
        String loaiYeuCau = yeuCau.get("type").getAsString();

        switch (loaiYeuCau) {
            case ActionType.CREATE_PRODUCT:
                return xuLyThemSanPham(yeuCau, client);
            case ActionType.GET_ALL_PRODUCTS:
                return xuLyLayTatCaSanPham(yeuCau);
            case ActionType.DELETE_PRODUCT:
                return xuLyXoaSanPham(yeuCau, client);
            case ActionType.SEARCH_PRODUCT:
                return xuLyTimKiemSanPham(yeuCau);
            case ActionType.GET_PRODUCT_BY_ID:
                return xuLyLaySanPhamTheoId(yeuCau);
            case ActionType.UPDATE_PRODUCT:
                return xuLyCapNhatSanPham(yeuCau, client);
            case ActionType.GET_MY_PRODUCTS:
                return xuLyLaySanPhamCuaToi(yeuCau, client);
            default:
                return null;
        }
    }

    private String buildResponse(JsonObject request, String responseType, Object payloadDTO) {
        JsonObject response = gson.toJsonTree(payloadDTO).getAsJsonObject();
        response.addProperty("type", responseType);

        if (request != null && request.has("requestId")) {
            response.addProperty("requestId", request.get("requestId").getAsString());
        }
        return gson.toJson(response);
    }

    // --- CÁC HÀM XỬ LÝ NGHIỆP VỤ ---

    private String xuLyLaySanPhamCuaToi(JsonObject yeuCau, ClientHandler client) {
        // LUÔN lấy thông tin người dùng đang đăng nhập trên hệ thống Server
        User nguoiDung = client.layNguoiDungHienTai();
        
        if (nguoiDung == null) {
            logger.warn("xuLyLaySanPhamCuaToi: Người dùng chưa đăng nhập!");
            return buildResponse(yeuCau, ActionType.GET_MY_PRODUCTS, new BaseDTOs.ErrorResponse(StatusCode.UNAUTHORIZED, "Vui lòng đăng nhập!", ErrorCode.UNAUTHORIZED));
        }

        int sellerId = nguoiDung.getId();
        logger.info("xuLyLaySanPhamCuaToi: Lấy sản phẩm cho sellerId = {}", sellerId);
        List<Item> danhSach = ProductManager.getInstance().laySanPhamTheoSellerId(sellerId);
        logger.info("xuLyLaySanPhamCuaToi: Tìm thấy {} sản phẩm", danhSach.size());

        JsonObject dataPayload = new JsonObject();
        dataPayload.addProperty("success", true);
        dataPayload.add("data", gson.toJsonTree(danhSach));

        return buildResponse(yeuCau, ActionType.GET_MY_PRODUCTS, dataPayload);
    }

    private String xuLyThemSanPham(JsonObject yeuCau, ClientHandler client) {
        logger.info("Bắt đầu xử lý thêm sản phẩm mới");
        ItemDTOs.CreateItemRequest request = gson.fromJson(yeuCau, ItemDTOs.CreateItemRequest.class);
        User nguoiDung = client.layNguoiDungHienTai();

        if (nguoiDung == null || !"SELLER".equals(nguoiDung.getRoleName())) {
            logger.warn("xuLyThemSanPham: Thất bại - User null hoặc không phải SELLER. User: {}", nguoiDung != null ? nguoiDung.getUsername() : "null");
            return buildResponse(yeuCau, "CREATE_ITEM_RESPONSE", new BaseDTOs.ErrorResponse(StatusCode.FORBIDDEN, "Chỉ Seller mới có quyền đăng sản phẩm!", ErrorCode.UNAUTHORIZED));
        }

        ItemAttributes thuocTinh = new ItemAttributes();
        thuocTinh.setName(request.getName());
        thuocTinh.setDescription(request.getDescription());
        thuocTinh.setStartingPrice(request.getStartingPrice());

        Item sanPhamMoi = ProductManager.getInstance().taoSanPham(request.getCategory(), thuocTinh);
        if (sanPhamMoi == null) {
            logger.warn("xuLyThemSanPham: Thất bại - Không tạo được đối tượng Item từ Factory");
            return buildResponse(yeuCau, "CREATE_ITEM_RESPONSE", new BaseDTOs.ErrorResponse(StatusCode.BAD_REQUEST, "Loại sản phẩm không hợp lệ!", ErrorCode.ITEM_NOT_FOUND));
        }

        sanPhamMoi.setCategory(request.getCategory());
        sanPhamMoi.setSellerId(nguoiDung.getId());
        sanPhamMoi.setImageUrl(request.getImageUrl() != null ? request.getImageUrl() : "");

        LocalDateTime startTime = LocalDateTime.parse(request.getStartTime(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        LocalDateTime endTime = LocalDateTime.parse(request.getEndTime(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        boolean thanhCong = ProductManager.getInstance().dangBanSanPham(sanPhamMoi, startTime, endTime);

        if (thanhCong) {
            logger.info("xuLyThemSanPham: Thành công - Sản phẩm ID = {} đã được đăng bởi sellerId = {}", sanPhamMoi.getId(), nguoiDung.getId());
            return buildResponse(yeuCau, "CREATE_ITEM_RESPONSE", new ItemDTOs.CreateItemResponse(true, "Đăng sản phẩm thành công!", sanPhamMoi.getId()));
        } else {
            logger.error("xuLyThemSanPham: Thất bại - Lỗi khi lưu xuống DB");
            return buildResponse(yeuCau, "CREATE_ITEM_RESPONSE", new BaseDTOs.ErrorResponse(StatusCode.SERVER_ERROR, "Lỗi hệ thống khi lưu sản phẩm.", ErrorCode.INTERNAL_SERVER_ERROR));
        }
    }

    private String xuLyLayTatCaSanPham(JsonObject yeuCau) {
        List<Item> danhSach = ProductManager.getInstance().layTatCaSanPham();
        JsonObject dataPayload = new JsonObject();
        dataPayload.addProperty("success", true);
        dataPayload.add("data", gson.toJsonTree(danhSach));
        return buildResponse(yeuCau, ActionType.GET_ALL_PRODUCTS, dataPayload);
    }

    private String xuLyXoaSanPham(JsonObject yeuCau, ClientHandler client) {
        int idSanPham = yeuCau.get("itemId").getAsInt();
        if (client.layNguoiDungHienTai() == null) {
            return buildResponse(yeuCau, ActionType.DELETE_PRODUCT, new BaseDTOs.ErrorResponse(StatusCode.UNAUTHORIZED, "Vui lòng đăng nhập!", ErrorCode.UNAUTHORIZED));
        }

        if (ProductManager.getInstance().xoaSanPham(idSanPham)) {
            JsonObject successPayload = new JsonObject();
            successPayload.addProperty("statusCode", StatusCode.OK);
            successPayload.addProperty("success", true);
            successPayload.addProperty("message", "Xóa sản phẩm thành công!");
            return buildResponse(yeuCau, ActionType.DELETE_PRODUCT, successPayload);
        }
        return buildResponse(yeuCau, ActionType.DELETE_PRODUCT, new BaseDTOs.ErrorResponse(StatusCode.NOT_FOUND, "Không tìm thấy sản phẩm để xóa.", ErrorCode.ITEM_NOT_FOUND));
    }

    private String xuLyTimKiemSanPham(JsonObject yeuCau) {
        String tuKhoa = yeuCau.get("keyword").getAsString();
        List<Item> ketQua = ProductManager.getInstance().timSanPhamTheoTukhoa(tuKhoa);

        JsonObject dataPayload = new JsonObject();
        dataPayload.addProperty("success", true);
        dataPayload.addProperty("message", "Tìm thấy " + ketQua.size() + " sản phẩm.");
        dataPayload.add("data", gson.toJsonTree(ketQua));

        return buildResponse(yeuCau, ActionType.SEARCH_PRODUCT, dataPayload);
    }

    private String xuLyLaySanPhamTheoId(JsonObject yeuCau) {
        try {
            Item sanPham = ProductManager.getInstance().laySanPhamTheoId(yeuCau.get("itemId").getAsInt());
            if (sanPham != null) {
                JsonObject dataPayload = new JsonObject();
                dataPayload.addProperty("success", true);
                dataPayload.add("data", gson.toJsonTree(sanPham));
                return buildResponse(yeuCau, ActionType.GET_PRODUCT_BY_ID, dataPayload);
            }
            return buildResponse(yeuCau, ActionType.GET_PRODUCT_BY_ID, new BaseDTOs.ErrorResponse(StatusCode.NOT_FOUND, "Không tìm thấy sản phẩm", ErrorCode.ITEM_NOT_FOUND));
        } catch (Exception e) {
            return buildResponse(yeuCau, ActionType.GET_PRODUCT_BY_ID, new BaseDTOs.ErrorResponse(StatusCode.SERVER_ERROR, "Lỗi hệ thống.", ErrorCode.INTERNAL_SERVER_ERROR));
        }
    }

    private String xuLyCapNhatSanPham(JsonObject yeuCau, ClientHandler client) {
        try {
            User nguoiDung = client.layNguoiDungHienTai();
            if (nguoiDung == null || !"SELLER".equals(nguoiDung.getRoleName())) {
                return buildResponse(yeuCau, ActionType.UPDATE_PRODUCT, new BaseDTOs.ErrorResponse(StatusCode.FORBIDDEN, "Chỉ Seller mới được cập nhật!", ErrorCode.UNAUTHORIZED));
            }

            Item sanPham = ProductManager.getInstance().laySanPhamTheoId(yeuCau.get("itemId").getAsInt());
            if (sanPham == null) return buildResponse(yeuCau, ActionType.UPDATE_PRODUCT, new BaseDTOs.ErrorResponse(StatusCode.NOT_FOUND, "Không tìm thấy", ErrorCode.ITEM_NOT_FOUND));
            if (sanPham.getSellerId() != nguoiDung.getId()) return buildResponse(yeuCau, ActionType.UPDATE_PRODUCT, new BaseDTOs.ErrorResponse(StatusCode.FORBIDDEN, "Không có quyền", ErrorCode.FORBIDDEN));

            if (yeuCau.has("name")) sanPham.setName(yeuCau.get("name").getAsString());
            if (yeuCau.has("description")) sanPham.setDescription(yeuCau.get("description").getAsString());
            if (yeuCau.has("startingPrice")) sanPham.setStartingPrice(yeuCau.get("startingPrice").getAsLong());
            if (yeuCau.has("category")) sanPham.setCategory(yeuCau.get("category").getAsString());
            if (yeuCau.has("imageUrl")) sanPham.setImageUrl(yeuCau.get("imageUrl").getAsString());

            if (ProductManager.getInstance().capNhatSanPham(sanPham)) {
                JsonObject successPayload = new JsonObject();
                successPayload.addProperty("success", true);
                return buildResponse(yeuCau, ActionType.UPDATE_PRODUCT, successPayload);
            }
            return buildResponse(yeuCau, ActionType.UPDATE_PRODUCT, new BaseDTOs.ErrorResponse(StatusCode.SERVER_ERROR, "Lỗi DB", ErrorCode.INTERNAL_SERVER_ERROR));
        } catch (Exception e) {
            return buildResponse(yeuCau, ActionType.UPDATE_PRODUCT, new BaseDTOs.ErrorResponse(StatusCode.BAD_REQUEST, "Lỗi định dạng", ErrorCode.BAD_REQUEST));
        }
    }
}