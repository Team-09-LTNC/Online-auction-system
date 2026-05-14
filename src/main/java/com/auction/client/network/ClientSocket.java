package com.auction.client.network;

import com.auction.common.dto.BaseDTOs;
import com.google.gson.Gson;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * CLIENT SOCKET MANAGER
 * 1. Singleton: Duy trì duy nhất một kết nối (Persistent Connection).
 * 2. DTO + Gson: Đóng gói dữ liệu JSON an toàn, không gửi String thô.
 * 3. Thread-safe UI: Cập nhật giao diện thông qua Platform.runLater.
 */
public class ClientSocket {

    private static final Logger logger = LoggerFactory.getLogger(ClientSocket.class);
    //  giá trị mặc định của server,
//  nếu muốn test xem chạy oke không ae cứ sửa địa chỉ thành localhost máy mình trong application.properties trước để test
//  và không cần sửa đây, code  dưới đọc từ file properties, đây chỉ mặc định khi hệ thống chạy tốt
    private static String SERVER_IP = "127.0.0.1";
    private static int SERVER_PORT = 8080;

    // --- KHỐI STATIC: ĐỌC CẤU HÌNH TỪ FILE KHI CHẠY APP ---
    static {
        // Lưu ý: Client cũng cần có file application.properties (hoặc client_config.properties) trong thư mục resources
        try (InputStream input = ClientSocket.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                Properties props = new Properties();
                props.load(input);
                // Đọc từ file cấu hình, nếu file k có dòng này thì dùng 127.0.0.1 làm dự phòng
                SERVER_IP = props.getProperty("server.ip", "127.0.0.1");
                SERVER_PORT = Integer.parseInt(props.getProperty("server.port", "8080"));
                logger.info("Đã nạp cấu hình mạng từ properties: IP = {}, Port = {}", SERVER_IP, SERVER_PORT);
            } else {
                logger.warn("Không tìm thấy file application.properties, sử dụng localhost:8080 mặc định.");
            }
        } catch (Exception e) {
            logger.error("Lỗi khi đọc cấu hình mạng, sử dụng cấu hình mặc định.", e);
        }
    }
    // ---------------------------------------------------------

    private static ClientSocket instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final Gson gson = new Gson();

    private ClientSocket() {
        connect();
    }

    public static synchronized ClientSocket getInstance() {
        if (instance == null) {
            instance = new ClientSocket();
        }
        return instance;
    }

    private void connect() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);

            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            logger.info("ClientSocket đã kết nối tới Server tại {}:{}", SERVER_IP, SERVER_PORT);

            startListeningThread();

        } catch (IOException e) {
            System.err.println("[Network Error] Không thể kết nối tới Server: " + e.getMessage());
            logger.error("Không thể kết nối tới Server tại {}:{}", SERVER_IP, SERVER_PORT, e.getMessage());
        }
    }

    public void sendRequest(BaseDTOs.Request request) {
        if (out != null) {
            String jsonPayload = gson.toJson(request);
            out.println(jsonPayload);
            logger.debug("Đã gửi request tới Server: {}", jsonPayload);
        } else {
            System.err.println("[Network Error] Socket chưa sẵn sàng!");
            logger.error("Socket chưa sẵn sàng để gửi request!");
        }
    }

    private void startListeningThread() {
        Thread listenerThread = new Thread(() -> {
            try {
                String responseLine;
                while ((responseLine = in.readLine()) != null) {
                    final String rawData = responseLine;
                    logger.debug("Nhận dữ liệu đẩy từ Server: {}", rawData);

                    Platform.runLater(() -> {
                        handleServerResponse(rawData);
                    });
                }
            } catch (IOException e) {
                System.err.println("[Network Error] Mất kết nối luồng đọc: " + e.getMessage());
                logger.error("Mất kết nối luồng đọc: {}", e.getMessage());
            }
        });

        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void handleServerResponse(String json) {
        // Parse JSON và update UI
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}