package com.opencirc.api.passport.config;

import java.time.Duration;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/** Rest Template configuration. */
@Configuration
public class RestConfig {

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration CONNECTION_REQUEST_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(15);

  public final RestTemplateBuilder restTemplateBuilder;

  /** Uses Boot's builder so the templates share the auto-configured JsonMapper. */
  public RestConfig(RestTemplateBuilder restTemplateBuilder) {
    this.restTemplateBuilder = restTemplateBuilder;
  }

  /** Rest Template Bean Initialisation. */
  @Bean
  public RestTemplate restTemplate() {
    return createRestTemplate(true);
  }

  /** EPD client that does not follow redirects to unvalidated destinations. */
  @Bean
  public RestTemplate epdRestTemplate() {
    return createRestTemplate(false);
  }

  /** Creates an HTTP client with bounded timeouts and the requested redirect policy. */
  public RestTemplate createRestTemplate(boolean redirectsEnabled) {
    PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
    connectionManager.setMaxTotal(10);
    connectionManager.setDefaultMaxPerRoute(10);

    RequestConfig requestConfig =
        RequestConfig.custom()
            .setRedirectsEnabled(redirectsEnabled)
            .setConnectTimeout(Timeout.of(CONNECT_TIMEOUT))
            .setConnectionRequestTimeout(Timeout.of(CONNECTION_REQUEST_TIMEOUT))
            .setResponseTimeout(Timeout.of(RESPONSE_TIMEOUT))
            .build();

    CloseableHttpClient httpClient =
        HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .build();

    HttpComponentsClientHttpRequestFactory factory =
        new HttpComponentsClientHttpRequestFactory(httpClient);
    // Built through Boot's builder so the auto-configured JsonMapper (and its Jackson 2 era
    // defaults) is used for response parsing instead of a bare Spring default mapper.
    return restTemplateBuilder.requestFactory(() -> factory).build();
  }
}
