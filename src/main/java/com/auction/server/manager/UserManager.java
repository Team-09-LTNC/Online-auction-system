package com.auction.server.manager;

import com.auction.common.model.user.User;
import com.auction.server.dao.UserDao;
import com.auction.common.exception.AuthenticationException;
import com.auction.server.networkserver.ClientHandler;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý đăng nhập và trạng thái Online của người dùng
 * Dùng Singleton để đảm bảo chỉ có 1 instance trên toàn Server
 */
public class UserManager {
    private static volatile UserManager instance;
    private final UserDao userDao;

    // Lưu trữ danh sách người dùng đang kết nối để gửi dữ liệu Real-time
    private final Map<Integer, User> onlineUsers = new ConcurrentHashMap<>();
    private final Map<Integer, Set<ClientHandler>> onlineConnections = new ConcurrentHashMap<>();

    private UserManager() {
        this.userDao = new UserDao();
    }

    public static UserManager getInstance() {
        if (instance == null) {
            synchronized (UserManager.class) {
                if (instance == null) {
                    instance = new UserManager();
                }
            }
        }
        return instance;
    }

    /**
     * Xác thực thông tin và đưa người dùng vào danh sách Online
     */
    public User login(String tenDangNhap, String matKhau, String role) throws AuthenticationException {
        // 1. Truy vấn Database tìm người dùng (Chỉ chọc xuống DB đúng 1 lần)
        Optional<User> userOpt = userDao.findByUsername(tenDangNhap);

        if (userOpt.isEmpty()) {
            throw new AuthenticationException("Tài khoản không tồn tại trong hệ thống!");
        }
        User user = userOpt.get();

        // 2. Kiểm tra mật khẩu thô theo yêu cầu hiện tại
        if (!user.getPassword().equals(matKhau)) {
            throw new AuthenticationException("Sai mật khẩu, vui lòng thử lại!");
        }

        // 3. Kiểm tra xem Role có khớp không
        if (!user.getRoleName().equalsIgnoreCase(role)) {
            throw new AuthenticationException("Tài khoản này không có quyền truy cập với vai trò " + role + "!");
        }

        // 4. TỐI ƯU: Lấy trực tiếp trạng thái từ RAM, xóa bỏ hoàn toàn truy vấn DB lần 2
        String status = user.getStatus() != null ? user.getStatus() : "ACTIVE";
        if ("LOCKED".equals(status)) {
            throw new AuthenticationException("Tài khoản đã bị khoá, vui lòng liên hệ Admin!");
        }

        // 5. Đăng nhập thành công -> Cập nhật trạng thái Online
        onlineUsers.put(user.getId(), user);
        return user;
    }

    /**
     * Tạo tài khoản mới, từ chối nếu tên đăng nhập đã tồn tại
     */
    public boolean register(User nguoiDungMoi) {
        if (userDao.findByUsername(nguoiDungMoi.getUsername()).isPresent()) {
            return false;
        }
        return userDao.saveUser(nguoiDungMoi);
    }

    /**
     * Xóa người dùng khỏi danh sách Online khi họ ngắt kết nối Socket
     */
    public void logout(int idNguoiDung) {
        onlineUsers.remove(idNguoiDung);
    }

    public void registerConnection(int userId, ClientHandler client) {
        onlineConnections
                .computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet())
                .add(client);
    }

    public void unregisterConnection(int userId, ClientHandler client) {
        Set<ClientHandler> connections = onlineConnections.get(userId);
        if (connections == null) {
            return;
        }
        connections.remove(client);
        if (connections.isEmpty()) {
            onlineConnections.remove(userId);
        }
    }

    public void sendSystemNotification(int userId, JsonObject payload) {
        Set<ClientHandler> connections = onlineConnections.get(userId);
        if (connections == null) {
            return;
        }
        for (ClientHandler client : connections) {
            client.sendSystemNotification(payload.deepCopy());
        }
    }

    public void broadcastPushEvent(JsonObject payload) {
        Set<ClientHandler> delivered = ConcurrentHashMap.newKeySet();
        for (Set<ClientHandler> connections : onlineConnections.values()) {
            for (ClientHandler client : connections) {
                if (delivered.add(client)) {
                    client.sendPushEvent(payload.deepCopy());
                }
            }
        }
    }

    /**
     * Trích xuất thông tin người dùng đang kết nối
     */
    public User getOnlineUser(int idNguoiDung) {
        return onlineUsers.get(idNguoiDung);
    }

    public boolean updateAccountStatus(int userId, String status) {
        boolean ok = userDao.updateStatusById(userId, status);
        if (!ok) {
            return false;
        }
        User online = onlineUsers.get(userId);
        if (online != null) {
            online.setStatus(status);
        }
        return true;
    }
}
