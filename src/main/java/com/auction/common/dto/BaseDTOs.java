package com.auction.common.dto;

public class BaseDTOs {
    // Lớp cha cho mọi yêu cầu
    public static class Request {
        public String type; // Để phân loại yêu cầu tại ClientHandler
    }

    // Lớp cha cho mọi phản hồi
    public static class Response {
        public boolean success;
        public String message;
    }

    // Thông báo lỗi chung
    public static class ErrorResponse {
        public String errorCode;
        public String errorMessage;
    }
}