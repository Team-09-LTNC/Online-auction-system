package com.auction.client.controller.auth;

/**
 * UserSession: Quản lý phiên làm việc tập trung tại client.
 */
public class UserSession {
    private static int userId;
    private static String username;
    private static String currentRole;

    public static int getUserId() {
        return userId;
    }

    public static void setUserId(int id) {
        userId = id;
    }

    public static String getUsername() {
        return username;
    }

    public static void setUsername(String name) {
        username = name;
    }

    public static String getCurrentRole() {
        return currentRole;
    }

    public static void setCurrentRole(String role) {
        currentRole = role;
    }

    public static void clear() {
        userId = 0;
        username = null;
        currentRole = null;
    }
}
