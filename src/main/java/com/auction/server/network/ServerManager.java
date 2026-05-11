//package com.auction.server.network;
//
//import java.io.IOException;
//import java.net.ServerSocket;
//import java.net.Socket;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//public class ServerManager {
//    private final int port;
//    // Thread Pool quản lý tối đa 50 kết nối đồng thời
//    private final ExecutorService clientPool = Executors.newFixedThreadPool(50);
//
//    public ServerManager(int port) {
//        this.port = port;
//    }
//
//    public void batDauServer() {
//        try (ServerSocket serverSocket = new ServerSocket(port)) {
//            while (true) {
//                // Chặn (block) cho đến khi có một Client kết nối
//                Socket clientSocket = serverSocket.accept();
//                System.out.println("[Server] Có Client mới kết nối từ IP: " + clientSocket.getInetAddress());
//
//                // Đẩy công việc xử lý Client này cho Thread Pool
//                clientPool.execute(new ClientHandler(clientSocket));
//            }
//        } catch (IOException e) {
//            System.err.println("[Server Error] Lỗi khi mở cổng kết nối: " + e.getMessage());
//        }
//    }
//}