package com.opencirc.api.passport.service.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.opencirc.api.passport.adapter.PlatformAdapterFactory;
import com.opencirc.api.passport.auth.principal.ApiKeyPrincipal;
import com.opencirc.api.passport.dao.DatasheetRepository;
import com.opencirc.api.passport.dao.PassportRepository;
import com.opencirc.api.passport.enums.PassportLogAction;
import com.opencirc.api.passport.enums.Platform;
import com.opencirc.api.passport.model.AccessGroupPermissions;
import com.opencirc.api.passport.model.Datasheet;
import com.opencirc.api.passport.model.DatasheetDefinition;
import com.opencirc.api.passport.model.Passport;
import com.opencirc.api.passport.service.DatasheetDefinitionService;
import com.opencirc.api.passport.service.EpdEnrichmentService;
import com.opencirc.api.passport.service.PassportLogService;
import com.opencirc.api.passport.service.PassportService;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class TestPassportServiceStructureLogging {

  private static final String PROJECT_ID = "project-1";
  private static final ApiKeyPrincipal ACTOR =
      new ApiKeyPrincipal("key-1", "Key", "group-1", PROJECT_ID, AccessGroupPermissions.full());

  @Mock private DatasheetRepository datasheetRepository;
  @Mock private DatasheetDefinitionService datasheetDefinitionService;
  @Mock private PassportRepository passportRepository;
  @Mock private PlatformAdapterFactory platformAdapterFactory;
  @Mock private ObjectMapper objectMapper;
  @Mock private EpdEnrichmentService epdEnrichmentService;
  @Mock private PassportLogService passportLogService;

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
            epdEnrichmentService,
            passportLogService);
  }

  private static Passport activePassport(String id) {
    Passport passport = new Passport();
    passport.setId(id);
    passport.setProjectId(PROJECT_ID);
    passport.setStatus(Passport.Status.ACTIVE);
    passport.setDatasheets(new HashSet<>());
    return passport;
  }

  @Test
  public void shouldLogEventWhenDatasheetAdded() throws Exception {
    String passportId = "passport-123";
    Passport passport = activePassport(passportId);

    when(passportRepository.findById(passportId)).thenReturn(Optional.of(passport));

    Platform platform = Platform.BSDD;
    String platformId = "platform-abc";
    Datasheet.DataCategory category = Datasheet.DataCategory.GENERIC;

    DatasheetDefinition definition = new DatasheetDefinition();
    definition.setId("definition-789");
    definition.setPlatformId(platformId);
    definition.setName("Test Definition");
    when(datasheetDefinitionService.resolveDefinitions(platform, platformId, false))
        .thenReturn(List.of(definition));
    when(datasheetRepository.save(any(Datasheet.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    passportService.addDatasheetsToPassportUsingPlatform(
        passportId, platform, platformId, category, ACTOR);

    verify(passportLogService)
        .logEvent(eq(passportId), eq(PassportLogAction.ADD_DATASHEET), any(List.class));
    org.junit.jupiter.api.Assertions.assertEquals(1, passport.getDatasheets().size());
    Datasheet attached = passport.getDatasheets().iterator().next();
    org.junit.jupiter.api.Assertions.assertSame(definition, attached.getDefinition());
    org.junit.jupiter.api.Assertions.assertEquals("key-1", attached.getCreatedById());
  }

  @Test
  public void shouldNotAttachSameDefinitionTwice() throws Exception {
    String passportId = "passport-123";
    Passport passport = activePassport(passportId);

    DatasheetDefinition definition = new DatasheetDefinition();
    definition.setId("definition-789");
    definition.setPlatformId("platform-abc");

    Datasheet existing = new Datasheet();
    existing.setId("ds-1");
    existing.setDefinition(definition);
    passport.setDatasheets(new HashSet<>(List.of(existing)));

    when(passportRepository.findById(passportId)).thenReturn(Optional.of(passport));
    when(datasheetDefinitionService.resolveDefinitions(Platform.BSDD, "platform-abc", false))
        .thenReturn(List.of(definition));

    passportService.addDatasheetsToPassportUsingPlatform(
        passportId, Platform.BSDD, "platform-abc", Datasheet.DataCategory.GENERIC, ACTOR);

    verify(datasheetRepository, org.mockito.Mockito.never()).save(any());
    org.mockito.Mockito.verifyNoInteractions(passportLogService);
    org.junit.jupiter.api.Assertions.assertEquals(1, passport.getDatasheets().size());
  }

  @Test
  public void shouldRejectDatasheetAttachmentOutsideProject() {
    Passport foreign = activePassport("foreign");
    foreign.setProjectId("project-2");
    when(passportRepository.findById("foreign")).thenReturn(Optional.of(foreign));

    org.junit.jupiter.api.Assertions.assertThrows(
        com.opencirc.api.passport.exception.ForbiddenException.class,
        () ->
            passportService.addDatasheetsToPassportUsingPlatform(
                "foreign", Platform.BSDD, "platform-abc", Datasheet.DataCategory.GENERIC, ACTOR));
    org.mockito.Mockito.verifyNoInteractions(datasheetDefinitionService);
  }

  @Test
  public void shouldLogEventWhenDatasheetRemoved() {
    String passportId = "passport-123";
    String datasheetId = "ds-789";

    Passport passport = activePassport(passportId);

    DatasheetDefinition definition = new DatasheetDefinition();
    definition.setId("definition-1");
    definition.setName("Test Datasheet");

    Datasheet datasheet = new Datasheet();
    datasheet.setId(datasheetId);
    datasheet.setDefinition(definition);
    passport.setDatasheets(new HashSet<>(List.of(datasheet)));

    when(passportRepository.findById(passportId)).thenReturn(Optional.of(passport));

    passportService.removeDatasheet(passportId, datasheetId, ACTOR);

    verify(datasheetRepository).delete(datasheet);
    verify(passportLogService)
        .logEvent(eq(passportId), eq(PassportLogAction.REMOVE_DATASHEET), any(List.class));
    org.junit.jupiter.api.Assertions.assertTrue(passport.getDatasheets().isEmpty());
  }

  @Test
  public void shouldRejectSelfParenting() {
    Passport passport = activePassport("passport");
    when(passportRepository.findById("passport")).thenReturn(Optional.of(passport));

    org.junit.jupiter.api.Assertions.assertThrows(
        com.opencirc.api.passport.exception.InvalidInputException.class,
        () -> passportService.updateParent("passport", "passport", ACTOR));

    org.mockito.Mockito.verify(passportRepository, org.mockito.Mockito.never()).save(any());
    org.mockito.Mockito.verifyNoInteractions(passportLogService);
  }

  @Test
  public void shouldRejectDescendantAsParent() {
    Passport passport = activePassport("ancestor");
    when(passportRepository.findById("ancestor")).thenReturn(Optional.of(passport));
    when(passportRepository.getParentId("grandchild")).thenReturn("child");
    when(passportRepository.getParentId("child")).thenReturn("ancestor");

    org.junit.jupiter.api.Assertions.assertThrows(
        com.opencirc.api.passport.exception.InvalidInputException.class,
        () -> passportService.updateParent("ancestor", "grandchild", ACTOR));

    org.mockito.Mockito.verify(passportRepository, org.mockito.Mockito.never()).save(any());
    org.mockito.Mockito.verifyNoInteractions(passportLogService);
  }

  @Test
  public void shouldRejectAnAlreadyCyclicParentChain() {
    Passport passport = activePassport("passport");
    when(passportRepository.findById("passport")).thenReturn(Optional.of(passport));
    when(passportRepository.getParentId("first-parent")).thenReturn("second-parent");
    when(passportRepository.getParentId("second-parent")).thenReturn("first-parent");

    org.junit.jupiter.api.Assertions.assertThrows(
        com.opencirc.api.passport.exception.InvalidInputException.class,
        () -> passportService.updateParent("passport", "first-parent", ACTOR));

    org.mockito.Mockito.verify(passportRepository, org.mockito.Mockito.never()).save(any());
  }

  @Test
  public void shouldRejectDetachingFromParent() {
    Passport passport = activePassport("passport");
    passport.setParentId("old-parent");
    when(passportRepository.findById("passport")).thenReturn(Optional.of(passport));

    org.junit.jupiter.api.Assertions.assertThrows(
        org.springframework.web.server.ResponseStatusException.class,
        () -> passportService.updateParent("passport", "", ACTOR));

    org.mockito.Mockito.verify(passportRepository, org.mockito.Mockito.never()).save(any());
  }

  @Test
  public void shouldLogEventWhenParentUpdated() {
    String passportId = "passport-123";
    String oldParentId = "old-parent";
    String newParentId = "new-parent";

    Passport passport = activePassport(passportId);
    passport.setParentId(oldParentId);

    when(passportRepository.findById(passportId)).thenReturn(Optional.of(passport));
    when(passportRepository.findPassport(newParentId, Passport.Status.ACTIVE))
        .thenReturn(Optional.of(activePassport(newParentId)));
    when(passportRepository.save(any(Passport.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    passportService.updateParent(passportId, newParentId, ACTOR);

    verify(passportRepository).save(passport);
    verify(passportLogService)
        .logEvent(eq(passportId), eq(PassportLogAction.UPDATE_RELATIONSHIPS), any(List.class));
  }
}
