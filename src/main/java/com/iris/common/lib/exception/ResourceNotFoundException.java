package com.iris.common.lib.exception;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends Exception{

  private final String customerMessage;
  private final String responseDesc;

  public ResourceNotFoundException(String customerMessage, String responseDesc) {
    this.customerMessage = customerMessage;
    this.responseDesc = responseDesc;
  }
}
