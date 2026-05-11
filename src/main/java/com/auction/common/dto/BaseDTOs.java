// common/dto/BaseDTOs.java
package com.auction.common.dto;

public class BaseDTOs {

    // --- Lớp cha cho mọi yêu cầu (Client -> Server) ---
    public static abstract class Request {
        private final String type; // Định danh dùng cho Gson (RuntimeTypeAdapterFactory)

        public Request(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    // --- Lớp cha cho mọi phản hồi (Server -> Client) ---
    public static abstract class Response {
        private final String type;       // Định danh dùng cho Gson phân loại Response
        private final int statusCode;    // Mã trạng thái (Dùng StatusCode, VD: 200, 400, 500)
        private final boolean success;   // Cờ báo hiệu thành công/thất bại
        private final String message;    // Lời nhắn thông báo

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
    }

    //  Lớp thông báo lỗi chung, kế thừa từ Response
    public static class ErrorResponse extends Response {
        private final String errorCode;  // Mã lỗi nghiệp vụ chi tiết (Dùng ErrorCode, VD: "ERR_INVALID_BID")

        public ErrorResponse(int statusCode, String message, String errorCode) {
            super("ERROR_RESPONSE", statusCode, false, message);
            this.errorCode = errorCode;
        }

        public String getErrorCode() { return errorCode; }
    }
}