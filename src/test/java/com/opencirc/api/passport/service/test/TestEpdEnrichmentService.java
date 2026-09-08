package com.opencirc.api.passport.service.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.opencirc.api.passport.dao.DatasheetRepository;
import com.opencirc.api.passport.dao.PassportRepository;
import com.opencirc.api.passport.enums.PassportLogAction;
import com.opencirc.api.passport.model.Passport;
import com.opencirc.api.passport.service.EpdEnrichmentService;
import com.opencirc.api.passport.service.PassportLogService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
public class TestEpdEnrichmentService {

  @Mock private RestTemplate restTemplate;
  @Mock private DatasheetRepository datasheetRepository;
  @Mock private Environment environment;
  @Mock private PassportRepository passportRepository;
  @Mock private PassportLogService passportLogService;

  private EpdEnrichmentService epdEnrichmentService;

  @BeforeEach
  public void setUp() {
    epdEnrichmentService =
        new EpdEnrichmentService(
            restTemplate, datasheetRepository, environment, passportRepository, passportLogService);
  }

  @Test
  public void shouldRejectHttpUrlOutsideLocalProfiles() {
    when(environment.getActiveProfiles()).thenReturn(new String[0]);
    Passport passport = passport("passport-1");

    epdEnrichmentService.enrich(passport, "http://1.1.1.1/epd.json");

    verify(restTemplate, never()).getForObject(anyString(), any());
  }

  @Test
  public void shouldRejectLocalhostOutsideLocalProfiles() {
    when(environment.getActiveProfiles()).thenReturn(new String[0]);
    Passport passport = passport("passport-1");

    epdEnrichmentService.enrich(passport, "http://localhost:8089/epd.json");

    verify(restTemplate, never()).getForObject(anyString(), any());
  }

  @Test
  public void shouldRejectPrivateAndMetadataAddresses() {
    when(environment.getActiveProfiles()).thenReturn(new String[0]);
    Passport passport = passport("passport-1");

    epdEnrichmentService.enrich(passport, "https://10.0.0.1/epd.json");
    epdEnrichmentService.enrich(passport, "https://169.254.169.254/latest/meta-data");
    epdEnrichmentService.enrich(passport, "https://user:pass@1.1.1.1/epd.json");
    epdEnrichmentService.enrich(passport, "https://1.1.1.1:8080/epd.json");

    verify(restTemplate, never()).getForObject(anyString(), any());
  }

  @Test
  public void shouldRejectLocalhostSubstringBypass() {
    when(environment.getActiveProfiles()).thenReturn(new String[0]);
    Passport passport = passport("passport-1");

    epdEnrichmentService.enrich(passport, "http://1.1.1.1/?x=localhost");

    verify(restTemplate, never()).getForObject(anyString(), any());
  }

  @Test
  public void shouldAllowLoopbackHttpInTestProfile() {
    when(environment.getActiveProfiles()).thenReturn(new String[] {"test"});
    Passport passport = passport("passport-1");

    epdEnrichmentService.enrich(passport, "http://localhost:8089/epd.json");

    verify(restTemplate).getForObject(anyString(), any());
  }

  @Test
  public void shouldFetchPublicHttpsUrl() {
    when(environment.getActiveProfiles()).thenReturn(new String[0]);
    Passport passport = passport("passport-1");

    epdEnrichmentService.enrich(passport, "https://1.1.1.1/epd.json");

    verify(restTemplate).getForObject(anyString(), any());
  }

  @Test
  public void shouldLogFailedEnrichmentWhenUrlIsUnsafe() {
    when(environment.getActiveProfiles()).thenReturn(new String[0]);
    Passport passport = passport("passport-1");

    epdEnrichmentService.enrich(passport, "http://1.1.1.1/epd.json");

    verify(passportLogService)
        .logEvent(
            eq("passport-1"), eq(PassportLogAction.EPD_ENRICHMENT_FAILED), any(), any(), any());
  }

  @Test
  public void shouldEnrichByPassportId() {
    when(environment.getActiveProfiles()).thenReturn(new String[] {"test"});
    Passport passport = passport("passport-1");
    when(passportRepository.findById("passport-1")).thenReturn(Optional.of(passport));

    epdEnrichmentService.enrich("passport-1", "http://localhost:8089/epd.json");

    verify(restTemplate).getForObject(anyString(), any());
  }

  @Test
  public void shouldNotEnrichWhenPassportNotFound() {
    when(passportRepository.findById("passport-nonexistent")).thenReturn(Optional.empty());

    epdEnrichmentService.enrich("passport-nonexistent", "http://localhost:8089/epd.json");

    verify(restTemplate, never()).getForObject(anyString(), any());
  }

  private static Passport passport(String id) {
    Passport passport = new Passport();
    passport.setId(id);
    return passport;
  }
}
