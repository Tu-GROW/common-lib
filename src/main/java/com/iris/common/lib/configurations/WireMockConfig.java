package com.iris.common.lib.configurations;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class WireMockConfig {

  @Value("${wiremock.server.port}")
  private int wireServerPort;

  @Bean(initMethod = "start", destroyMethod = "stop")
  public WireMockServer mockServer() {
    return new  WireMockServer(options().port(wireServerPort));
  }

}
