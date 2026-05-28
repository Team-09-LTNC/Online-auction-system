package com.auction.common.exception;

/**
 * Ngoại lệ được ném khi xác thực thất bại.
 */
public class AuthenticationException extends Exception {
  public AuthenticationException(String msg) {
    super(msg);
  }
}
