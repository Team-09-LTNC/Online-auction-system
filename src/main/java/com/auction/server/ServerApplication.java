//package com.auction.server;
//
//import java.io.*;
//import java.net.*;
//import java.nio.charset.StandardCharsets;
//import com.google.gson.*;
//
//public class ServerApplication {
//    private static final int PORT = 8080;
//
//    public static void main(String[] args) {
//        System.out.println("=== TEST SERVER (Có xử lý đăng nhập) ===");
//        System.out.println("Server đang lắng nghe tại cổng: " + PORT);
//
//        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
//            while (true) {
//                Socket clientSocket = serverSocket.accept();
//                System.out.println("[Server] Client mới kết nối từ: " + clientSocket.getInetAddress());
//
//                // Tạo thread mới xử lý client
//                new Thread(() -> handleClient(clientSocket)).start();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private static void handleClient(Socket socket) {
//        try (
//                BufferedReader in = new BufferedReader(
//                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
//                PrintWriter out = new PrintWriter(
//                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)
//        ) {
//            Gson gson = new Gson();
//            String jsonLine;
//
//            while ((jsonLine = in.readLine()) != null) {
//                System.out.println("[Server Received] " + jsonLine);
//
//                // Parse JSON
//                JsonObject request = JsonParser.parseString(jsonLine).getAsJsonObject();
//                String type = request.get("type").getAsString();
//
//                String response;
//
//                switch (type) {
//                    case "LOGIN_REQUEST":
//                        response = handleLogin(request, gson);
//                        break;
//                    case "REGISTER_REQUEST":
//                        response = handleRegister(request, gson);
//                        break;
//                    default:
//                        response = createErrorResponse("UNKNOWN_TYPE", "Không hỗ trợ loại request này");
//                }
//
//                out.println(response);
//                System.out.println("[Server Sent] " + response);
//            }
//        } catch (IOException e) {
//            System.out.println("[Server] Client ngắt kết nối");
//        }
//    }
//
//    private static String handleLogin(JsonObject request, Gson gson) {
//        String username = request.get("username").getAsString();
//        String password = request.get("password").getAsString();
//
//        // Test: Chỉ cần username = "test" và password = "123"
//        if ("test".equals(username) && "123".equals(password)) {
//            JsonObject response = new JsonObject();
//            response.addProperty("success", true);
//            response.addProperty("message", "Đăng nhập thành công!");
//
//            JsonObject userData = new JsonObject();
//            userData.addProperty("id", 1);
//            userData.addProperty("username", username);
//            userData.addProperty("role", "BIDDER");
//            userData.addProperty("fullName", "Người Dùng Test");
//            response.add("userData", userData);
//
//            return gson.toJson(response);
//        } else {
//            return createErrorResponse("AUTH_FAILED", "Sai tên đăng nhập hoặc mật khẩu!");
//        }
//    }
//
//    private static String handleRegister(JsonObject request, Gson gson) {
//        String username = request.get("username").getAsString();
//
//        JsonObject response = new JsonObject();
//        response.addProperty("success", true);
//        response.addProperty("message", "Đăng ký thành công cho: " + username);
//
//        return gson.toJson(response);
//    }
//
//    private static String createErrorResponse(String errorCode, String message) {
//        JsonObject error = new JsonObject();
//        error.addProperty("success", false);
//        error.addProperty("message", message);
//        error.addProperty("errorCode", errorCode);
//
//        return new Gson().toJson(error);
//    }
//}