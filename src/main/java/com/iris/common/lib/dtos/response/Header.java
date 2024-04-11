package com.iris.common.lib.dtos.response;


import java.io.Serializable;
import lombok.Builder;

@Builder
public record Header(
    int responseCode,
    String responseDesc,
    String customerMessage) implements Serializable{

}
