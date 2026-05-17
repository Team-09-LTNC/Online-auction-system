package com.auction.common.enums;

public class ActionType {

    // --- AUTH ---
    public static final String REGISTER      = "AUTH_REGISTER";
    public static final String LOGIN         = "AUTH_LOGIN";
    public static final String LOGOUT        = "AUTH_LOGOUT";

    // --- PRODUCT ---
    public static final String CREATE_PRODUCT    = "PRODUCT_CREATE";
    public static final String GET_ALL_PRODUCTS  = "PRODUCT_GET_ALL";
    public static final String GET_PRODUCT_BY_ID = "PRODUCT_GET_BY_ID";
    public static final String UPDATE_PRODUCT    = "PRODUCT_UPDATE";
    public static final String DELETE_PRODUCT    = "PRODUCT_DELETE";
    public static final String SEARCH_PRODUCT    = "PRODUCT_SEARCH";

    // --- AUCTION ---
    public static final String CREATE_AUCTION    = "AUCTION_CREATE";
    public static final String GET_ALL_AUCTIONS  = "AUCTION_GET_ALL";
    public static final String GET_AUCTION_BY_ID = "AUCTION_GET_BY_ID";
    public static final String JOIN_AUCTION      = "AUCTION_JOIN";
    public static final String LEAVE_AUCTION     = "AUCTION_LEAVE";
    public static final String CLOSE_AUCTION     = "AUCTION_CLOSE";
    public static final String PLACE_BID         = "AUCTION_BID_PLACE";
    public static final String GET_BID_HISTORY   = "AUCTION_BID_HISTORY";

    // --- SERVER PUSH (server chủ động gửi, không có requestId) ---
    public static final String AUCTION_BID_UPDATE = "AUCTION_BID_UPDATE";
    public static final String AUCTION_RESULT     = "AUCTION_RESULT";
}