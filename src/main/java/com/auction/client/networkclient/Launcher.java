package com.auction.client.networkclient;

import com.auction.client.ClientApplication;

public class Launcher {
    public static void main(String[] args) {
        // Ép chuẩn encoding ngay từ lớp khởi động
        System.setProperty("file.encoding", "UTF-8");

        // Gọi ClientApplication
        ClientApplication.main(args);
    }
}