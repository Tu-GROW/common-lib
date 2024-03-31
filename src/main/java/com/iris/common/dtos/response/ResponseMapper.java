package com.iris.common.dtos.response;

import lombok.Builder;
import lombok.ToString;

@Builder
@ToString
public record ResponseMapper<T>(Header header, T body){

}
