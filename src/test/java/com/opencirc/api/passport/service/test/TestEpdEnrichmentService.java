package com.opencirc.api.passport.service.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencirc.api.passport.dao.DatasheetRepository;
import com.opencirc.api.passport.dao.PassportRepository;
import com.opencirc.api.passport.dto.CreatedByDto;
import com.opencirc.api.passport.enums.PassportLogAction;
import com.opencirc.api.passport.model.Datasheet;
import com.opencirc.api.passport.model.DatasheetProperty;
import com.opencirc.api.passport.model.Passport;
import com.opencirc.api.passport.model.PassportDatasheetMapping;
import com.opencirc.api.passport.service.EpdEnrichmentService;
import com.opencirc.api.passport.service.PassportLogService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
    var beanFactory = new org.springframework.beans.factory.support.DefaultListableBeanFactory();
    beanFactory.registerSingleton("epdEnrichmentService", epdEnrichmentService);
    epdEnrichmentService.enrichmentServiceProvider =
        beanFactory.getBeanProvider(EpdEnrichmentService.class);
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

  @Test
  public void shouldAttributeFailureToEditorRatherThanPassportCreator() {
    when(environment.getActiveProfiles()).thenReturn(new String[0]);
    Passport passport = passport("passport-1");
    passport.setCreatedById("owner-id");
    passport.setCreatedBy(new CreatedByDto("Owner", "owner@example.com"));
    when(passportRepository.findById("passport-1")).thenReturn(Optional.of(passport));
    PassportLogService.Actor actor =
        new PassportLogService.Actor(new CreatedByDto("Editor", "editor@example.com"), "editor-id");

    epdEnrichmentService.enrich("passport-1", "http://1.1.1.1/epd.json", "trigger", actor);

    verify(passportLogService)
        .logEvent(
            eq("passport-1"),
            eq(PassportLogAction.EPD_ENRICHMENT_FAILED),
            any(),
            eq(actor.createdBy()),
            eq("editor-id"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"http://localhost:8089/epd.json", "http://localhost:8089/new.json", ""})
  public void shouldApplyOnlyWhenReloadedTriggerStillMatches(String currentUrl) throws Exception {
    String expectedUrl = "http://localhost:8089/epd.json";
    Passport original = enrichmentPassport(expectedUrl);
    Passport current = enrichmentPassport(currentUrl);
    when(passportRepository.findById("passport-1"))
        .thenReturn(Optional.of(original), Optional.of(current));
    when(environment.getActiveProfiles()).thenReturn(new String[] {"test"});
    when(restTemplate.getForObject(anyString(), eq(com.fasterxml.jackson.databind.JsonNode.class)))
        .thenReturn(new ObjectMapper().readTree("{\"impacts\":{\"gwp\":{\"a1a3\":12.34}}}"));

    epdEnrichmentService.enrich(
        "passport-1", expectedUrl, "trigger", PassportLogService.systemActor());

    if (expectedUrl.equals(currentUrl)) {
      Datasheet datasheet = current.getDatasheetMappings().iterator().next().getDatasheet();
      verify(datasheetRepository).save(datasheet);
      assertEquals(new java.math.BigDecimal("12.34"), datasheet.getData().get("gwp"));
    } else {
      verify(datasheetRepository, never()).save(any());
      verify(passportRepository, never()).save(any());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"lcax", "ilcd"})
  public void shouldStoreNumericGwpForBothFormats(String format) throws Exception {
    Passport passport = enrichmentPassport("http://localhost:8089/epd.json");
    when(passportRepository.findById("passport-1")).thenReturn(Optional.of(passport));
    when(environment.getActiveProfiles()).thenReturn(new String[] {"test"});
    when(restTemplate.getForObject(anyString(), eq(com.fasterxml.jackson.databind.JsonNode.class)))
        .thenReturn(new ObjectMapper().readTree(gwpPayload(format, "123.45")));

    epdEnrichmentService.enrich(passport, "http://localhost:8089/epd.json");

    Datasheet datasheet = passport.getDatasheetMappings().iterator().next().getDatasheet();
    assertInstanceOf(java.math.BigDecimal.class, datasheet.getData().get("gwp"));
    assertEquals(new java.math.BigDecimal("123.45"), datasheet.getData().get("gwp"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"lcax", "ilcd"})
  public void shouldRejectInvalidNumericGwpForBothFormats(String format) throws Exception {
    Passport passport = enrichmentPassport("http://localhost:8089/epd.json");
    when(environment.getActiveProfiles()).thenReturn(new String[] {"test"});
    when(restTemplate.getForObject(anyString(), eq(com.fasterxml.jackson.databind.JsonNode.class)))
        .thenReturn(new ObjectMapper().readTree(gwpPayload(format, "NaN")));

    epdEnrichmentService.enrich(passport, "http://localhost:8089/epd.json");

    verify(datasheetRepository, never()).save(any());
    verify(passportLogService)
        .logEvent(
            eq("passport-1"),
            eq(PassportLogAction.EPD_ENRICHMENT_FAILED),
            any(),
            any(),
            eq("system"));
  }

  public String gwpPayload(String format, String value) {
    if ("lcax".equals(format)) {
      return "{\"impacts\":{\"gwp\":{\"a1a3\":\"" + value + "\"}}}";
    }
    return "{\"LCIAResults\":{\"LCIAResult\":[{\"referenceToLCIAMethodFlowProperty\":{"
        + "\"refObjectId\":\"a7ea142a-9749-11ed-a8fc-0242ac120002\"},"
        + "\"other\":{\"anies\":[{\"module\":\"A1-A3\",\"value\":\""
        + value
        + "\"}]}}]}}";
  }

  public Passport enrichmentPassport(String triggerUrl) {
    Passport passport = passport("passport-1");
    Datasheet datasheet = new Datasheet();
    datasheet.setId("datasheet");
    datasheet.setData(new HashMap<>(Map.of("trigger", triggerUrl)));
    DatasheetProperty trigger = new DatasheetProperty();
    trigger.setId("trigger");
    DatasheetProperty gwp = new DatasheetProperty();
    gwp.setId("gwp");
    gwp.setCode("ClimateChangePerUnit");
    gwp.setGroupTag("Pset_EnvironmentalImpactIndicators");
    datasheet.setDatasheetProperties(new HashSet<>(List.of(trigger, gwp)));
    PassportDatasheetMapping mapping = new PassportDatasheetMapping();
    mapping.setDatasheet(datasheet);
    passport.setDatasheetMappings(new HashSet<>(List.of(mapping)));
    return passport;
  }

  private static Passport passport(String id) {
    Passport passport = new Passport();
    passport.setId(id);
    return passport;
  }
}
