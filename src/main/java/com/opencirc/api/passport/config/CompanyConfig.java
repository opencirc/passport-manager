package com.opencirc.api.passport.config;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "company")
@Data
@ToString
public class CompanyConfig {

  /** Company name. */
  private String name;
}
