package com.opencirc.api.passport.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registers the access group permission check on the machine-facing passport endpoints. */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  private final AccessGroupPermissionInterceptor accessGroupPermissionInterceptor;

  /** Constructor. */
  public WebMvcConfig(AccessGroupPermissionInterceptor accessGroupPermissionInterceptor) {
    this.accessGroupPermissionInterceptor = accessGroupPermissionInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(accessGroupPermissionInterceptor).addPathPatterns("/api/passport/**");
  }
}
