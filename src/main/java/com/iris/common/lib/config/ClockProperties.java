package com.iris.common.lib.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.clock")
public class ClockProperties {

    /**
     * IANA timezone ID used to create the application {@link java.time.Clock}.
     * Defaults to UTC if not specified.
     * Example: Africa/Nairobi, Europe/London, America/New_York
     */
    private String timezone = "UTC";
}
