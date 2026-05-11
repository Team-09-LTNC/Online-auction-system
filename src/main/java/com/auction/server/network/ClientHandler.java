//package com.auction.server.network;
//
//import com.auction.common.dto.BaseDTOs;
//import com.google.gson.Gson;
//import java.io.*;
//import java.net.Socket;
//import java.nio.charset.StandardCharsets;
//
//public class ClientHandler implements Runnable {
//    private final Socket clientSocket;
//    private final Gson gson;
//
//    public ClientHandler(Socket clientSocket) {
//        this.clientSocket = clientSocket;
//        this.gson = new Gson();
//    }
//
//    @Override
//    public void run() {
//        try (
//                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
//                PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true)
//        ) {
//            String jsonLine;
//            // Liên tục lắng nghe DTO từ Client gửi lên
//            while ((jsonLine = in.readLine()) != null) {
//                System.out.println("[Server Received] " + jsonLine);
//
//                // Tạm thời gửi lại một phản hồi thành công để kiểm tra (Ping - Pong)
//                BaseDTOs.Response testResponse = new BaseDTOs.Response(true, "Server đã nhận được gói tin của bạn!") {};
//                out.println(gson.toJson(testResponse));
//            }
//        } catch (IOException e) {
//            System.err.println("[Server Error] Client ngắt kết nối: " + e.getMessage());
//        }
//    }
//}