package com.auction.common.exception;

/**
 * Exception được throw khi bid không hợp lệ.
 */
public class InvalidBidException extends Exception {
  public InvalidBidException(String message) {
    super(message);
  }
}
