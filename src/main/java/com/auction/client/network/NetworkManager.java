package com.auction.client.network;

import com.auction.common.dto.BaseDTOs;
import com.google.gson.Gson;
import com.auction.common.enums.ActionType;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javafx.application.Platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NetworkManager {

    private static final Logger logger = LoggerFactory.getLogger(NetworkManager.class);

    private static final String SERVER_IP = "4.194.28.97";
    private static final int    SERVER_PORT = 8080;

    private static Socket         socket;
    private static PrintWriter    out;
    private static BufferedReader in;
    private static final Gson     gson = new Gson();

    // ✅ Map lưu callback: requestId → hàm xử lý response
    // ConcurrentHashMap vì listenerThread và UI thread cùng truy cập
    private static final ConcurrentHashMap<String, Consumer<BaseDTOs.Response>>
            pendingCallbacks = new ConcurrentHashMap<>();

    public static void connect() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            logger.info("Đã kết nối tới Server tại {}:{}", SERVER_IP, SERVER_PORT);
            startListeningThread();
        } catch (IOException e) {
            System.err.println("[Network Error] " + e.getMessage());
            logger.error("Không thể kết nối tới Server tại {}:{} ({})", SERVER_IP, SERVER_PORT, e.getMessage());
        }
    }

    /**
     * Gửi request kèm callback – Manager dùng method này
     * @param request  DTO cần gửi
     * @param callback Hàm xử lý khi nhận được response tương ứng
     */
    public static void sendRequest(BaseDTOs.Request request,
                                   Consumer<BaseDTOs.Response> callback) {
        // 1. Sinh requestId nếu chưa có
        if (request.requestId == null) {
            request.requestId = UUID.randomUUID().toString();
        }

        // 2. Lưu callback vào Map trước khi gửi
        if (callback != null) {
            pendingCallbacks.put(request.requestId, callback);
        }

        // 3. Gửi JSON qua socket
        String json = gson.toJson(request);
        out.println(json);
        logger.info("Đã gửi request tới Server: {}", json);
    }

    private static void startListeningThread() {
        Thread listener = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    final String responseLine = line;
                    logger.info("Nhận dữ liệu từ Server: {}", responseLine);

                    // Parse base để lấy type và requestId
                    BaseDTOs.Response base = gson.fromJson(responseLine, BaseDTOs.Response.class);

                    // ✅ Có requestId → đây là response cho 1 request cụ thể
                    if (base.requestId != null) {
                        Consumer<BaseDTOs.Response> callback =
                                pendingCallbacks.remove(base.requestId); // lấy ra và xóa

                        if (callback != null) {
                            // Phải chạy trên JavaFX thread để cập nhật UI
                            Platform.runLater(() -> callback.accept(base));
                        }

                        // ✅ Không có requestId → đây là Server Push (bid update, kết quả...)
                    } else {
                        Platform.runLater(() -> handleServerPush(base, responseLine));
                    }
                }
            } catch (IOException e) {
                System.err.println("[Network Error] Mất kết nối: " + e.getMessage());
                logger.error("Mất kết nối: {}", e.getMessage());
            }
        });

        listener.setDaemon(true);
        listener.start();
    }

    /** Xử lý các push từ server không kèm requestId */
    private static void handleServerPush(BaseDTOs.Response base, String raw) {
        logger.debug("Xử lý Server Push: {}", raw);
        switch (base.type) {
            case ActionType.AUCTION_BID_UPDATE -> {
                BaseDTOs.AuctionBidUpdatePush push =
                        gson.fromJson(raw, BaseDTOs.AuctionBidUpdatePush.class);
                // TODO: thông báo tới AuctionView đang mở
            }
            case ActionType.AUCTION_RESULT -> {
                BaseDTOs.AuctionResultPush push =
                        gson.fromJson(raw, BaseDTOs.AuctionResultPush.class);
                // TODO: thông báo kết quả đấu giá
            }
        }
    }
}