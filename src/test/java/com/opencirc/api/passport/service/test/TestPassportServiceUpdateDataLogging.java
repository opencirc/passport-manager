package com.opencirc.api.passport.service.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencirc.api.passport.adapter.PlatformAdapterFactory;
import com.opencirc.api.passport.config.AppProperties;
import com.opencirc.api.passport.dao.DatasheetRepository;
import com.opencirc.api.passport.dao.PassportDatasheetMappingRepository;
import com.opencirc.api.passport.dao.PassportRepository;
import com.opencirc.api.passport.enums.PassportLogAction;
import com.opencirc.api.passport.model.Datasheet;
import com.opencirc.api.passport.model.DatasheetProperty;
import com.opencirc.api.passport.model.Passport;
import com.opencirc.api.passport.model.PassportDatasheetMapping;
import com.opencirc.api.passport.service.EpdEnrichmentService;
import com.opencirc.api.passport.service.PassportLogService;
import com.opencirc.api.passport.service.PassportService;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TestPassportServiceUpdateDataLogging {

  @Mock private DatasheetRepository datasheetRepository;
  @Mock private PassportRepository passportRepository;
  @Mock private PassportDatasheetMappingRepository passportDatasheetMappingRepository;
  @Mock private PlatformAdapterFactory platformAdapterFactory;
  @Mock private AppProperties appProperties;
  @Mock private EpdEnrichmentService epdEnrichmentService;
  @Mock private PassportLogService passportLogService;

  private ObjectMapper objectMapper = new ObjectMapper();
  private PassportService passportService;

  @BeforeEach
  public void setUp() {
    passportService =
        new PassportService(
            datasheetRepository,
            passportRepository,
            passportDatasheetMappingRepository,
            platformAdapterFactory,
            objectMapper,
            appProperties,
            epdEnrichmentService,
            passportLogService);
  }

  @Test
  public void shouldLogUpdatePropertiesWhenDataIsChanged() {
    String passportId = "passport-123";
    String propId = "prop-1";
    Map<String, Object> values = Map.of(propId, "new-value");

    Passport passport = new Passport();
    passport.setId(passportId);
    passport.setStatus(Passport.Status.ACTIVE);

    Datasheet datasheet = new Datasheet();
    datasheet.setId("ds-1");
    datasheet.setData(Map.of(propId, "old-value"));
    datasheet.setDataCategory(Datasheet.DataCategory.GENERIC);

    DatasheetProperty property = new DatasheetProperty();
    property.setId(propId);
    property.setDatasheet(datasheet);
    datasheet.setDatasheetProperties(new HashSet<>(List.of(property)));

    PassportDatasheetMapping mapping = new PassportDatasheetMapping();
    mapping.setDatasheet(datasheet);
    passport.setDatasheetMappings(Collections.singleton(mapping));

    when(passportRepository.findPassport(passportId, Passport.Status.ACTIVE))
        .thenReturn(Optional.of(passport));

    passportService.updateData(passportId, values);

    ArgumentCaptor<List<Map<String, Object>>> captor = ArgumentCaptor.forClass(List.class);
    verify(passportLogService)
        .logEvent(eq(passportId), eq(PassportLogAction.UPDATE_PROPERTIES), captor.capture());

    List<Map<String, Object>> changes = captor.getValue();
    assertEquals(1, changes.size());
    Map<String, Object> diff = changes.get(0);
    assertEquals(propId, diff.get("propertyId"));
    assertEquals("ds-1", diff.get("datasheetId"));
    assertEquals("old-value", diff.get("old"));
    assertEquals("new-value", diff.get("new"));
  }

  @Test
  public void shouldNotLogUpdatePropertiesWhenDataIsSame() {
    String passportId = "passport-123";
    String propId = "prop-1";
    Map<String, Object> values = Map.of(propId, "same-value");

    Passport passport = new Passport();
    passport.setId(passportId);
    passport.setStatus(Passport.Status.ACTIVE);

    Datasheet datasheet = new Datasheet();
    datasheet.setId("ds-1");
    datasheet.setData(Map.of(propId, "same-value"));
    datasheet.setDataCategory(Datasheet.DataCategory.GENERIC);

    DatasheetProperty property = new DatasheetProperty();
    property.setId(propId);
    property.setDatasheet(datasheet);
    datasheet.setDatasheetProperties(new HashSet<>(List.of(property)));

    PassportDatasheetMapping mapping = new PassportDatasheetMapping();
    mapping.setDatasheet(datasheet);
    passport.setDatasheetMappings(Collections.singleton(mapping));

    when(passportRepository.findPassport(passportId, Passport.Status.ACTIVE))
        .thenReturn(Optional.of(passport));

    passportService.updateData(passportId, values);

    verify(passportLogService, never()).logEvent(anyString(), any(), any());
  }

  @Test
  public void shouldCaptureMultiplePropertyChangesAcrossDatasheets() {
    String passportId = "passport-123";
    Map<String, Object> values = Map.of("prop-1", "new-1", "prop-2", "new-2");

    Passport passport = new Passport();
    passport.setId(passportId);
    passport.setStatus(Passport.Status.ACTIVE);

    // Datasheet 1
    Datasheet ds1 = new Datasheet();
    ds1.setId("ds-1");
    ds1.setData(Map.of("prop-1", "old-1"));
    ds1.setDataCategory(Datasheet.DataCategory.GENERIC);
    DatasheetProperty p1 = new DatasheetProperty();
    p1.setId("prop-1");
    p1.setDatasheet(ds1);
    ds1.setDatasheetProperties(new HashSet<>(List.of(p1)));

    // Datasheet 2
    Datasheet ds2 = new Datasheet();
    ds2.setId("ds-2");
    ds2.setData(Map.of("prop-2", "old-2"));
    ds2.setDataCategory(Datasheet.DataCategory.GENERIC);
    DatasheetProperty p2 = new DatasheetProperty();
    p2.setId("prop-2");
    p2.setDatasheet(ds2);
    ds2.setDatasheetProperties(new HashSet<>(List.of(p2)));

    PassportDatasheetMapping m1 = new PassportDatasheetMapping();
    m1.setId("m1");
    m1.setDatasheet(ds1);
    PassportDatasheetMapping m2 = new PassportDatasheetMapping();
    m2.setId("m2");
    m2.setDatasheet(ds2);
    passport.setDatasheetMappings(new HashSet<>(List.of(m1, m2)));

    when(passportRepository.findPassport(passportId, Passport.Status.ACTIVE))
        .thenReturn(Optional.of(passport));

    passportService.updateData(passportId, values);

    ArgumentCaptor<List<Map<String, Object>>> captor = ArgumentCaptor.forClass(List.class);
    verify(passportLogService)
        .logEvent(eq(passportId), eq(PassportLogAction.UPDATE_PROPERTIES), captor.capture());

    List<Map<String, Object>> changes = captor.getValue();
    assertEquals(2, changes.size());

    Map<String, Object> diff1 =
        changes.stream()
            .filter(c -> c.get("propertyId").equals("prop-1"))
            .findFirst()
            .orElseThrow();
    assertEquals("old-1", diff1.get("old"));
    assertEquals("new-1", diff1.get("new"));
    assertEquals("ds-1", diff1.get("datasheetId"));

    Map<String, Object> diff2 =
        changes.stream()
            .filter(c -> c.get("propertyId").equals("prop-2"))
            .findFirst()
            .orElseThrow();
    assertEquals("old-2", diff2.get("old"));
    assertEquals("new-2", diff2.get("new"));
    assertEquals("ds-2", diff2.get("datasheetId"));
  }
}
