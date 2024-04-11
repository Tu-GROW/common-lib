package com.iris.common.lib.dtos.response;


import java.io.Serializable;
import lombok.Builder;
import lombok.ToString;

@Builder
public class Header implements Serializable {
  int responseCode;
  String responseDesc;
  String customerMessage;
}
