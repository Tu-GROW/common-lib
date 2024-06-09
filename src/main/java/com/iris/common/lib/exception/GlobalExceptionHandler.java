package com.iris.common.lib.exception;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.iris.common.lib.dtos.response.Header;
import com.iris.common.lib.dtos.response.ResponseMapper;
import com.iris.common.lib.enums.MdcKeys;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

/**
 * Global exception handler. Will return custom response when the service encounters runtime/custom defined exceptions.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler({Exception.class, RuntimeException.class, NullPointerException.class})
  public ResponseEntity<ResponseMapper<?>> globalExceptionHandler(Exception ex, WebRequest request) {
    log.error(
        String.format("Unhandled Service Exception for Request %s RequestId %s", request.getDescription(true),
            MDC.get(MdcKeys.REQUEST_ID.getKey())),
        ex);

    var header = Header.builder().responseCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
        .customerMessage("Request was not processed successfully")
        .responseDesc(ex.getMessage())
        .executionTime(getExecutionTime())
        .build();
    var response = ResponseMapper.builder()
        .header(header)
        .body(null)
        .build();

    return ResponseEntity.internalServerError().body(response);

  }

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ResponseMapper<?>> apiException(ApiException exception){
    log.error("Error: {}", exception.getMessage(), exception);
    var header = Header.builder()
        .customerMessage(exception.getCustomerMessage())
        .responseDesc(exception.getResponseDesc())
        .responseCode(exception.getErrorCode())
        .executionTime(getExecutionTime())
        .build();
    var response = ResponseMapper.builder()
        .header(header)
        .body(null)
        .build();
    return ResponseEntity.ok(response);
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ResponseMapper<?>> notFoundException(ResourceNotFoundException exception){
    log.error("Error: {}", exception.getMessage(), exception);
    var header = Header.builder()
        .customerMessage(exception.getCustomerMessage())
        .responseDesc(exception.getResponseDesc())
        .responseCode(HttpStatus.NOT_FOUND.value())
        .executionTime(getExecutionTime())
        .build();
    var response = ResponseMapper.builder()
        .header(header)
        .body(null)
        .build();
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
  }

  private String getJsonPropertyName(BeanPropertyBindingResult bindingResult, String fieldName) {
    Field field;
    try {
      field = bindingResult.getTarget().getClass().getDeclaredField(fieldName);
    } catch (NoSuchFieldException | NullPointerException e) {
      return fieldName;
    }
    JsonProperty jsonPropertyAnnotation = field.getAnnotation(JsonProperty.class);
    return (jsonPropertyAnnotation != null) ? jsonPropertyAnnotation.value() : fieldName;
  }

  private static long getExecutionTime() {
    return (System.currentTimeMillis() - Long.parseLong(MDC.get(MdcKeys.START_TIME.getKey())));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  protected ResponseEntity<ResponseMapper<?>> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception) {
    BeanPropertyBindingResult bindingResult = (BeanPropertyBindingResult) exception.getBindingResult();

    List<String> errors = new ArrayList<>();
    bindingResult.getFieldErrors().forEach(error -> {
      String fieldName = getJsonPropertyName(bindingResult, error.getField());
      String message = error.getDefaultMessage();
      errors.add(String.format("%s:  %s", fieldName, message));
    });

    var header = Header.builder()
        .customerMessage("Failed validation")
        .responseDesc("Failed validation")
        .responseCode(HttpStatus.BAD_REQUEST.value())
        .errors(errors)
        .executionTime(getExecutionTime())
        .build();

    return ResponseEntity.badRequest()
        .body(ResponseMapper.builder()
            .header(header)
            .body(null)
            .build());
  }
}
