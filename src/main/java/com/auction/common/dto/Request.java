package com.auction.common.dto;

public abstract class Request {
    // type để phân loại gói tin ví dụ nhu : LOGIN,LOGOUT,....
    private String type;

    public Request(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}