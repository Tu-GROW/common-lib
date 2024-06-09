package com.iris.common.lib.enums;

import lombok.Getter;

@Getter
public enum MdcKeys {
  REQUEST_ID("requestId"),
  START_TIME("startTime");

  private final String key;
  MdcKeys(String key) {
    this.key = key;
  }
}
