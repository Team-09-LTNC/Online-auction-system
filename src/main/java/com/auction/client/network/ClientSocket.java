package com.auction.client.network;

import com.auction.common.dto.BaseDTOs;
import com.google.gson.Gson;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * CLIENT SOCKET MANAGER
 * 1. Singleton: Duy trì duy nhất một kết nối (Persistent Connection).
 * 2. DTO + Gson: Đóng gói dữ liệu JSON an toàn, không gửi String thô.
 * 3. Thread-safe UI: Cập nhật giao diện thông qua Platform.runLater.
 */
public class ClientSocket {

    private static final String SERVER_IP = "4.194.28.97";
    private static final int SERVER_PORT = 8080;

    private static ClientSocket instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final Gson gson = new Gson();

    // Constructor private để thực hiện Singleton
    private ClientSocket() {
        connect();
    }

    /**
     * Thực thi Singleton: Đảm bảo chỉ có một luồng giao tiếp duy nhất
     */
    public static synchronized ClientSocket getInstance() {
        if (instance == null) {
            instance = new ClientSocket();
        }
        return instance;
    }

    /**
     * Thiết lập kết nối bền vững (Persistent Connection)
     */
    private void connect() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);

            // Luồng ra: Đẩy JSON lên Server
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            // Luồng vào: Đợi nhận gói tin từ Server
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            System.out.println("[Network] Đã thiết lập kết nối Singleton thành công!");

            // Kích hoạt luồng nghe ngầm (Daemon Thread)
            startListeningThread();

        } catch (IOException e) {
            System.err.println("[Network Error] Không thể kết nối tới Server: " + e.getMessage());
        }
    }

    /**
     * Thực thi gửi DTO (Data Transfer Object) thay vì chuỗi String thô
     * Giải quyết lỗi vi phạm về bảo mật dữ liệu và parsing
     */
    public void sendRequest(BaseDTOs.Request request) {
        if (out != null) {
            String jsonPayload = gson.toJson(request);
            out.println(jsonPayload);
            System.out.println("[Client Sent JSON] " + jsonPayload);
        } else {
            System.err.println("[Network Error] Socket chưa sẵn sàng!");
        }
    }

    /**
     * Luồng nền (Background Thread) liên tục hứng dữ liệu từ Server đẩy về
     */
    private void startListeningThread() {
        Thread listenerThread = new Thread(() -> {
            try {
                String responseLine;
                while ((responseLine = in.readLine()) != null) {
                    final String rawData = responseLine;
                    System.out.println("[Server Push] " + rawData);

                    // Cập nhật giao diện an toàn trên luồng JavaFX
                    Platform.runLater(() -> {
                        handleServerResponse(rawData);
                    });
                }
            } catch (IOException e) {
                System.err.println("[Network Error] Mất kết nối luồng đọc: " + e.getMessage());
            }
        });

        listenerThread.setDaemon(true); // Luồng sẽ tự hủy khi đóng App
        listenerThread.start();
    }

    /**
     * Logic xử lý dữ liệu sau khi nhận được từ Server
     */
    private void handleServerResponse(String json) {
        // sẽ parse JSON ở đây và báo cho các View update dữ liệu
        // Ví dụ: update giá đấu mới nhất lên màn hình
    }

    /**
     * Kiểm tra trạng thái kết nối
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}