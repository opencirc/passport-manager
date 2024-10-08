package com.oc.api.passport.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.ToString;

@Component
@ConfigurationProperties(prefix = "company")
@Data
@ToString
public class CompanyConfig {

    private String name;
    
}
