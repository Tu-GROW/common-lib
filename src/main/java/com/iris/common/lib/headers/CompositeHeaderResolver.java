package com.iris.common.lib.headers;

import com.iris.common.lib.dtos.request.NotificationEvent;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class CompositeHeaderResolver implements NotificationHeaderProvider {

    private final List<NotificationHeaderProvider> providers;

    @Override
    public Map<String, String> resolve(NotificationEvent event) {
        Map<String, String> headers = new LinkedHashMap<>();
        providers.forEach(p -> headers.putAll(p.resolve(event)));
        return headers;
    }
}
