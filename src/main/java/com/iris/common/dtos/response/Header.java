package com.iris.common.dtos.response;


import java.io.Serializable;
import lombok.Builder;

@Builder
public class Header implements Serializable {
  int responseCode;
  String responseDesc;
  String customerMessage;
}
