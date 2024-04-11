package com.iris.common.lib.exception;

import com.iris.common.lib.dtos.response.Header;
import com.iris.common.lib.dtos.response.ResponseMapper;
import com.iris.common.lib.enums.MdcKeys;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

/**
 * Global exception handler. Will return custom response when the service encounters runtime/custom defined exceptions.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ResponseMapper<?>> globalExceptionHandler(Exception ex, WebRequest request) {
    log.error(
        String.format("Unhandled Service Exception for Request %s RequestId %s", request.getDescription(true),
            MDC.get(MdcKeys.REQUEST_ID.getKey())),
        ex);

    Header header = Header.builder().responseCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
        .customerMessage("Request was not processed successfully")
        .responseDesc(ex.getMessage())
        .build();
    var response = ResponseMapper.builder()
        .header(header)
        .body(null)
        .build();

    return ResponseEntity.internalServerError().body(response);

  }
}
