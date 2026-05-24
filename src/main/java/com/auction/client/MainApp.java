package com.auction.client;

import com.auction.client.networkclient.ClientApplication;

public class MainApp {
    public static void main(String[] args) {
        // Ép chuẩn encoding ngay từ lớp khởi động
        System.setProperty("file.encoding", "UTF-8");

        // Gọi ClientApplication
        ClientApplication.main(args);
    }
}