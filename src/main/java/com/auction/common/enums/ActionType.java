package com.auction.common.enums;

public class ActionType {

    // --- AUTH & USER ---
    public static final String REGISTER = "AUTH_REGISTER";
    public static final String LOGIN = "AUTH_LOGIN";
    public static final String LOGOUT = "AUTH_LOGOUT";
    public static final String TOP_UP_MONEY = "USER_TOP_UP";
    public static final String WITHDRAW_MONEY = "USER_WITHDRAW";

    // --- PRODUCT ---
    public static final String CREATE_PRODUCT = "CREATE_PRODUCT";
    public static final String GET_ALL_PRODUCTS = "PRODUCT_GET_ALL";
    public static final String GET_PRODUCT_BY_ID = "PRODUCT_GET_BY_ID";
    public static final String UPDATE_PRODUCT = "PRODUCT_UPDATE";
    public static final String DELETE_PRODUCT = "PRODUCT_DELETE";
    public static final String SEARCH_PRODUCT = "PRODUCT_SEARCH";
    public static final String GET_MY_PRODUCTS = "GET_MY_PRODUCTS";
    // --- AUCTION ---
    public static final String CREATE_AUCTION = "AUCTION_CREATE";
    public static final String GET_ALL_AUCTIONS = "AUCTION_GET_ALL";
    public static final String GET_JOINED_AUCTIONS = "AUCTION_GET_JOINED";
    public static final String GET_AUCTION_BY_ID = "AUCTION_GET_BY_ID";
    public static final String JOIN_AUCTION      = "AUCTION_JOIN";
    public static final String LEAVE_AUCTION     = "AUCTION_LEAVE";
    public static final String CLOSE_AUCTION     = "AUCTION_CLOSE";
    public static final String PLACE_BID         = "AUCTION_BID_PLACE";
    public static final String CONFIRM_BUY_NOW   = "AUCTION_BUY_NOW_CONFIRM";
    public static final String SETTLE_BUY_NOW    = "AUCTION_BUY_NOW_SETTLE";
    public static final String REGISTER_AUTO_BID = "AUCTION_REGISTER_AUTO_BID";
    public static final String GET_BID_HISTORY = "AUCTION_BID_HISTORY";
    public static final String GET_DASHBOARD_STATS = "USER_DASHBOARD_STATS";

    // --- FOLLOW (Theo dõi) ---
    public static final String FOLLOW_AUCTION = "AUCTION_FOLLOW";
    public static final String UNFOLLOW_AUCTION = "AUCTION_UNFOLLOW";
    public static final String GET_FOLLOWED_AUCTIONS = "AUCTION_GET_FOLLOWED";

    // --- CHAT REAL-TIME ---
    public static final String SEND_CHAT_MESSAGE = "CHAT_SEND_MESSAGE";
    public static final String GET_SYSTEM_NOTIFICATIONS = "CHAT_GET_SYSTEM_NOTIFICATIONS";
    public static final String MARK_SYSTEM_NOTIFICATIONS_READ = "CHAT_MARK_SYSTEM_NOTIFICATIONS_READ";

    // --- SERVER PUSH (server chủ động gửi, không có requestId) ---
    public static final String AUCTION_BID_UPDATE = "AUCTION_BID_UPDATE";
    public static final String AUCTION_RESULT = "AUCTION_RESULT";
    public static final String RECEIVE_CHAT_MESSAGE = "CHAT_RECEIVE_MESSAGE";
    // --- Admin ---
    public static final String ADMIN_GET_ALL_BIDDERS = "ADMIN_GET_ALL_BIDDERS";
    public static final String ADMIN_GET_ALL_SELLERS = "ADMIN_GET_ALL_SELLERS";
    public static final String ADMIN_GET_ALL_AUCTIONS = "ADMIN_GET_ALL_AUCTIONS";
    public static final String ADMIN_TOGGLE_LOCK_USER = "ADMIN_TOGGLE_LOCK_USER";
    public static final String ADMIN_GET_PENDING_AUCTIONS = "ADMIN_GET_PENDING_AUCTIONS";
    public static final String ADMIN_APPROVE_AUCTION = "ADMIN_APPROVE_AUCTION";
    public static final String ADMIN_REJECT_AUCTION = "ADMIN_REJECT_AUCTION";
    public static final String ADMIN_GET_INVOICES = "ADMIN_GET_INVOICES";
    public static final String ADMIN_CHANGE_AUCTION_STATUS = "ADMIN_CHANGE_AUCTION_STATUS";
}