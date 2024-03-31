package com.iris.common.dtos.response;

import lombok.Builder;

@Builder
public record ResponseMapper<T>(Header header, T body){

}
