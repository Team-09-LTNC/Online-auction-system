package com.auction.common.exception;

/**
 * Ngoại lệ được ném khi giá đặt không hợp lệ.
 */
public class InvalidBidException extends Exception {
  public InvalidBidException(String message) {
    super(message);
  }
}
