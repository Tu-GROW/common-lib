package com.iris.common.lib.configurations;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.iris.common.lib.utils.Helpers.convertObjectToJson;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WireMockStub {

  private final WireMockConfig wireMockServer;
  private final String endpoint;
  private final int status;
  private final String response;
  private final Object payload;

  public WireMockStub(
      WireMockConfig wireMockServer,
      String endpoint,
      int status,
      String response,
      Object payload) {
    this.wireMockServer = wireMockServer;
    this.endpoint = endpoint;
    this.status = status;
    this.response = response;
    this.payload = payload;

    configureWiremockStub();
  }

  /**
   * Create a wiremock configuration to dynamically stub any endpoint
   */
  private void configureWiremockStub() {
    try{

      configureFor("localhost",wireMockServer.mockServer().port());
      wireMockServer.mockServer().start();
      if(Objects.isNull(this.payload)){
        log.debug("Stubbing without payload");
        wireMockServer.mockServer().stubFor(get(urlEqualTo(this.endpoint))
            .willReturn(aResponse()
                .withStatus(this.status)
                .withHeader("Content-Type", "application/json")
                .withBody(this.response)
            ));
      }else{
        log.debug("Stubbing with payload");
        wireMockServer.mockServer().stubFor(get(urlEqualTo(this.endpoint))
            .withRequestBody(equalToJson(convertObjectToJson(this.payload)))
            .willReturn(aResponse()
                .withStatus(this.status)
                .withHeader("Content-Type", "application/json")
                .withBody(this.response)
            ));
      }

    }catch (Exception e){
      throw new RuntimeException(e.getMessage());
    }
  }
}
