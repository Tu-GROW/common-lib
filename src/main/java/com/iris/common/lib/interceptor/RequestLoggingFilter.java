package com.iris.common.lib.interceptor;

import com.iris.common.lib.utils.DataMasker;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

@Component
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, 0);

        chain.doFilter(wrappedRequest, response);

        byte[] bodyBytes = wrappedRequest.getContentAsByteArray();
        if (bodyBytes.length > 0) {
            String body = new String(bodyBytes, wrappedRequest.getCharacterEncoding());
            log.info("Request Payload: {}", DataMasker.maskJson(body));
        }
    }
}
