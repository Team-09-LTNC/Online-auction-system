package com.auction.client.network;

import com.auction.common.dto.Request;
import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;


// Nhiệm vụ:Quản lý danh sách các Client đang kết nối

public class NetworkManager {

    private static final String SERVER_IP = "4.194.28.97";
    private static final int SERVER_PORT = 8080;

    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;
    private static final Gson gson = new Gson();

    /**
     *Khởi tạo kết nối 1 lần duy nhất khi mở app hoặc khi Login
     */
    public static void connect() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);

            // Dùng luồng ký tự (Character Stream) JSON
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            System.out.println("[Network] Đã kết nối thành công tới Server!");

            //Chạy một luồng nền để liên tục lắng nghe Server
            startListeningThread();

        } catch (IOException e) {
            System.err.println("[Network Error] Không thể kết nối tới Server: " + e.getMessage());
        }
    }

    /**
     * Chuyển Object thành chuỗi JSON và đẩy qua Socket
     */
    public static void sendRequest(Request request) {
        if (out != null) {
            String jsonPayload = gson.toJson(request);
            out.println(jsonPayload); // Gửi kèm ký tự xuống dòng để Server biết kết thúc gói tin
            System.out.println("[Client Sent] " + jsonPayload);
        } else {
            System.err.println("[Network Error] Socket chưa được khởi tạo!");
        }
    }

    /**
     * Xử lý đa luồng: Lắng nghe Server mà không làm đơ giao diện
     */
    private static void startListeningThread() {
        Thread listenerThread = new Thread(() -> {
            try {
                String responseLine;
                // in.readLine() sẽ block luồng nền này để đợi data, bảo vệ luồng giao diện (UI)
                while ((responseLine = in.readLine()) != null) {
                    System.out.println("[Client Received] " + responseLine);

                    // TODO: Dùng Gson để chuyển responseLine thành Response Object
                    // TODO: Gọi Platform.runLater(...) để cập nhật giao diện JavaFX
                }
            } catch (IOException e) {
                System.err.println("[Network Error] Mất kết nối tới Server: " + e.getMessage());
            }
        });

        listenerThread.setDaemon(true); // Luồng tự tắt khi tắt ứng dụng chính
        listenerThread.start();
    }
}