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
 * Bộ quản lý server: Quản lý kết nối mạng và xử lý đa luồng.
 * Nhiệm vụ chính: Lắng nghe các kết nối socket tới từ client và cấp phát
 * mỗi client cho một luồng riêng biệt thông qua nhóm luồng.
 */
public class ServerManager {
    private static final Logger logger = LoggerFactory.getLogger(ServerManager.class);

    private final int port;
    // Nhóm luồng quản lý tối đa 50 kết nối đồng thời để tránh sập server
    private final ExecutorService danhSachLuongClient = Executors.newFixedThreadPool(50);

    public ServerManager(int port) {
        this.port = port;
    }

    /**
     * Mở cổng kết nối và liên tục lắng nghe các yêu cầu từ client.
     */
    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.info("Server đã mở thành công và đang lắng nghe tại cổng: {}", port);

            while (true) {
                // Chặn luồng cho đến khi có một client thực sự kết nối tới
                Socket clientSocket = serverSocket.accept();
                logger.info("Có Client mới kết nối từ IP: {}", clientSocket.getInetAddress());

                // Khởi tạo ClientHandler để xử lý client này và đưa vào nhóm luồng
                ClientHandler trinhXuLyClient = new ClientHandler(clientSocket);
                danhSachLuongClient.execute(trinhXuLyClient);
            }
        } catch (BindException e) {
            logger.warn("Không mở server mới được vì cổng {} đang được sử dụng.", port);
        } catch (IOException e) {
            logger.error("Lỗi nghiêm trọng khi mở cổng kết nối mạng: ", e);
        } finally {
            // Đảm bảo dọn dẹp nhóm luồng khi server bị tắt
            danhSachLuongClient.shutdown();
            logger.info("Hệ thống mạng của Server đã được đóng an toàn.");
        }
    }
}
