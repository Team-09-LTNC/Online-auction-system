package com.auction.common.dto;

public class BaseDTOs {
    // Lớp cha cho mọi yêu cầu gửi lên Server
    public static abstract class Request {
        private final String type; // Định danh dùng cho RuntimeTypeAdapterFactory

        public Request(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    // Lớp cha cho mọi phản hồi từ Server
    public static abstract class Response {
        private final boolean success;
        private final String message;

        public Response(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }

    // Thông báo lỗi chung, kế thừa từ Response
    public static class ErrorResponse extends Response {
        private final String errorCode;

        public ErrorResponse(String message, String errorCode) {
            super(false, message);
            this.errorCode = errorCode;
        }

        public String getErrorCode() { return errorCode; }
    }
}