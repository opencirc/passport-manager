package com.opencirc.api.passport.service.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencirc.api.passport.dao.DatasheetRepository;
import com.opencirc.api.passport.dao.PassportRepository;
import com.opencirc.api.passport.model.Passport;
import com.opencirc.api.passport.service.EpdEnrichmentService;
import com.opencirc.api.passport.service.PassportLogService;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

/** Verifies transaction boundaries through the actual Spring service proxy. */
public class TestEpdEnrichmentTransactions {

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void shouldFetchWithoutTransactionAndApplyIndependently(boolean persistenceFails)
      throws Exception {
    RestTemplate restTemplate = mock(RestTemplate.class);
    PassportRepository passportRepository = mock(PassportRepository.class);
    DatasheetRepository datasheetRepository = mock(DatasheetRepository.class);
    PassportLogService passportLogService = mock(PassportLogService.class);
    DataSource dataSource = mock(DataSource.class);
    List<Connection> connections = new ArrayList<>();
    when(dataSource.getConnection())
        .thenAnswer(
            invocation -> {
              Connection connection = mock(Connection.class);
              when(connection.getAutoCommit()).thenReturn(true);
              connections.add(connection);
              return connection;
            });
    DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
    Passport passport = new Passport();
    passport.setId("passport");
    JsonNode payload = new ObjectMapper().readTree("{\"format\":\"lcax\",\"name\":\"Product\"}");
    when(restTemplate.getForObject(anyString(), eq(JsonNode.class)))
        .thenAnswer(
            invocation -> {
              assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
              assertFalse(TransactionSynchronizationManager.hasResource(dataSource));
              return payload;
            });
    when(passportRepository.findById("passport"))
        .thenAnswer(
            invocation -> {
              assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
              return Optional.of(passport);
            });
    when(passportRepository.save(passport))
        .thenAnswer(
            invocation -> {
              assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
              if (persistenceFails) {
                throw new IllegalStateException("Persistence failed");
              }
              return passport;
            });

    new ApplicationContextRunner()
        .withUserConfiguration(TransactionConfiguration.class, EpdEnrichmentService.class)
        .withInitializer(context -> context.getEnvironment().setActiveProfiles("test"))
        .withBean("epdRestTemplate", RestTemplate.class, () -> restTemplate)
        .withBean(PassportRepository.class, () -> passportRepository)
        .withBean(DatasheetRepository.class, () -> datasheetRepository)
        .withBean(PassportLogService.class, () -> passportLogService)
        .withBean(DataSourceTransactionManager.class, () -> transactionManager)
        .run(
            context -> {
              EpdEnrichmentService service = context.getBean(EpdEnrichmentService.class);
              new TransactionTemplate(transactionManager)
                  .executeWithoutResult(
                      status -> {
                        Object outerResource =
                            TransactionSynchronizationManager.getResource(dataSource);
                        service.enrichPassport(
                            passport,
                            "http://localhost/epd",
                            null,
                            PassportLogService.systemActor());
                        assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
                        assertSame(
                            outerResource,
                            TransactionSynchronizationManager.getResource(dataSource));
                      });
              verify(passportRepository).findById("passport");
              verify(passportRepository).save(passport);
              verify(connections.get(0)).commit();
              if (persistenceFails) {
                verify(connections.get(1)).rollback();
                verify(connections.get(1), never()).commit();
                verify(passportLogService)
                    .logEvent(eq("passport"), any(), any(), any(), eq("system"));
              } else {
                verify(connections.get(1)).commit();
              }
            });
  }

  @Configuration
  @EnableTransactionManagement
  public static class TransactionConfiguration {}
}
