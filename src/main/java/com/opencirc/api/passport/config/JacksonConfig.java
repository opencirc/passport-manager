package com.opencirc.api.passport.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;

@Configuration
public class JacksonConfig {

  /** Configures Jackson to handle Java time types and serialize dates as ISO-8601 strings. */
  @Bean
  public JsonMapperBuilderCustomizer jsonCustomizer() {
    return builder -> {
      builder.disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS);
      // Jackson 3 fails on null for primitives by default; spring.jackson.use-jackson2-defaults
      // does not cover this flag, and upstream APIs such as bsdd send explicit nulls.
      builder.disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
    };
  }
}
