package com.opencirc.api.passport.service.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.opencirc.api.passport.adapter.PlatformAdapterFactory;
import com.opencirc.api.passport.auth.principal.ApiKeyPrincipal;
import com.opencirc.api.passport.context.ActorContext;
import com.opencirc.api.passport.dao.AuditLogRepository;
import com.opencirc.api.passport.dao.AuditLogTagRepository;
import com.opencirc.api.passport.dao.DatasheetRepository;
import com.opencirc.api.passport.dao.PassportRepository;
import com.opencirc.api.passport.enums.AuditLogAction;
import com.opencirc.api.passport.enums.AuditTargetType;
import com.opencirc.api.passport.model.AccessGroupPermissions;
import com.opencirc.api.passport.model.AuditLog;
import com.opencirc.api.passport.model.Datasheet;
import com.opencirc.api.passport.model.DatasheetDefinition;
import com.opencirc.api.passport.model.DatasheetDefinitionProperty;
import com.opencirc.api.passport.model.Passport;
import com.opencirc.api.passport.service.AuditLogService;
import com.opencirc.api.passport.service.DatasheetDefinitionService;
import com.opencirc.api.passport.service.EpdEnrichmentService;
import com.opencirc.api.passport.service.JsonDiffService;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Property updates are audited as RFC 6902 diffs and EPD enrichment runs only after commit. */
@ExtendWith(MockitoExtension.class)
public class TestPassportServiceUpdateDataLogging {

  private static final ApiKeyPrincipal ACTOR =
      new ApiKeyPrincipal("key-1", "Key", "group-1", "project-1", AccessGroupPermissions.full());

  @Mock private DatasheetRepository datasheetRepository;
  @Mock private DatasheetDefinitionService datasheetDefinitionService;
  @Mock private PassportRepository passportRepository;
  @Mock private PlatformAdapterFactory platformAdapterFactory;
  @Mock private EpdEnrichmentService epdEnrichmentService;
  @Mock private AuditLogRepository auditLogRepository;
  @Mock private AuditLogTagRepository auditLogTagRepository;
  @Mock private ActorContext actorContext;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private PassportService passportService;

  @BeforeEach
  public void setUp() {
    lenient().when(actorContext.currentApiKey()).thenReturn(Optional.of(ACTOR));
    lenient()
        .when(auditLogRepository.save(any(AuditLog.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    AuditLogService auditLogService =
        new AuditLogService(
            auditLogRepository,
            auditLogTagRepository,
            new JsonDiffService(objectMapper),
            actorContext,
            objectMapper);
    passportService =
        new PassportService(
            datasheetRepository,
            datasheetDefinitionService,
            passportRepository,
            platformAdapterFactory,
            objectMapper,
            epdEnrichmentService,
            auditLogService);
  }

  @Test
  public void shouldEnrichOnlyAfterCommit() {
    prepareEnrichmentPassport();
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
              passportService.updateData(
                  "passport", Map.of("trigger", "https://example.com/epd"), ACTOR);
              org.mockito.Mockito.verifyNoInteractions(epdEnrichmentService);
            });

    verify(epdEnrichmentService)
        .enrich(
            "passport", "https://example.com/epd", "trigger", new AuditLogService.Actor("key-1"));
  }

  @Test
  public void shouldNotEnrichAfterRollback() {
    prepareEnrichmentPassport();
    new TransactionTemplate(new TestTransactionManager())
        .executeWithoutResult(
            status -> {
              passportService.updateData(
                  "passport", Map.of("trigger", "https://example.com/epd"), ACTOR);
              status.setRollbackOnly();
            });

    org.mockito.Mockito.verifyNoInteractions(epdEnrichmentService);
  }

  public void prepareEnrichmentPassport() {
    Passport passport = new Passport();
    passport.setId("passport");
    passport.setProjectId("project-1");
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
  public void shouldAuditPropertyChangeAsJsonPatch() {
    String passportId = "passport-123";
    String propId = "prop-1";
    Map<String, Object> values = Map.of(propId, "new-value");

    Passport passport = new Passport();
    passport.setId(passportId);
    passport.setProjectId("project-1");
    passport.setStatus(Passport.Status.ACTIVE);
    passport.setDatasheets(
        Collections.singleton(datasheet("ds-1", Map.of(propId, "old-value"), property(propId))));

    when(passportRepository.findPassport(passportId, Passport.Status.ACTIVE))
        .thenReturn(Optional.of(passport));

    passportService.updateData(passportId, values, ACTOR);

    ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
    verify(auditLogRepository).save(captor.capture());

    AuditLog log = captor.getValue();
    assertEquals(AuditTargetType.PASSPORT_DATASHEET, log.getTargetType());
    assertEquals("ds-1", log.getTargetId());
    assertEquals("project-1", log.getProjectId());
    assertEquals(passportId, log.getPassportId());
    assertEquals(AuditLogAction.UPDATE, log.getAction());
    assertEquals("key-1", log.getCreatedById());
    assertEquals(1, log.getDiff().size());
    JsonNode operation = log.getDiff().get(0);
    assertEquals("replace", operation.get("op").asText());
    assertEquals("/data/prop-1", operation.get("path").asText());
    assertEquals("new-value", operation.get("value").asText());
  }

  @Test
  public void shouldNotAuditWhenDataIsSame() {
    String passportId = "passport-123";
    String propId = "prop-1";
    Map<String, Object> values = Map.of(propId, "same-value");

    Passport passport = new Passport();
    passport.setId(passportId);
    passport.setProjectId("project-1");
    passport.setStatus(Passport.Status.ACTIVE);
    passport.setDatasheets(
        Collections.singleton(datasheet("ds-1", Map.of(propId, "same-value"), property(propId))));

    when(passportRepository.findPassport(passportId, Passport.Status.ACTIVE))
        .thenReturn(Optional.of(passport));

    passportService.updateData(passportId, values, ACTOR);

    verify(auditLogRepository, never()).save(any());
    verify(datasheetRepository, never()).save(any());
  }

  @Test
  public void shouldAuditEachChangedDatasheetSeparately() {
    String passportId = "passport-123";
    Map<String, Object> values = Map.of("prop-1", "new-1", "prop-2", "new-2");

    Passport passport = new Passport();
    passport.setId(passportId);
    passport.setProjectId("project-1");
    passport.setStatus(Passport.Status.ACTIVE);
    passport.setDatasheets(
        new HashSet<>(
            List.of(
                datasheet("ds-1", Map.of("prop-1", "old-1"), property("prop-1")),
                datasheet("ds-2", Map.of("prop-2", "old-2"), property("prop-2")))));

    when(passportRepository.findPassport(passportId, Passport.Status.ACTIVE))
        .thenReturn(Optional.of(passport));

    passportService.updateData(passportId, values, ACTOR);

    ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
    verify(auditLogRepository, org.mockito.Mockito.times(2)).save(captor.capture());

    List<AuditLog> logs = captor.getAllValues();
    AuditLog first =
        logs.stream().filter(l -> "ds-1".equals(l.getTargetId())).findFirst().orElseThrow();
    AuditLog second =
        logs.stream().filter(l -> "ds-2".equals(l.getTargetId())).findFirst().orElseThrow();
    assertEquals("/data/prop-1", first.getDiff().get(0).get("path").asText());
    assertEquals("new-1", first.getDiff().get(0).get("value").asText());
    assertEquals("/data/prop-2", second.getDiff().get(0).get("path").asText());
    assertEquals("new-2", second.getDiff().get(0).get("value").asText());
    assertTrue(logs.stream().allMatch(l -> l.getAction() == AuditLogAction.UPDATE));
  }
}
