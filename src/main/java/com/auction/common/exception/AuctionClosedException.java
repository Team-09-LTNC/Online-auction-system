package com.auction.common.exception;

/**
 * Ngoại lệ được ném khi phiên đấu giá đã đóng.
 */
public class AuctionClosedException extends Exception {
  public AuctionClosedException(String msg) {
    super(msg);
  }
}
