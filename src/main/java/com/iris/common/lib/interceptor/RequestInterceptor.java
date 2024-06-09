package com.iris.common.lib.interceptor;

import com.iris.common.lib.enums.MdcKeys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Class intercepts every request and generate a request id if non was provided. The request id alongside the request
 * start time are saved in the MDC context.
 */
@Slf4j
public class RequestInterceptor implements HandlerInterceptor {

  private static final String X_REQUEST_ID = "X-Request-Id";

  @Override
  public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response,
      @NonNull Object handler) {

    String requestId = request.getHeader(X_REQUEST_ID);
    MDC.put(MdcKeys.START_TIME.getKey(), String.valueOf(System.currentTimeMillis()));
    if (requestId == null || requestId.isEmpty()) {
      requestId = UUID.randomUUID().toString();
      log.info("New incoming requestId: {} ", requestId);
    }else{
      log.info("New call for requestId: {}", requestId);
    }

    MDC.put(MdcKeys.REQUEST_ID.getKey(), requestId);
    return true;
  }

  @Override
  public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
      @NonNull Object handler, Exception ex) {
    log.info("RequestId {} completed in {} ms", MDC.get(MdcKeys.REQUEST_ID.getKey()), MdcKeys.START_TIME.getKey());

    MDC.remove(MdcKeys.REQUEST_ID.getKey());
    MDC.remove(MdcKeys.START_TIME.getKey());

  }
}
