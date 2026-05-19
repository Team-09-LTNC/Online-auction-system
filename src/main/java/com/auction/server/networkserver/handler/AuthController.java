package com.auction.server.networkserver.handler;

import com.auction.common.dto.AuthDTOs;
import com.auction.common.dto.BaseDTOs;
import com.auction.common.enums.ActionType;
import com.auction.common.enums.ErrorCode;
import com.auction.common.enums.StatusCode;
import com.auction.common.model.user.Bidder;
import com.auction.common.model.user.Seller;
import com.auction.common.model.user.User;
import com.auction.server.manager.UserManager;
import com.auction.server.networkserver.ClientHandler;
import com.auction.server.dao.UserDao;
import com.auction.server.dao.WalletTransactionDao;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Optional;

public class AuthController implements RequestHandler {
    private final Gson gson = new Gson();
    private final UserDao userDao = new UserDao();
    private final WalletTransactionDao walletTransactionDao = new WalletTransactionDao();

    @Override
    public String xuLy(JsonObject yeuCau, ClientHandler client) {
        String loaiYeuCau = yeuCau.get("type").getAsString();

        // Trích xuất chung mã requestId từ Client gửi lên
        String reqId = yeuCau.has("requestId") && !yeuCau.get("requestId").isJsonNull()
                ? yeuCau.get("requestId").getAsString() : null;

        // Truyền thêm reqId vào tất cả các phương thức con
        switch (loaiYeuCau) {
            case ActionType.LOGIN:
                return xuLyDangNhap(yeuCau, client, reqId);
            case ActionType.REGISTER:
                return xuLyDangKy(yeuCau, reqId);
            case ActionType.LOGOUT:
                return xuLyDangXuat(client, reqId);
            case ActionType.TOP_UP_MONEY:
                return xuLyNapTien(yeuCau, client, reqId);
            case ActionType.WITHDRAW_MONEY:
                return xuLyRutTien(yeuCau, client, reqId);
            case "GET_WALLET_HISTORY":
                return xuLyLayLichSuGiaoDich(yeuCau, client, reqId);
            default:
                return null;
        }
    }

    private String xuLyDangNhap(JsonObject yeuCau, ClientHandler client, String reqId) {
        AuthDTOs.LoginRequest request = gson.fromJson(yeuCau, AuthDTOs.LoginRequest.class);
        try {
            User nguoiDung = UserManager.getInstance().dangNhap(request.getUsername(), request.getPassword(), request.getRole());
            client.datNguoiDungHienTai(nguoiDung);

            AuthDTOs.UserDTO userDTO = new AuthDTOs.UserDTO(
                    nguoiDung.getId(), nguoiDung.getUsername(), nguoiDung.getRoleName(), nguoiDung.getFullName()
            );

            AuthDTOs.LoginResponse response = new AuthDTOs.LoginResponse(true, "Đăng nhập thành công!", userDTO);
            response.setRequestId(reqId); // Bắt buộc: Gắn requestId vào response Thành công
            return gson.toJson(response);

        } catch (Exception e) {
            BaseDTOs.ErrorResponse err = new BaseDTOs.ErrorResponse(StatusCode.UNAUTHORIZED, e.getMessage(), ErrorCode.INVALID_CREDENTIALS);
            err.setRequestId(reqId); // Bắt buộc: Gắn requestId vào response Lỗi
            return gson.toJson(err);
        }
    }

    private String xuLyDangKy(JsonObject yeuCau, String reqId) {
        AuthDTOs.RegisterRequest request = gson.fromJson(yeuCau, AuthDTOs.RegisterRequest.class);
        User nguoiDungMoi = "SELLER".equalsIgnoreCase(request.getRole())
                ? new Seller(request.getUsername(), request.getPassword(), request.getFullName())
                : new Bidder(request.getUsername(), request.getPassword(), request.getFullName());

        if (UserManager.getInstance().dangKy(nguoiDungMoi)) {
            AuthDTOs.RegisterResponse response = new AuthDTOs.RegisterResponse(true, "Đăng ký thành công!");
            response.setRequestId(reqId);
            return gson.toJson(response);
        } else {
            BaseDTOs.ErrorResponse err = new BaseDTOs.ErrorResponse(StatusCode.BAD_REQUEST, "Tên đăng nhập đã tồn tại!", ErrorCode.USERNAME_ALREADY_EXISTS);
            err.setRequestId(reqId);
            return gson.toJson(err);
        }
    }

    private String xuLyDangXuat(ClientHandler client, String reqId) {
        if (client.layNguoiDungHienTai() != null) {
            UserManager.getInstance().dangXuat(client.layNguoiDungHienTai().getId());
            client.datNguoiDungHienTai(null);
        }
        JsonObject phanHoi = new JsonObject();
        phanHoi.addProperty("type", "LOGOUT_RESPONSE");
        phanHoi.addProperty("statusCode", StatusCode.OK);
        phanHoi.addProperty("success", true);
        phanHoi.addProperty("message", "Đăng xuất thành công");

        if (reqId != null) phanHoi.addProperty("requestId", reqId); // Gắn requestId
        return gson.toJson(phanHoi);
    }

    private String xuLyNapTien(JsonObject yeuCau, ClientHandler client, String reqId) {
        User nguoiDung = client.layNguoiDungHienTai();
        if (nguoiDung == null) {
            BaseDTOs.ErrorResponse err = new BaseDTOs.ErrorResponse(StatusCode.UNAUTHORIZED, "Chưa đăng nhập!", ErrorCode.UNAUTHORIZED);
            err.setRequestId(reqId);
            return gson.toJson(err);
        }

        long soTienNap = yeuCau.has("amount") ? yeuCau.get("amount").getAsLong() : 0;
        if (soTienNap <= 0) {
            BaseDTOs.ErrorResponse err = new BaseDTOs.ErrorResponse(StatusCode.BAD_REQUEST, "Số tiền nạp không hợp lệ", ErrorCode.BAD_REQUEST);
            err.setRequestId(reqId);
            return gson.toJson(err);
        }

        Optional<User> userOpt = userDao.timTheoTenDangNhap(nguoiDung.getUsername());
        if (userOpt.isPresent()) {
            User currentUser = userOpt.get();
            long newBalance = 0;
            try {
                java.lang.reflect.Method getBalMethod = currentUser.getClass().getMethod("getBalance");
                long currentBalance = (Long) getBalMethod.invoke(currentUser);
                newBalance = currentBalance + soTienNap;
            } catch(Exception e) {
                BaseDTOs.ErrorResponse err = new BaseDTOs.ErrorResponse(StatusCode.FORBIDDEN, "Tài khoản không hỗ trợ số dư", ErrorCode.FORBIDDEN);
                err.setRequestId(reqId);
                return gson.toJson(err);
            }

            if (userDao.capNhatSoDu(currentUser.getId(), newBalance)) {
                walletTransactionDao.logTransaction(currentUser.getId(), "DEPOSIT", soTienNap, "Nạp tiền vào ví");

                JsonObject phanHoi = new JsonObject();
                phanHoi.addProperty("type", "TOP_UP_RESPONSE");
                phanHoi.addProperty("success", true);
                phanHoi.addProperty("message", "Nạp tiền thành công!");
                phanHoi.addProperty("newBalance", newBalance);

                if (reqId != null) phanHoi.addProperty("requestId", reqId);
                return gson.toJson(phanHoi);
            }
        }
        BaseDTOs.ErrorResponse err = new BaseDTOs.ErrorResponse(StatusCode.SERVER_ERROR, "Lỗi khi nạp tiền", ErrorCode.INTERNAL_SERVER_ERROR);
        err.setRequestId(reqId);
        return gson.toJson(err);
    }

    private String xuLyRutTien(JsonObject yeuCau, ClientHandler client, String reqId) {
        User nguoiDung = client.layNguoiDungHienTai();
        if (nguoiDung == null) {
            BaseDTOs.ErrorResponse err = new BaseDTOs.ErrorResponse(StatusCode.UNAUTHORIZED, "Chưa đăng nhập!", ErrorCode.UNAUTHORIZED);
            err.setRequestId(reqId);
            return gson.toJson(err);
        }

        long soTienRut = yeuCau.has("amount") ? yeuCau.get("amount").getAsLong() : 0;
        if (soTienRut <= 0) {
            BaseDTOs.ErrorResponse err = new BaseDTOs.ErrorResponse(StatusCode.BAD_REQUEST, "Số tiền rút không hợp lệ", ErrorCode.BAD_REQUEST);
            err.setRequestId(reqId);
            return gson.toJson(err);
        }

        Optional<User> userOpt = userDao.timTheoTenDangNhap(nguoiDung.getUsername());
        if (userOpt.isPresent()) {
            User currentUser = userOpt.get();
            long newBalance = 0;
            try {
                java.lang.reflect.Method getBalMethod = currentUser.getClass().getMethod("getBalance");
                long currentBalance = (Long) getBalMethod.invoke(currentUser);
                if (currentBalance < soTienRut) {
                    BaseDTOs.ErrorResponse err = new BaseDTOs.ErrorResponse(StatusCode.BAD_REQUEST, "Số dư không đủ để rút", ErrorCode.BAD_REQUEST);
                    err.setRequestId(reqId);
                    return gson.toJson(err);
                }
                newBalance = currentBalance - soTienRut;
            } catch(Exception e) {
                BaseDTOs.ErrorResponse err = new BaseDTOs.ErrorResponse(StatusCode.FORBIDDEN, "Tài khoản không hỗ trợ số dư", ErrorCode.FORBIDDEN);
                err.setRequestId(reqId);
                return gson.toJson(err);
            }

            if (userDao.capNhatSoDu(currentUser.getId(), newBalance)) {
                walletTransactionDao.logTransaction(currentUser.getId(), "WITHDRAW", soTienRut, "Rút tiền từ ví");

                JsonObject phanHoi = new JsonObject();
                phanHoi.addProperty("type", "WITHDRAW_RESPONSE");
                phanHoi.addProperty("success", true);
                phanHoi.addProperty("message", "Rút tiền thành công!");
                phanHoi.addProperty("newBalance", newBalance);

                if (reqId != null) phanHoi.addProperty("requestId", reqId);
                return gson.toJson(phanHoi);
            }
        }
        BaseDTOs.ErrorResponse err = new BaseDTOs.ErrorResponse(StatusCode.SERVER_ERROR, "Lỗi khi rút tiền", ErrorCode.INTERNAL_SERVER_ERROR);
        err.setRequestId(reqId);
        return gson.toJson(err);
    }

    private String xuLyLayLichSuGiaoDich(JsonObject yeuCau, ClientHandler client, String reqId) {
        User nguoiDung = client.layNguoiDungHienTai();
        if (nguoiDung == null) {
            BaseDTOs.ErrorResponse err = new BaseDTOs.ErrorResponse(StatusCode.UNAUTHORIZED, "Chưa đăng nhập!", ErrorCode.UNAUTHORIZED);
            err.setRequestId(reqId);
            return gson.toJson(err);
        }

        JsonArray history = walletTransactionDao.getTransactionHistory(nguoiDung.getId());
        JsonObject phanHoi = new JsonObject();
        phanHoi.addProperty("type", "WALLET_HISTORY_RESPONSE");
        phanHoi.addProperty("success", true);
        phanHoi.add("data", history);

        if (reqId != null) phanHoi.addProperty("requestId", reqId); // Gắn requestId

        Optional<User> userOpt = userDao.timTheoTenDangNhap(nguoiDung.getUsername());
        if (userOpt.isPresent()) {
            try {
                User currentUser = userOpt.get();
                java.lang.reflect.Method getBalMethod = currentUser.getClass().getMethod("getBalance");
                long currentBalance = (Long) getBalMethod.invoke(currentUser);
                phanHoi.addProperty("currentBalance", currentBalance);
            } catch (Exception ignored) {}
        }

        return gson.toJson(phanHoi);
    }
}