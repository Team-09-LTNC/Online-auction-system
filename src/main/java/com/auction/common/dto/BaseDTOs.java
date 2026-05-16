package com.auction.common.dto;

import java.util.UUID;

public class BaseDTOs {

    // --- Lớp cha cho mọi yêu cầu (Client -> Server) ---
    public static abstract class Request {
        private final String type;

        // [TỐI ƯU KIẾN TRÚC] Định danh duy nhất để tránh Race Condition khi gọi Callback.
        // Khởi tạo trực tiếp để không ảnh hưởng đến code cũ trên Server.
        private String requestId = UUID.randomUUID().toString();

        public Request(String type) {
            this.type = type;
        }

        public String getType() { return type; }
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
    }

    // --- Lớp cha cho mọi phản hồi (Server -> Client) ---
    public static abstract class Response {
        private final String type;
        private final int statusCode;
        private final boolean success;
        private final String message;

        // [TỐI ƯU KIẾN TRÚC] Client sẽ dựa vào ID này để tìm đúng Callback tương ứng.
        private String requestId;

        public Response(String type, int statusCode, boolean success, String message) {
            this.type = type;
            this.statusCode = statusCode;
            this.success = success;
            this.message = message;
        }

        public String getType() { return type; }
        public int getStatusCode() { return statusCode; }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }

        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
    }

    // Lớp thông báo lỗi chung
    public static class ErrorResponse extends Response {
        private final String errorCode;

        public ErrorResponse(int statusCode, String message, String errorCode) {
            super("ERROR_RESPONSE", statusCode, false, message);
            this.errorCode = errorCode;
        }

        public String getErrorCode() { return errorCode; }
    }
}