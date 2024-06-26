package com.iris.common.lib.interceptor;


import com.iris.common.lib.dtos.response.Header;
import com.iris.common.lib.dtos.response.ResponseMapper;
import com.iris.common.lib.enums.MdcKeys;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice
@Slf4j
public class HttpResponseDecorator implements ResponseBodyAdvice<Object> {

  @Override
  public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
    return returnType.getContainingClass().isAnnotationPresent(RestController.class);
  }

  @Override
  public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
      Class<? extends HttpMessageConverter<?>> selectedConverterType,
      ServerHttpRequest request, ServerHttpResponse response) {

    log.info("Response Payload: {} ", body);

    if (body instanceof ResponseEntity<?> || body instanceof ResponseMapper<?>) {
      return  body;
    }
    return decorateToAtlasEnvelope(body);
  }

  private ResponseMapper<?> decorateToAtlasEnvelope(Object body) {

    long executionTime = System.currentTimeMillis() - Long.parseLong(MDC.get(MdcKeys.START_TIME.getKey()));
    var header = Header.builder()
        .customerMessage("Success")
        .responseDesc("Success")
        .responseCode(HttpStatus.OK.value())
        .executionTime(executionTime)
        .build();

    return  ResponseMapper.builder()
        .header(header)
        .body(body)
        .build();
  }
}
