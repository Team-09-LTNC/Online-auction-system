package com.auction.common.exception;

/**
 * Exception được throw khi phiên đấu giá đã đóng.
 */
public class AuctionClosedException extends Exception {
  public AuctionClosedException(String msg) {
    super(msg);
  }
}
