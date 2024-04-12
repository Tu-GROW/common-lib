package com.iris.common.lib.aop;

import com.iris.common.lib.dtos.response.Header;
import com.iris.common.lib.dtos.response.ResponseMapper;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ExecutionTimeAspect {

  @Around("@within(org.springframework.web.bind.annotation.RestController) || @annotation(ExecutionTime)")
  public Object addExecutionTimeToResponse(ProceedingJoinPoint joinPoint)
      throws Throwable {
    log.debug("Adding execution time");
    long startTime = System.currentTimeMillis();

    Object proceed = joinPoint.proceed();

    long executionTime = System.currentTimeMillis() - startTime;

    if (proceed instanceof ResponseEntity) {
      ResponseEntity<ResponseMapper<?>> previousResponseEntity = (ResponseEntity<ResponseMapper<?>>) proceed;
      var header = Objects.requireNonNull(previousResponseEntity.getBody()).header();
      Header newHeader = Header.builder()
          .executionTime(executionTime)
          .responseCode(header.responseCode())
          .errors(header.errors())
          .responseDesc(header.responseDesc())
          .customerMessage(header.customerMessage())
          .build();

      return ResponseEntity.status(previousResponseEntity.getStatusCode())
          .body(ResponseMapper.builder()
              .header(newHeader)
              .body(previousResponseEntity.getBody().body())
              .build());
    } else {
      log.warn("Cannot add Execution time to non ResponseEntity Object");
      return proceed;
    }
  }

}
