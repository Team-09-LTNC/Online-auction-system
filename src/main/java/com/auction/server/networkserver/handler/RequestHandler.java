package com.auction.server.networkserver.handler;

import com.auction.server.networkserver.ClientHandler;
import com.google.gson.JsonObject;

/**
 * RequestHandler: Giao diện (Interface) cho các lớp xử lý yêu cầu từ Client.
 * Nhiệm vụ chính: Tiếp nhận gói tin JSON và thực thi nghiệp vụ tương ứng.
 */
public interface RequestHandler {
    /**
     * Phương thức thực hiện xử lý yêu cầu nghiệp vụ.
     * @param yeuCau Đối tượng JsonObject chứa dữ liệu từ Client.
     * @param client Tham chiếu đến ClientHandler đang quản lý kết nối này.
     * @return Chuỗi JSON phản hồi lại cho Client.
     */
    String xuLy(JsonObject yeuCau, ClientHandler client);
}