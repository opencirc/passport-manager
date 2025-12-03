package com.opencirc.api.passport.helper.test;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@TestConfiguration
public class TestConfig {

  /** RestTemplate Bean. */
  @Bean(name = "testRestTemplate")
  @Primary
  public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder.build();
  }
}
