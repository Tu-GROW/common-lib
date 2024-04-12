package com.iris.common.lib.exception;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {

  private final int errorCode;
  private final String customerMessage;
  private final String responseDesc;

  public ApiException(String message, int errorCode, String customerMessage, String responseDesc) {
    super(message);
    this.errorCode = errorCode;
    this.customerMessage = customerMessage;
    this.responseDesc = responseDesc;
  }
}
