package com.iris.common.lib.exception;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class NotificationClientException extends RuntimeException {

    private final HttpStatusCode status;
    private final String body;

    public NotificationClientException(HttpStatusCode status, String body) {
        super("Notification client error [" + status + "]: " + body);
        this.status = status;
        this.body = body;
    }
}
