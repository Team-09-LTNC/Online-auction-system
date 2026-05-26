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
import com.auction.server.manager.SystemNotificationManager;
import com.auction.server.networkserver.ClientHandler;
import com.auction.server.dao.UserDao;
import com.auction.server.dao.WalletTransactionDao;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Optional;

public class AuthController implements RequestHandler {
    private final Gson gson = new Gson();
    private final UserDao userDao = new UserDao();
    private final WalletTransactionDao walletTransactionDao = new WalletTransactionDao();

    @Override
    public String handleRequest(JsonObject yeuCau, ClientHandler client) {
        if (yeuCau == null || !yeuCau.has("type") || yeuCau.get("type").isJsonNull()) {
            return gson.toJson(new BaseDTOs.ErrorResponse(StatusCode.BAD_REQUEST, "Thieu truong type.", ErrorCode.BAD_REQUEST));
        }
        String loaiYeuCau = yeuCau.get("type").getAsString();

        // Trích xuất chung mã requestId từ Client gửi lên
        String reqId = yeuCau.has("requestId") && !yeuCau.get("requestId").isJsonNull()
                ? yeuCau.get("requestId").getAsString()
                : null;

        // Truyền thêm reqId vào tất cả các phương thức con
        switch (loaiYeuCau) {
            case ActionType.LOGIN:
                return handleLogin(yeuCau, client, reqId);
            case ActionType.REGISTER:
                return handleRegister(yeuCau, reqId);
            case ActionType.LOGOUT:
                return handleLogout(client, reqId);
            case ActionType.TOP_UP_MONEY:
                return handleDeposit(yeuCau, client, reqId);
            case ActionType.WITHDRAW_MONEY:
                return handleWithdraw(yeuCau, client, reqId);
            case "GET_WALLET_HISTORY":
                return handleGetWalletHistory(yeuCau, client, reqId);

            // Thêm vào switch trong handleRequest() để xử lý các yêu cầu admin mới
            case ActionType.ADMIN_GET_ALL_BIDDERS:
                return handleGetBidders(reqId);
            case ActionType.ADMIN_GET_ALL_SELLERS:
                return handleGetSellers(reqId);
            case ActionType.ADMIN_TOGGLE_LOCK_USER:
                return handleToggleAccountLock(yeuCau, reqId);
            default:
                return gson.toJson(new BaseDTOs.ErrorResponse(StatusCode.BAD_REQUEST, "Action khong duoc ho tro.", ErrorCode.BAD_REQUEST));
        }
    }

    // Xử lý yêu cầu lấy danh sách Bidder (Admin)
    private String handleGetBidders(String reqId) {
        List<User> bidders = userDao.getAllBidders(); // cần thêm method này vào UserDao
        JsonArray array = new JsonArray();
        for (User u : bidders) {
            JsonObject obj = new JsonObject();
            obj.addProperty("username", u.getUsername());
            obj.addProperty("fullname", u.getFullName());
            obj.addProperty("status", u.getStatus() != null ? u.getStatus() : "ACTIVE");
            array.add(obj);
        }
        JsonObject res = new JsonObject();
        res.addProperty("type", "GET_ALL_BIDDERS_RESPONSE");
        res.addProperty("success", true);
        res.add("data", array);
        if (reqId != null)
            res.addProperty("requestId", reqId);
        return gson.toJson(res);
    }

    // Tương tự như trên nhưng lấy danh sách Seller
    private String handleGetSellers(String reqId) {
        List<User> sellers = userDao.getAllSellers(); // cần thêm method này vào UserDao
        JsonArray array = new JsonArray();
        for (User u : sellers) {
            JsonObject obj = new JsonObject();
            obj.addProperty("username", u.getUsername());
            obj.addProperty("fullname", u.getFullName());
            obj.addProperty("status", u.getStatus() != null ? u.getStatus() : "ACTIVE");
            array.add(obj);
        }
        JsonObject res = new JsonObject();
        res.addProperty("type", "GET_ALL_SELLERS_RESPONSE");
        res.addProperty("success", true);
        res.add("data", array);
        if (reqId != null)
            res.addProperty("requestId", reqId);
        return gson.toJson(res);
    }

    private String handleLogin(JsonObject yeuCau, ClientHandler client, String reqId) {
        AuthDTOs.LoginRequest request = gson.fromJson(yeuCau, AuthDTOs.LoginRequest.class);
        try {
            User nguoiDung = UserManager.getInstance().login(request.getUsername(), request.getPassword(),
                    request.getRole());
            client.setCurrentUser(nguoiDung);

            AuthDTOs.UserDTO userDTO = new AuthDTOs.UserDTO(
                    nguoiDung.getId(), nguoiDung.getUsername(), nguoiDung.getRoleName(), nguoiDung.getFullName());

            AuthDTOs.LoginResponse response = new AuthDTOs.LoginResponse(true, "Đăng nhập thành công!", userDTO);
            response.setRequestId(reqId);
            return gson.toJson(response);

        } catch (Exception e) {
            JsonObject errorJson = new JsonObject();
            errorJson.addProperty("type", "LOGIN_RESPONSE"); // Bắt buộc phải có dòng này
            errorJson.addProperty("success", false);
            errorJson.addProperty("message", e.getMessage());

            if (reqId != null) {
                errorJson.addProperty("requestId", reqId);
            }
            return gson.toJson(errorJson);
        }
    }

    private String handleRegister(JsonObject yeuCau, String reqId) {
        AuthDTOs.RegisterRequest request = gson.fromJson(yeuCau, AuthDTOs.RegisterRequest.class);
        User nguoiDungMoi = "SELLER".equalsIgnoreCase(request.getRole())
                ? new Seller(request.getUsername(), request.getPassword(), request.getFullName())
                : new Bidder(request.getUsername(), request.getPassword(), request.getFullName());

        if (UserManager.getInstance().register(nguoiDungMoi)) {
            AuthDTOs.RegisterResponse response = new AuthDTOs.RegisterResponse(true, "Đăng ký thành công!");
            response.setRequestId(reqId);
            return gson.toJson(response);
        } else {
            BaseDTOs.ErrorResponse err = new BaseDTOs.ErrorResponse(StatusCode.BAD_REQUEST, "Tên đăng nhập đã tồn tại!",
                    ErrorCode.USERNAME_ALREADY_EXISTS);
            err.setRequestId(reqId);
            return gson.toJson(err);
        }
    }

    private String handleLogout(ClientHandler client, String reqId) {
        if (client.getCurrentUser() != null) {
            UserManager.getInstance().logout(client.getCurrentUser().getId());
            client.setCurrentUser(null);
        }
        JsonObject phanHoi = new JsonObject();
        phanHoi.addProperty("type", "LOGOUT_RESPONSE");
        phanHoi.addProperty("statusCode", StatusCode.OK);
        phanHoi.addProperty("success", true);
        phanHoi.addProperty("message", "Đăng xuất thành công");

        if (reqId != null)
            phanHoi.addProperty("requestId", reqId); // Gắn requestId
        return gson.toJson(phanHoi);
    }

    private String handleDeposit(JsonObject yeuCau, ClientHandler client, String reqId) {
        User nguoiDung = client.getCurrentUser();
        if (nguoiDung == null) {
            BaseDTOs.ErrorResponse err = new BaseDTOs.ErrorResponse(StatusCode.UNAUTHORIZED, "Chưa đăng nhập!",
                    ErrorCode.UNAUTHORIZED);
            err.setRequestId(reqId);
            return gson.toJson(err);
        }

        long soTienNap = yeuCau.has("amount") ? yeuCau.get("amount").getAsLong() : 0;
        if (soTienNap <= 0) {
            BaseDTOs.ErrorResponse err = new BaseDTOs.ErrorResponse(StatusCode.BAD_REQUEST, "Số tiền nạp không hợp lệ",
                    ErrorCode.BAD_REQUEST);
            err.setRequestId(reqId);
            return gson.toJson(err);
        }

        Optional<User> userOpt = userDao.findByUsername(nguoiDung.getUsername());
        if (userOpt.isPresent()) {
            User currentUser = userOpt.get();
            long newBalance = 0;
            try {
                java.lang.reflect.Method getBalMethod = currentUser.getClass().getMethod("getBalance");
                long currentBalance = (Long) getBalMethod.invoke(currentUser);
                newBalance = currentBalance + soTienNap;
            } catch (Exception e) {
                BaseDTOs.ErrorResponse err = new BaseDTOs.ErrorResponse(StatusCode.FORBIDDEN,
                        "Tài khoản không hỗ trợ số dư", ErrorCode.FORBIDDEN);
                err.setRequestId(reqId);
                return gson.toJson(err);
            }

            if (userDao.updateBalance(currentUser.getId(), newBalance)) {
                String description = "Nạp tiền vào ví";
                walletTransactionDao.logTransaction(currentUser.getId(), "DEPOSIT", soTienNap, description);
                sendBalanceChangeNotification(currentUser.getId(), "DEPOSIT", soTienNap, description);

                JsonObject phanHoi = new JsonObject();
                phanHoi.addProperty("type", "TOP_UP_RESPONSE");
                phanHoi.addProperty("success", true);
                phanHoi.addProperty("message", "Nạp tiền thành công!");
                phanHoi.addProperty("newBalance", newBalance);

                if (reqId != null)
                    phanHoi.addProperty("requestId", reqId);
                return gson.toJson(phanHoi);
            }
        }
        BaseDTOs.ErrorResponse err = new BaseDTOs.ErrorResponse(StatusCode.SERVER_ERROR, "Lỗi khi nạp tiền",
                ErrorCode.INTERNAL_SERVER_ERROR);
        err.setRequestId(reqId);
        return gson.toJson(err);
    }

    private String handleWithdraw(JsonObject yeuCau, ClientHandler client, String reqId) {
        User nguoiDung = client.getCurrentUser();
        if (nguoiDung == null) {
            BaseDTOs.ErrorResponse err = new BaseDTOs.ErrorResponse(StatusCode.UNAUTHORIZED, "Chưa đăng nhập!",
                    ErrorCode.UNAUTHORIZED);
            err.setRequestId(reqId);
            return gson.toJson(err);
        }

        long soTienRut = yeuCau.has("amount") ? yeuCau.get("amount").getAsLong() : 0;
        if (soTienRut <= 0) {
            BaseDTOs.ErrorResponse err = new BaseDTOs.ErrorResponse(StatusCode.BAD_REQUEST, "Số tiền rút không hợp lệ",
                    ErrorCode.BAD_REQUEST);
            err.setRequestId(reqId);
            return gson.toJson(err);
        }

        Optional<User> userOpt = userDao.findByUsername(nguoiDung.getUsername());
        if (userOpt.isPresent()) {
            User currentUser = userOpt.get();
            long newBalance = 0;
            try {
                java.lang.reflect.Method getBalMethod = currentUser.getClass().getMethod("getBalance");
                long currentBalance = (Long) getBalMethod.invoke(currentUser);
                if (currentBalance < soTienRut) {
                    BaseDTOs.ErrorResponse err = new BaseDTOs.ErrorResponse(StatusCode.BAD_REQUEST,
                            "Số dư không đủ để rút", ErrorCode.BAD_REQUEST);
                    err.setRequestId(reqId);
                    return gson.toJson(err);
                }
                newBalance = currentBalance - soTienRut;
            } catch (Exception e) {
                BaseDTOs.ErrorResponse err = new BaseDTOs.ErrorResponse(StatusCode.FORBIDDEN,
                        "Tài khoản không hỗ trợ số dư", ErrorCode.FORBIDDEN);
                err.setRequestId(reqId);
                return gson.toJson(err);
            }

            if (userDao.updateBalance(currentUser.getId(), newBalance)) {
                String description = "Rút tiền từ ví";
                walletTransactionDao.logTransaction(currentUser.getId(), "WITHDRAW", soTienRut, description);
                sendBalanceChangeNotification(currentUser.getId(), "WITHDRAW", soTienRut, description);

                JsonObject phanHoi = new JsonObject();
                phanHoi.addProperty("type", "WITHDRAW_RESPONSE");
                phanHoi.addProperty("success", true);
                phanHoi.addProperty("message", "Rút tiền thành công!");
                phanHoi.addProperty("newBalance", newBalance);

                if (reqId != null)
                    phanHoi.addProperty("requestId", reqId);
                return gson.toJson(phanHoi);
            }
        }
        BaseDTOs.ErrorResponse err = new BaseDTOs.ErrorResponse(StatusCode.SERVER_ERROR, "Lỗi khi rút tiền",
                ErrorCode.INTERNAL_SERVER_ERROR);
        err.setRequestId(reqId);
        return gson.toJson(err);
    }

    private String handleGetWalletHistory(JsonObject yeuCau, ClientHandler client, String reqId) {
        User nguoiDung = client.getCurrentUser();
        if (nguoiDung == null) {
            BaseDTOs.ErrorResponse err = new BaseDTOs.ErrorResponse(StatusCode.UNAUTHORIZED, "Chưa đăng nhập!",
                    ErrorCode.UNAUTHORIZED);
            err.setRequestId(reqId);
            return gson.toJson(err);
        }

        JsonArray history = walletTransactionDao.getTransactionHistory(nguoiDung.getId());
        JsonObject phanHoi = new JsonObject();
        phanHoi.addProperty("type", "WALLET_HISTORY_RESPONSE");
        phanHoi.addProperty("success", true);
        phanHoi.add("data", history);

        if (reqId != null)
            phanHoi.addProperty("requestId", reqId); // Gắn requestId

        Optional<User> userOpt = userDao.findByUsername(nguoiDung.getUsername());
        if (userOpt.isPresent()) {
            try {
                User currentUser = userOpt.get();
                java.lang.reflect.Method getBalMethod = currentUser.getClass().getMethod("getBalance");
                long currentBalance = (Long) getBalMethod.invoke(currentUser);
                phanHoi.addProperty("currentBalance", currentBalance);
            } catch (Exception ignored) {
            }
        }

        return gson.toJson(phanHoi);
    }

    private void sendBalanceChangeNotification(int userId, String loaiGiaoDich, long soTien, String moTa) {
        String symbol = ("WITHDRAW".equals(loaiGiaoDich) || "PAYMENT_SENT".equals(loaiGiaoDich))
                ? "🔻"
                : "🔹";
        String noiDung = String.format("%s %s: %s đ - %s",
                symbol,
                loaiGiaoDich,
                formatMoney(soTien),
                moTa);
        SystemNotificationManager.getInstance().sendPrivateNotification(
                -1,
                userId,
                "Biến động số dư\n" + noiDung,
                false
        );
    }

    private String formatMoney(long value) {
        String digits = String.valueOf(value);
        StringBuilder formatted = new StringBuilder();
        int firstGroupLength = digits.length() % 3;
        if (firstGroupLength == 0) {
            firstGroupLength = 3;
        }

        formatted.append(digits, 0, firstGroupLength);
        for (int i = firstGroupLength; i < digits.length(); i += 3) {
            formatted.append('.').append(digits, i, i + 3);
        }
        return formatted.toString();
    }

    /*
     * Xử lý yêu cầu khoá tài khoản từ Admin. Yêu cầu này sẽ nhận vào username và
     * trạng thái mới (LOCKED/ACTIVE), cập nhật vào DB, và trả về kết quả cho Admin.
     * Lưu ý: Chỉ Admin mới có quyền gửi yêu cầu này, nên controller không cần kiểm
     * tra role ở đây mà sẽ dựa vào việc route yêu cầu từ ClientHandler đã đảm bảo
     * chỉ Admin mới có thể gọi đến phương thức này.
     * Response sẽ bao gồm thông tin username, trạng thái mới, và message phản hồi
     * để Admin có thể hiển thị thông báo phù hợp trên UI.
     */
    private String handleToggleAccountLock(JsonObject yeuCau, String reqId) {
        if (!yeuCau.has("username") || yeuCau.get("username").isJsonNull()
                || !yeuCau.has("newStatus") || yeuCau.get("newStatus").isJsonNull()) {
            BaseDTOs.ErrorResponse err = new BaseDTOs.ErrorResponse(StatusCode.BAD_REQUEST,
                    "Thieu username hoac newStatus.", ErrorCode.BAD_REQUEST);
            err.setRequestId(reqId);
            return gson.toJson(err);
        }
        String username = yeuCau.get("username").getAsString();
        String newStatus = yeuCau.get("newStatus").getAsString();

        boolean ok = userDao.updateStatus(username, newStatus);

        JsonObject res = new JsonObject();
        res.addProperty("type", "TOGGLE_LOCK_USER_RESPONSE");
        res.addProperty("success", ok);
        res.addProperty("message", ok
                ? ("LOCKED".equals(newStatus) ? "Đã khoá tài khoản!" : "Đã mở khoá tài khoản!")
                : "Cập nhật thất bại!");
        res.addProperty("username", username);
        res.addProperty("newStatus", newStatus);
        if (reqId != null)
            res.addProperty("requestId", reqId);
        return gson.toJson(res);
    }
}
