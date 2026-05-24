package com.auction.common.exception;

/**
 * Exception được throw khi xác thực thất bại.
 */
public class AuthenticationException extends Exception {
  public AuthenticationException(String msg) {
    super(msg);
  }
}
