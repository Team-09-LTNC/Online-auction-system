package com.auction.client;

import com.auction.client.network.NetworkManager;
import com.auction.common.dto.AuthDTOs;

public class ClientApplication {
    public static void main(String[] args) {
        System.out.println("=== TEST ĐĂNG NHẬP THẬT ===");

        // Kết nối
        NetworkManager.connect();

        // Đợi kết nối ổn định
        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        // Test 1: Đăng nhập SAI
        System.out.println("\n>>> Test 1: Đăng nhập với thông tin SAI");
        AuthDTOs.LoginRequest wrongLogin = new AuthDTOs.LoginRequest("sai_user", "sai_pass");
        NetworkManager.sendRequest(wrongLogin);

        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        // Test 2: Đăng nhập ĐÚNG
        System.out.println("\n>>> Test 2: Đăng nhập với thông tin ĐÚNG");
        AuthDTOs.LoginRequest correctLogin = new AuthDTOs.LoginRequest("test", "123");
        NetworkManager.sendRequest(correctLogin);

        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        // Test 3: Đăng ký
        System.out.println("\n>>> Test 3: Đăng ký tài khoản mới");
        AuthDTOs.RegisterRequest registerRequest = new AuthDTOs.RegisterRequest(
                "newuser", "newpass", "Người Mới", "BIDDER"
        );
        NetworkManager.sendRequest(registerRequest);

        // Giữ kết nối
        System.out.println("\n>>> Đợi phản hồi từ Server...");
        try { Thread.sleep(10000); } catch (InterruptedException e) {}

        System.out.println("\n=== KẾT THÚC TEST ===");
    }
}