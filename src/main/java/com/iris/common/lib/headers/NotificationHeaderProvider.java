package com.iris.common.lib.headers;

import com.iris.common.lib.dtos.request.NotificationEvent;

import java.util.Map;

@FunctionalInterface
public interface NotificationHeaderProvider {
    Map<String, String> resolve(NotificationEvent event);
}
