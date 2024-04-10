package com.iris.common.dtos.response;

import lombok.Builder;
import lombok.ToString;

@Builder
public record ResponseMapper<T>(Header header, T body){

}
