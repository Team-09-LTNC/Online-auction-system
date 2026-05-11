package com.auction.server.manager;

import com.auction.common.model.user.User;
import com.auction.server.dao.UserDao;
import com.auction.common.exception.AuthenticationException;
import java.util.Map;
import java.util.Optional;
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

    private UserManager() {
        this.userDao = new UserDao();
    }

    public static UserManager getInstance() {
        if (instance == null) {
            synchronized (UserManager.class) {
                if (instance == null) instance = new UserManager();
            }
        }
        return instance;
    }

    /**
     * Xác thực thông tin và đưa người dùng vào danh sách Online
     */
    public User dangNhap(String tenDangNhap, String matKhau) throws AuthenticationException {
        // 1. Truy vấn Database tìm người dùng
        Optional<User> userOpt = userDao.timTheoTenDangNhap(tenDangNhap);
        // 2. Nếu không tìm thấy -> Ném lỗi chi tiết
        if (userOpt.isEmpty()) {
            throw new AuthenticationException("Tài khoản không tồn tại trong hệ thống!");
        }
        User user = userOpt.get();
        // 3. Kiểm tra mật khẩu (Tạm thời so sánh chuỗi thô, sau này áp dụng mã hóa nếu đủ thời gian)
        if (!user.getPassword().equals(matKhau)) {
            throw new AuthenticationException("Sai mật khẩu, vui lòng thử lại!");
        }
        // 4. Đăng nhập thành công -> Cập nhật trạng thái Online và trả về dữ liệu
        onlineUsers.put(user.getId(), user);
        return user;
    }

    /**
     * Tạo tài khoản mới, từ chối nếu tên đăng nhập đã tồn tại
     */
    public boolean dangKy(User nguoiDungMoi) {
        if (userDao.timTheoTenDangNhap(nguoiDungMoi.getUsername()).isPresent()) {
            return false;
        }
        return userDao.luuNguoiDung(nguoiDungMoi);
    }

    /**
     * Xóa người dùng khỏi danh sách Online khi họ ngắt kết nối Socket
     */
    public void dangXuat(int idNguoiDung) {
        onlineUsers.remove(idNguoiDung);
    }

    /**
     * Trích xuất thông tin người dùng đang kết nối
     */
    public User layNguoiDungOnline(int idNguoiDung) {
        return onlineUsers.get(idNguoiDung);
    }
}