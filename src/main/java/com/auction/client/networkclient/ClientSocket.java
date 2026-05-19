package com.auction.client.networkclient;

import com.auction.common.dto.BaseDTOs;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ClientSocket {

    private static final Logger logger = LoggerFactory.getLogger(ClientSocket.class);
    private static String SERVER_IP = "127.0.0.1";
    private static int SERVER_PORT = 8080;

    static {
        try (InputStream input = ClientSocket.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                Properties props = new Properties();
                props.load(input);
                SERVER_IP = props.getProperty("server.ip", "127.0.0.1");
                SERVER_PORT = Integer.parseInt(props.getProperty("server.port", "8080"));
                logger.info("Đã nạp cấu hình mạng: IP = {}, Port = {}", SERVER_IP, SERVER_PORT);
            }
        } catch (Exception e) {
            logger.error("Lỗi khi đọc cấu hình mạng, sử dụng mặc định.", e);
        }
    }

    private static ClientSocket instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final Gson gson = new Gson();

    // LƯU Ý: Đã đổi cơ chế, giờ map sẽ lưu Callback với Khóa (Key) là `requestId` thay vì `type`
    private final Map<String, Consumer<JsonObject>> responseCallbacks = new ConcurrentHashMap<>();

    private ClientSocket() { connect(); }

    public static synchronized ClientSocket getInstance() {
        if (instance == null) instance = new ClientSocket();
        return instance;
    }

    private void connect() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            logger.info("Đã kết nối Server {}:{}", SERVER_IP, SERVER_PORT);
            startListeningThread();
        } catch (IOException e) {
            logger.error("Kết nối Server thất bại: {}", e.getMessage());
        }
    }

    /**
     * Gửi JSON lên Server. Lưu trữ Callback dựa vào requestId để định tuyến chính xác.
     */
    public void sendJsonRequest(JsonObject jsonObject, String expectedResponseType, Consumer<JsonObject> onResponse) {
        if (out != null) {
            // Lấy requestId từ gói tin JSON (Lớp BaseDTOs.Request tự sinh ra)
            String requestId = jsonObject.has("requestId") && !jsonObject.get("requestId").isJsonNull()
                    ? jsonObject.get("requestId").getAsString() : null;

            if (onResponse != null) {
                if (requestId != null) {
                    // Định tuyến chuẩn: Dùng requestId
                    responseCallbacks.put(requestId, onResponse);
                } else if (expectedResponseType != null) {
                    // Dự phòng: Lỡ gói tin không có requestId thì vẫn dùng type như cũ
                    responseCallbacks.put(expectedResponseType, onResponse);
                }
            }

            String jsonPayload = gson.toJson(jsonObject);
            out.println(jsonPayload);
            logger.debug("Request -> Server: {}", jsonPayload);
        } else {
            logger.error("Socket chưa sẵn sàng!");
        }
    }

    public void sendRequest(BaseDTOs.Request request) {
        JsonObject jsonObject = gson.toJsonTree(request).getAsJsonObject();
        sendJsonRequest(jsonObject, null, null);
    }

    private void startListeningThread() {
        Thread listenerThread = new Thread(() -> {
            try {
                String responseLine;
                while ((responseLine = in.readLine()) != null) {
                    handleServerResponse(responseLine);
                }
            } catch (IOException e) {
                logger.error("Mất kết nối đọc: {}", e.getMessage());
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void handleServerResponse(String json) {
        try {
            JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
            if (!jsonObject.has("type")) return;

            String type = jsonObject.get("type").getAsString();

            if (isPushEvent(type)) {
                PushHandler.handle(type, jsonObject);
            } else {
                // 1. Trích xuất requestId từ Server trả về
                String requestId = jsonObject.has("requestId") && !jsonObject.get("requestId").isJsonNull()
                        ? jsonObject.get("requestId").getAsString() : null;

                Consumer<JsonObject> callback = null;

                // 2. Tìm Callback tương ứng
                if (requestId != null && responseCallbacks.containsKey(requestId)) {
                    callback = responseCallbacks.remove(requestId); // Xóa sau khi dùng (tránh rò rỉ RAM)
                } else if (responseCallbacks.containsKey(type)) {
                    // Fallback (Dự phòng): Nếu Server quên trả requestId, tìm theo type cũ
                    callback = responseCallbacks.remove(type);
                }

                // 3. Thực thi Callback đẩy data về Giao diện
                if (callback != null) {
                    callback.accept(jsonObject);
                } else {
                    logger.debug("Nhận phản hồi nhưng không có Callback (requestId: {}, type: {})", requestId, type);
                }
            }
        } catch (JsonSyntaxException e) {
            logger.error("JSON lỗi từ Server: {}", e.getMessage());
        }
    }

    private boolean isPushEvent(String type) {
        return com.auction.common.enums.ActionType.AUCTION_BID_UPDATE.equals(type) ||
                com.auction.common.enums.ActionType.AUCTION_RESULT.equals(type) ||
                com.auction.common.enums.ActionType.RECEIVE_CHAT_MESSAGE.equals(type);
    }
}