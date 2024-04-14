package com.iris.common.lib.dtos.response;


import java.io.Serializable;
import java.util.List;
import lombok.Builder;

@Builder
public record Header(
    int responseCode,
    String responseDesc,
    String customerMessage,
    List<?> errors,
    long executionTime
    ) implements Serializable{

}
