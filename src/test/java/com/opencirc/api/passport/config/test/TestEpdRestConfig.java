package com.opencirc.api.passport.config.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencirc.api.passport.adapter.bsdd.BsddPlatformAdapter;
import com.opencirc.api.passport.config.AppProperties;
import com.opencirc.api.passport.config.RestConfig;
import com.opencirc.api.passport.dao.DatasheetRepository;
import com.opencirc.api.passport.dao.PassportRepository;
import com.opencirc.api.passport.helper.test.TestConfig;
import com.opencirc.api.passport.service.CacheService;
import com.opencirc.api.passport.service.EpdEnrichmentService;
import com.opencirc.api.passport.service.PassportLogService;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.util.ReflectionTestUtils;

/** Tests redirect handling using the actual configured HTTP clients. */
public class TestEpdRestConfig {

  @Test
  public void shouldWireSeparateClientsAndPreserveTestOverrides() {
    ApplicationContextRunner contextRunner =
        new ApplicationContextRunner()
            .withUserConfiguration(
                RestConfig.class, BsddPlatformAdapter.class, EpdEnrichmentService.class)
            .withBean(AppProperties.class, () -> mock(AppProperties.class))
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(CacheService.class, () -> mock(CacheService.class))
            .withBean(DatasheetRepository.class, () -> mock(DatasheetRepository.class))
            .withBean(PassportRepository.class, () -> mock(PassportRepository.class))
            .withBean(PassportLogService.class, () -> mock(PassportLogService.class));
    contextRunner.run(
        context -> {
          assertSame(
              context.getBean("restTemplate"),
              ReflectionTestUtils.getField(
                  context.getBean(BsddPlatformAdapter.class), "restTemplate"));
          assertSame(
              context.getBean("epdRestTemplate"),
              ReflectionTestUtils.getField(
                  context.getBean(EpdEnrichmentService.class), "restTemplate"));
        });
    contextRunner
        .withUserConfiguration(TestConfig.class)
        .withBean(RestTemplateBuilder.class, RestTemplateBuilder::new)
        .run(
            context -> {
              assertSame(
                  context.getBean("testRestTemplate"),
                  ReflectionTestUtils.getField(
                      context.getBean(BsddPlatformAdapter.class), "restTemplate"));
              assertSame(
                  context.getBean("epdRestTemplate"),
                  ReflectionTestUtils.getField(
                      context.getBean(EpdEnrichmentService.class), "restTemplate"));
            });
  }

  @Test
  public void shouldRejectRedirectsOnlyForEpdRequests() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    AtomicInteger destinationRequests = new AtomicInteger();
    server.createContext(
        "/redirect",
        exchange -> {
          exchange.getResponseHeaders().add("Location", "/destination");
          exchange.sendResponseHeaders(302, -1);
          exchange.close();
        });
    server.createContext(
        "/destination",
        exchange -> {
          destinationRequests.incrementAndGet();
          exchange.sendResponseHeaders(200, -1);
          exchange.close();
        });
    server.start();
    try {
      String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/redirect";
      RestConfig configuration = new RestConfig();

      var epdResponse = configuration.epdRestTemplate().getForEntity(url, String.class);
      assertTrue(epdResponse.getStatusCode().is3xxRedirection());
      assertEquals(0, destinationRequests.get());

      var regularResponse = configuration.restTemplate().getForEntity(url, String.class);
      assertTrue(regularResponse.getStatusCode().is2xxSuccessful());
      assertEquals(1, destinationRequests.get());
    } finally {
      server.stop(0);
    }
  }
}
