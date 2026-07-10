package com.iris.common.lib.exception;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class NotificationServerException extends RuntimeException {

    private final HttpStatusCode status;
    private final String body;

    public NotificationServerException(HttpStatusCode status, String body) {
        super("Notification server error [" + status + "]: " + body);
        this.status = status;
        this.body = body;
    }

    public NotificationServerException(String message, Throwable cause) {
        super(message, cause);
        this.status = null;
        this.body = null;
    }
}
