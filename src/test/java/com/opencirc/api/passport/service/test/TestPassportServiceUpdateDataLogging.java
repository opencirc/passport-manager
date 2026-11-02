package com.opencirc.api.passport.service.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.opencirc.api.passport.adapter.PlatformAdapterFactory;
import com.opencirc.api.passport.config.AppProperties;
import com.opencirc.api.passport.dao.DatasheetRepository;
import com.opencirc.api.passport.dao.PassportRepository;
import com.opencirc.api.passport.enums.PassportLogAction;
import com.opencirc.api.passport.model.Datasheet;
import com.opencirc.api.passport.model.DatasheetDefinition;
import com.opencirc.api.passport.model.DatasheetDefinitionProperty;
import com.opencirc.api.passport.model.Passport;
import com.opencirc.api.passport.service.DatasheetDefinitionService;
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
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class TestPassportServiceUpdateDataLogging {

  @Mock private DatasheetRepository datasheetRepository;
  @Mock private DatasheetDefinitionService datasheetDefinitionService;
  @Mock private PassportRepository passportRepository;
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
            datasheetDefinitionService,
            passportRepository,
            platformAdapterFactory,
            objectMapper,
            appProperties,
            epdEnrichmentService,
            passportLogService);
  }

  @Test
  public void shouldEnrichOnlyAfterCommit() {
    prepareEnrichmentPassport();
    when(passportLogService.captureActor())
        .thenReturn(
            new PassportLogService.Actor(
                new com.opencirc.api.passport.dto.CreatedByDto("Editor", "editor@example.com"),
                "editor-id"));
    TestTransactionManager transactionManager = new TestTransactionManager();
    org.mockito.Mockito.doAnswer(
            invocation -> {
              org.junit.jupiter.api.Assertions.assertTrue(transactionManager.committed);
              return null;
            })
        .when(epdEnrichmentService)
        .enrich(eq("passport"), eq("https://example.com/epd"), eq("trigger"), any());

    new TransactionTemplate(transactionManager)
        .executeWithoutResult(
            status -> {
              passportService.updateData("passport", Map.of("trigger", "https://example.com/epd"));
              org.mockito.Mockito.verifyNoInteractions(epdEnrichmentService);
            });

    verify(epdEnrichmentService)
        .enrich(
            "passport",
            "https://example.com/epd",
            "trigger",
            new PassportLogService.Actor(
                new com.opencirc.api.passport.dto.CreatedByDto("Editor", "editor@example.com"),
                "editor-id"));
  }

  @Test
  public void shouldNotEnrichAfterRollback() {
    prepareEnrichmentPassport();
    when(passportLogService.captureActor())
        .thenReturn(
            new PassportLogService.Actor(
                new com.opencirc.api.passport.dto.CreatedByDto("Editor", "editor@example.com"),
                "editor-id"));
    new TransactionTemplate(new TestTransactionManager())
        .executeWithoutResult(
            status -> {
              passportService.updateData("passport", Map.of("trigger", "https://example.com/epd"));
              status.setRollbackOnly();
            });

    org.mockito.Mockito.verifyNoInteractions(epdEnrichmentService);
  }

  public void prepareEnrichmentPassport() {
    Passport passport = new Passport();
    passport.setId("passport");
    passport.setStatus(Passport.Status.ACTIVE);
    DatasheetDefinitionProperty property = new DatasheetDefinitionProperty();
    property.setId("trigger");
    property.setCode("referencetooriginalEPD");
    property.setGroupTag("GeneralInformation");
    Datasheet datasheet = datasheet("datasheet", null, property);
    passport.setDatasheets(Collections.singleton(datasheet));
    when(passportRepository.findPassport("passport", Passport.Status.ACTIVE))
        .thenReturn(Optional.of(passport));
  }

  /** Builds a datasheet instance whose shared definition declares the given properties. */
  private static Datasheet datasheet(
      String id, Map<String, Object> data, DatasheetDefinitionProperty... properties) {
    DatasheetDefinition definition = new DatasheetDefinition();
    definition.setId("definition-" + id);
    definition.setProperties(new HashSet<>(List.of(properties)));
    for (DatasheetDefinitionProperty property : properties) {
      property.setDatasheetDefinition(definition);
    }
    Datasheet datasheet = new Datasheet();
    datasheet.setId(id);
    datasheet.setDefinition(definition);
    datasheet.setData(data);
    datasheet.setDataCategory(Datasheet.DataCategory.GENERIC);
    return datasheet;
  }

  private static DatasheetDefinitionProperty property(String id) {
    DatasheetDefinitionProperty property = new DatasheetDefinitionProperty();
    property.setId(id);
    return property;
  }

  public static class TestTransactionManager extends AbstractPlatformTransactionManager {
    public boolean committed;

    @Override
    public Object doGetTransaction() {
      return new Object();
    }

    @Override
    public void doBegin(Object transaction, TransactionDefinition definition) {}

    @Override
    public void doCommit(DefaultTransactionStatus status) {
      committed = true;
    }

    @Override
    public void doRollback(DefaultTransactionStatus status) {}
  }

  @Test
  public void shouldLogUpdatePropertiesWhenDataIsChanged() {
    String passportId = "passport-123";
    String propId = "prop-1";
    Map<String, Object> values = Map.of(propId, "new-value");

    Passport passport = new Passport();
    passport.setId(passportId);
    passport.setStatus(Passport.Status.ACTIVE);
    passport.setDatasheets(
        Collections.singleton(datasheet("ds-1", Map.of(propId, "old-value"), property(propId))));

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
    passport.setDatasheets(
        Collections.singleton(datasheet("ds-1", Map.of(propId, "same-value"), property(propId))));

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
    passport.setDatasheets(
        new HashSet<>(
            List.of(
                datasheet("ds-1", Map.of("prop-1", "old-1"), property("prop-1")),
                datasheet("ds-2", Map.of("prop-2", "old-2"), property("prop-2")))));

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
