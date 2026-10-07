package com.iris.common.lib.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {

  private final int errorCode;
  private final String customerMessage;
  private final String responseDesc;
  private final Integer httpStatusCode;

  public ApiException(String message, int errorCode, String customerMessage, String responseDesc, Integer httpStatusCode) {
    super(message);
    this.errorCode = errorCode;
    this.customerMessage = customerMessage;
    this.responseDesc = responseDesc;
    if (httpStatusCode != null) {
      this.httpStatusCode = httpStatusCode;
    }else{
        this.httpStatusCode = HttpStatus.OK.value();
    }
  }

  public ApiException(String message, int errorCode, String customerMessage, String responseDesc) {
    super(message);
    this.errorCode = errorCode;
    this.customerMessage = customerMessage;
    this.responseDesc = responseDesc;
    this.httpStatusCode = HttpStatus.OK.value();
  }
}
