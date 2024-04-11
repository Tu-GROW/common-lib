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
    log.info("New incoming request {} ", requestId);
    if (requestId == null || requestId.isEmpty()) {
      requestId = UUID.randomUUID().toString();
    }

    MDC.put(MdcKeys.REQUEST_ID.getKey(), requestId);
    MDC.put(MdcKeys.START_TIME.getKey(), String.valueOf(System.currentTimeMillis()));
    return true;
  }

  @Override
  public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
      @NonNull Object handler, Exception ex) {
    log.info("Request {} completed", MDC.get(MdcKeys.REQUEST_ID.getKey()));
    MDC.remove(MdcKeys.REQUEST_ID.getKey());
    MDC.remove(MdcKeys.START_TIME.getKey());

  }
}
