package com.iris.common.dtos.response;


import java.io.Serializable;
import lombok.Builder;
import lombok.ToString;

@ToString
@Builder
public class Header implements Serializable {
  int responseCode;
  String responseDesc;
  String customerMessage;
}
