package com.auction.server.networkserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ServerManager: Quản lý kết nối mạng và đa luồng (Concurrency).
 * Nhiệm vụ chính: Lắng nghe các kết nối Socket tới từ Client và cấp phát
 * mỗi Client cho một luồng (Thread) riêng biệt thông qua Thread Pool.
 */
public class ServerManager {
    private static final Logger logger = LoggerFactory.getLogger(ServerManager.class);

    private final int port;
    // Thread Pool quản lý tối đa 50 kết nối đồng thời để tránh sập Server
    private final ExecutorService danhSachLuongClient = Executors.newFixedThreadPool(50);

    public ServerManager(int port) {
        this.port = port;
    }

    /**
     * Mở cổng kết nối và liên tục lắng nghe các yêu cầu từ Client.
     */
    public void batDauServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.info("Server đã mở thành công và đang lắng nghe tại cổng: {}", port);

            while (true) {
                // Chặn (block) cho đến khi có một Client thực sự kết nối tới
                Socket clientSocket = serverSocket.accept();
                logger.info("Có Client mới kết nối từ IP: {}", clientSocket.getInetAddress());

                // Khởi tạo ClientHandler để xử lý Client này và ném vào Thread Pool
                ClientHandler trinhXuLyClient = new ClientHandler(clientSocket);
                danhSachLuongClient.execute(trinhXuLyClient);
            }
        } catch (BindException e) {
            logger.warn("Không mở server mới được vì cổng {} đang được sử dụng.", port);
        } catch (IOException e) {
            logger.error("Lỗi nghiêm trọng khi mở cổng kết nối mạng: ", e);
        } finally {
            // Đảm bảo dọn dẹp Thread Pool khi Server bị tắt (shutdown)
            danhSachLuongClient.shutdown();
            logger.info("Hệ thống mạng của Server đã được đóng an toàn.");
        }
    }
}
