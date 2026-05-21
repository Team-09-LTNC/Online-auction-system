package com.auction.common.dto;

public class AdminDTOs {
    public static class UserSummaryDTO {
    private final String username;
    private final String fullname;
    private final String status;
    
    public UserSummaryDTO(String username, String fullname, String status) {
        this.username = username;
        this.fullname = fullname;
        this.status = status;
    }

    public String getUsername() {
        return username;
    }
    public String getFullname() {
        return fullname;
    }
    public String getStatus() {
        return status;
    }
}
}
