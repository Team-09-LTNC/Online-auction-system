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
import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * AuthController: Nhóm các chức năng liên quan đến xác thực người dùng. ( Nhóm AUTH trong ActionType)
 */
public class AuthController implements RequestHandler {
    private final Gson gson = new Gson();

    @Override
    public String xuLy(JsonObject yeuCau, ClientHandler client) {
        String loaiYeuCau = yeuCau.get("type").getAsString();

        switch (loaiYeuCau) {
            case ActionType.LOGIN:
                return xuLyDangNhap(yeuCau, client);
            case ActionType.REGISTER:
                return xuLyDangKy(yeuCau);
            case ActionType.LOGOUT:
                return xuLyDangXuat(client);
            default:
                return null;
        }
    }

    private String xuLyDangNhap(JsonObject yeuCau, ClientHandler client) {
        AuthDTOs.LoginRequest request = gson.fromJson(yeuCau, AuthDTOs.LoginRequest.class);
        try {
            User nguoiDung = UserManager.getInstance().dangNhap(request.getUsername(), request.getPassword());
            client.datNguoiDungHienTai(nguoiDung);

            AuthDTOs.UserDTO userDTO = new AuthDTOs.UserDTO(
                    nguoiDung.getId(), nguoiDung.getUsername(), nguoiDung.getRoleName(), nguoiDung.getFullName()
            );
            return gson.toJson(new AuthDTOs.LoginResponse(true, "Đăng nhập thành công!", userDTO));
        } catch (Exception e) {
            return gson.toJson(new BaseDTOs.ErrorResponse(StatusCode.UNAUTHORIZED, e.getMessage(), ErrorCode.INVALID_CREDENTIALS));
        }
    }

    private String xuLyDangKy(JsonObject yeuCau) {
        AuthDTOs.RegisterRequest request = gson.fromJson(yeuCau, AuthDTOs.RegisterRequest.class);
        User nguoiDungMoi = "SELLER".equalsIgnoreCase(request.getRole())
                ? new Seller(request.getUsername(), request.getPassword(), request.getFullName())
                : new Bidder(request.getUsername(), request.getPassword(), request.getFullName());

        if (UserManager.getInstance().dangKy(nguoiDungMoi)) {
            return gson.toJson(new AuthDTOs.RegisterResponse(true, "Đăng ký thành công!"));
        } else {
            return gson.toJson(new BaseDTOs.ErrorResponse(StatusCode.BAD_REQUEST, "Tên đăng nhập đã tồn tại!", ErrorCode.USERNAME_ALREADY_EXISTS));
        }
    }

    private String xuLyDangXuat(ClientHandler client) {
        if (client.layNguoiDungHienTai() != null) {
            UserManager.getInstance().dangXuat(client.layNguoiDungHienTai().getId());
            client.datNguoiDungHienTai(null);
        }
        return gson.toJson(new BaseDTOs.Response("LOGOUT_RESPONSE", StatusCode.OK, true, "Đăng xuất thành công") {});
    }
}