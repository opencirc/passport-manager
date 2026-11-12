package com.opencirc.api.passport.service.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.opencirc.api.passport.adapter.PlatformAdapterFactory;
import com.opencirc.api.passport.auth.principal.ApiKeyPrincipal;
import com.opencirc.api.passport.dao.DatasheetRepository;
import com.opencirc.api.passport.dao.PassportRepository;
import com.opencirc.api.passport.dto.CreatePassportUsingPlatformRequestDto;
import com.opencirc.api.passport.dto.PassportDto;
import com.opencirc.api.passport.dto.query.PassportDatasheetResultMapQueryResult;
import com.opencirc.api.passport.enums.AuditLogAction;
import com.opencirc.api.passport.enums.AuditTargetType;
import com.opencirc.api.passport.enums.Platform;
import com.opencirc.api.passport.exception.ForbiddenException;
import com.opencirc.api.passport.model.AccessGroupPermissions;
import com.opencirc.api.passport.model.Passport;
import com.opencirc.api.passport.service.AuditLogService;
import com.opencirc.api.passport.service.DatasheetDefinitionService;
import com.opencirc.api.passport.service.EpdEnrichmentService;
import com.opencirc.api.passport.service.PassportService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class TestPassportService {

  private static final String PROJECT_ID = "project-1";
  private static final String ROOT_ID = "root-id";
  private static final ApiKeyPrincipal ACTOR =
      new ApiKeyPrincipal("key-1", "Key", "group-1", PROJECT_ID, AccessGroupPermissions.full());

  @Mock private DatasheetRepository datasheetRepository;
  @Mock private DatasheetDefinitionService datasheetDefinitionService;
  @Mock private PassportRepository passportRepository;
  @Mock private PlatformAdapterFactory platformAdapterFactory;
  @Mock private ObjectMapper objectMapper;
  @Mock private EpdEnrichmentService epdEnrichmentService;
  @Mock private AuditLogService auditLogService;

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
            auditLogService);
  }

  private static Passport activePassport(String id) {
    Passport passport = new Passport();
    passport.setId(id);
    passport.setProjectId(PROJECT_ID);
    passport.setStatus(Passport.Status.ACTIVE);
    return passport;
  }

  private void stubRoot() {
    when(passportRepository.findPassport(ROOT_ID, Passport.Status.ACTIVE))
        .thenReturn(Optional.of(activePassport(ROOT_ID)));
  }

  @Test
  public void shouldRejectExistingPassportIdBeforeSaving() {
    CreatePassportUsingPlatformRequestDto request = new CreatePassportUsingPlatformRequestDto();
    request.setId("existing-passport");
    request.setParentId(ROOT_ID);
    when(passportRepository.existsById("existing-passport")).thenReturn(true);

    assertThrows(
        com.opencirc.api.passport.exception.InvalidInputException.class,
        () -> passportService.createPassportUsingPlatform(Platform.BSDD, request, ACTOR));

    org.mockito.Mockito.verify(passportRepository, org.mockito.Mockito.never()).save(any());
    org.mockito.Mockito.verifyNoInteractions(datasheetDefinitionService);
  }

  @Test
  public void shouldRejectChildWithoutParent() {
    CreatePassportUsingPlatformRequestDto request = new CreatePassportUsingPlatformRequestDto();
    request.setName("Orphan");
    request.setPlatformId("platform-id");

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> passportService.createPassportUsingPlatform(Platform.BSDD, request, ACTOR));

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
    org.mockito.Mockito.verify(passportRepository, org.mockito.Mockito.never()).save(any());
  }

  @Test
  public void shouldRejectParentFromAnotherProject() {
    CreatePassportUsingPlatformRequestDto request = new CreatePassportUsingPlatformRequestDto();
    request.setName("Child");
    request.setPlatformId("platform-id");
    request.setParentId("foreign-root");
    Passport foreignRoot = activePassport("foreign-root");
    foreignRoot.setProjectId("project-2");
    when(passportRepository.findPassport("foreign-root", Passport.Status.ACTIVE))
        .thenReturn(Optional.of(foreignRoot));

    HttpServerErrorException exception =
        assertThrows(
            HttpServerErrorException.class,
            () -> passportService.createPassportUsingPlatform(Platform.BSDD, request, ACTOR));

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
    org.mockito.Mockito.verify(passportRepository, org.mockito.Mockito.never()).save(any());
  }

  @Test
  public void shouldRejectDuplicateBatchIdsBeforeSaving() {
    CreatePassportUsingPlatformRequestDto firstRequest =
        new CreatePassportUsingPlatformRequestDto();
    firstRequest.setId("duplicate-passport");
    CreatePassportUsingPlatformRequestDto secondRequest =
        new CreatePassportUsingPlatformRequestDto();
    secondRequest.setId("duplicate-passport");

    assertThrows(
        com.opencirc.api.passport.exception.InvalidInputException.class,
        () ->
            passportService.batchCreatePassportsUsingPlatform(
                Platform.BSDD, List.of(firstRequest, secondRequest), ACTOR));

    org.mockito.Mockito.verify(passportRepository, org.mockito.Mockito.never()).save(any());
    org.mockito.Mockito.verifyNoInteractions(datasheetDefinitionService);
  }

  @Test
  public void shouldRejectBatchContainingExistingIdBeforeSavingAnyPassport() {
    CreatePassportUsingPlatformRequestDto firstRequest =
        new CreatePassportUsingPlatformRequestDto();
    firstRequest.setId("new-passport");
    CreatePassportUsingPlatformRequestDto secondRequest =
        new CreatePassportUsingPlatformRequestDto();
    secondRequest.setId("existing-passport");
    when(passportRepository.existsById(anyString()))
        .thenAnswer(invocation -> "existing-passport".equals(invocation.getArgument(0)));

    assertThrows(
        com.opencirc.api.passport.exception.InvalidInputException.class,
        () ->
            passportService.batchCreatePassportsUsingPlatform(
                Platform.BSDD, List.of(firstRequest, secondRequest), ACTOR));

    org.mockito.Mockito.verify(passportRepository, org.mockito.Mockito.never()).save(any());
  }

  @Test
  public void shouldLogEventWhenPassportCreated() throws Exception {
    Platform platform = Platform.BSDD;
    CreatePassportUsingPlatformRequestDto request = new CreatePassportUsingPlatformRequestDto();
    request.setName("Test Passport");
    request.setPlatformId("platform-id");
    request.setParentId(ROOT_ID);
    stubRoot();

    // Passport created inside the service
    Passport passport = activePassport("generated-id");
    when(passportRepository.save(any(Passport.class))).thenAnswer(i -> i.getArguments()[0]);
    when(passportRepository.findById(anyString())).thenReturn(Optional.of(passport));

    passportService.createPassportUsingPlatform(platform, request, ACTOR);

    var eventOrder = org.mockito.Mockito.inOrder(auditLogService, datasheetDefinitionService);
    eventOrder
        .verify(auditLogService)
        .record(
            eq(AuditTargetType.PASSPORT),
            anyString(),
            eq(PROJECT_ID),
            anyString(),
            eq(AuditLogAction.CREATE),
            any(),
            any());
    eventOrder
        .verify(datasheetDefinitionService)
        .resolveDefinitions(eq(platform), eq("platform-id"), eq(true));
  }

  @Test
  public void shouldUseProvidedIdAndScopeToActorProjectWhenCreatingPassport() throws Exception {
    Platform platform = Platform.BSDD;
    CreatePassportUsingPlatformRequestDto request = new CreatePassportUsingPlatformRequestDto();
    request.setId("my-custom-id");
    request.setName("Test Passport");
    request.setPlatformId("platform-id");
    request.setParentId(ROOT_ID);
    stubRoot();

    Passport passport = activePassport("my-custom-id");
    when(passportRepository.save(any(Passport.class))).thenAnswer(i -> i.getArguments()[0]);
    when(passportRepository.findById("my-custom-id")).thenReturn(Optional.of(passport));

    var createdPassport = passportService.createPassportUsingPlatform(platform, request, ACTOR);

    verify(auditLogService)
        .record(
            eq(AuditTargetType.PASSPORT),
            eq("my-custom-id"),
            eq(PROJECT_ID),
            eq("my-custom-id"),
            eq(AuditLogAction.CREATE),
            any(),
            any());
    assertEquals("my-custom-id", createdPassport.getId());

    ArgumentCaptor<Passport> saved = ArgumentCaptor.forClass(Passport.class);
    verify(passportRepository).save(saved.capture());
    assertEquals(PROJECT_ID, saved.getValue().getProjectId());
    assertEquals("key-1", saved.getValue().getCreatedById());
    assertEquals(ROOT_ID, saved.getValue().getParentId());
  }

  @Test
  public void shouldSortBatchAndCreateParentsFirst() throws Exception {
    Platform platform = Platform.BSDD;

    // Create child
    CreatePassportUsingPlatformRequestDto childReq = new CreatePassportUsingPlatformRequestDto();
    childReq.setId("child-id");
    childReq.setParentId("parent-id");
    childReq.setName("Child Passport");
    childReq.setPlatformId("platform-id");

    // Create parent, hanging from the project root
    CreatePassportUsingPlatformRequestDto parentReq = new CreatePassportUsingPlatformRequestDto();
    parentReq.setId("parent-id");
    parentReq.setParentId(ROOT_ID);
    parentReq.setName("Parent Passport");
    parentReq.setPlatformId("platform-id");

    // Add in reverse order
    java.util.List<CreatePassportUsingPlatformRequestDto> batch =
        java.util.List.of(childReq, parentReq);

    Passport parentPassport = activePassport("parent-id");
    Passport childPassport = activePassport("child-id");
    childPassport.setParentId("parent-id");

    java.util.List<String> saveOrder = new java.util.ArrayList<>();
    java.util.Set<String> savedIds = new java.util.HashSet<>();
    when(passportRepository.save(any(Passport.class)))
        .thenAnswer(
            i -> {
              Passport p = i.getArgument(0);
              savedIds.add(p.getId());
              saveOrder.add(p.getId());
              return p;
            });

    when(passportRepository.findById("parent-id")).thenReturn(Optional.of(parentPassport));
    when(passportRepository.findById("child-id")).thenReturn(Optional.of(childPassport));
    when(passportRepository.findPassport(anyString(), eq(Passport.Status.ACTIVE)))
        .thenAnswer(
            i -> {
              String id = i.getArgument(0);
              if (!ROOT_ID.equals(id) && !savedIds.contains(id)) {
                return Optional.empty();
              }
              return Optional.of(activePassport(id));
            });

    var createdPassports =
        passportService.batchCreatePassportsUsingPlatform(platform, batch, ACTOR);

    assertEquals(2, createdPassports.size());
    // The returned list should maintain the original input order
    assertEquals("child-id", createdPassports.get(0).getId());
    assertEquals("parent-id", createdPassports.get(1).getId());

    // BUT the actual processing/saving order must be topological (parent first)
    assertEquals("parent-id", saveOrder.get(0));
    assertEquals("child-id", saveOrder.get(1));
  }

  @Test
  public void shouldThrowInvalidInputExceptionForCircularDependencyInBatch() {
    Platform platform = Platform.BSDD;

    CreatePassportUsingPlatformRequestDto p1 = new CreatePassportUsingPlatformRequestDto();
    p1.setId("p1");
    p1.setParentId("p2");

    CreatePassportUsingPlatformRequestDto p2 = new CreatePassportUsingPlatformRequestDto();
    p2.setId("p2");
    p2.setParentId("p1");

    java.util.List<CreatePassportUsingPlatformRequestDto> batch = java.util.List.of(p1, p2);

    assertThrows(
        com.opencirc.api.passport.exception.InvalidInputException.class,
        () -> passportService.batchCreatePassportsUsingPlatform(platform, batch, ACTOR));
  }

  @Test
  public void testTopologicalSortWithRealData() throws Exception {
    Platform platform = Platform.BSDD;
    ObjectMapper mapper = new ObjectMapper();
    java.util.List<CreatePassportUsingPlatformRequestDto> dataArray =
        mapper.readValue(
            new java.io.File("test.json"),
            new tools.jackson.core.type.TypeReference<
                java.util.List<CreatePassportUsingPlatformRequestDto>>() {});
    // Top-level entries of the sample hang from the project root in the target model.
    for (var d : dataArray) {
      if (d.getParentId() == null || d.getParentId().isBlank()) {
        d.setParentId(ROOT_ID);
      }
    }

    java.util.Set<String> savedIds = new java.util.HashSet<>();
    java.util.List<String> saveOrder = new java.util.ArrayList<>();
    java.util.Set<String> batchIds = new java.util.HashSet<>();
    for (var d : dataArray) {
      batchIds.add(d.getId());
    }

    when(passportRepository.save(any(Passport.class)))
        .thenAnswer(
            i -> {
              Passport p = i.getArgument(0);
              savedIds.add(p.getId());
              saveOrder.add(p.getId());
              return p;
            });

    when(passportRepository.findById(anyString()))
        .thenAnswer(
            i -> {
              String id = i.getArgument(0);
              if (savedIds.contains(id) || !batchIds.contains(id)) {
                return Optional.of(activePassport(id));
              }
              return Optional.empty();
            });

    when(passportRepository.findPassport(anyString(), eq(Passport.Status.ACTIVE)))
        .thenAnswer(
            i -> {
              String id = i.getArgument(0);
              if (savedIds.contains(id) || !batchIds.contains(id)) {
                return Optional.of(activePassport(id));
              }
              return Optional.empty();
            });

    var createdPassports =
        passportService.batchCreatePassportsUsingPlatform(platform, dataArray, ACTOR);
    assertEquals(18, createdPassports.size());

    java.util.Set<CreatePassportUsingPlatformRequestDto> set = new java.util.HashSet<>(dataArray);
    assertEquals(18, set.size(), "Set size should be 18");

    // Verify topological order: every parent in the batch must be saved before its child
    for (var d : dataArray) {
      if (d.getParentId() != null && batchIds.contains(d.getParentId())) {
        int parentIndex = saveOrder.indexOf(d.getParentId());
        int childIndex = saveOrder.indexOf(d.getId());
        assertTrue(
            parentIndex < childIndex,
            "Parent " + d.getParentId() + " should be saved before child " + d.getId());
      }
    }
  }

  @Test
  public void shouldThrowUnprocessableEntityWhenParentOutsideBatchDoesNotExist() {
    Platform platform = Platform.BSDD;

    CreatePassportUsingPlatformRequestDto p1 = new CreatePassportUsingPlatformRequestDto();
    p1.setId("p1");
    p1.setParentId("non-existent-parent");

    List<CreatePassportUsingPlatformRequestDto> batch = List.of(p1);

    when(passportRepository.findPassport("non-existent-parent", Passport.Status.ACTIVE))
        .thenReturn(Optional.empty());

    HttpServerErrorException exception =
        assertThrows(
            HttpServerErrorException.class,
            () -> passportService.batchCreatePassportsUsingPlatform(platform, batch, ACTOR));

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
    assertTrue(exception.getMessage().contains("Invalid parentId: active parent not found"));
  }

  @Test
  public void getImmediateChildren_ShouldThrowNotFound_WhenPassportDoesNotExist() {
    String passportId = "non-existent-id";
    when(passportRepository.findById(passportId)).thenReturn(Optional.empty());

    HttpServerErrorException exception =
        assertThrows(
            HttpServerErrorException.class,
            () -> passportService.getImmediateChildren(passportId, ACTOR));

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    assertTrue(exception.getMessage().contains("Could not find passport with ID " + passportId));
  }

  @Test
  public void getImmediateChildren_ShouldRejectPassportOfAnotherProject() {
    Passport foreign = activePassport("foreign-id");
    foreign.setProjectId("project-2");
    when(passportRepository.findById("foreign-id")).thenReturn(Optional.of(foreign));

    assertThrows(
        ForbiddenException.class, () -> passportService.getImmediateChildren("foreign-id", ACTOR));
    org.mockito.Mockito.verify(passportRepository, org.mockito.Mockito.never())
        .findImmediateChildren(anyString(), anyString());
  }

  @Test
  public void getImmediateChildren_ShouldReturnEmptyList_WhenNoChildrenExist() {
    String passportId = "parent-id";
    when(passportRepository.findById(passportId))
        .thenReturn(Optional.of(activePassport(passportId)));
    when(passportRepository.findImmediateChildren(passportId, PROJECT_ID))
        .thenReturn(Optional.empty());

    List<PassportDto> children = passportService.getImmediateChildren(passportId, ACTOR);

    assertTrue(children.isEmpty());
  }

  @Test
  public void getImmediateChildren_ShouldReturnChildren_WhenChildrenExist() {
    String passportId = "parent-id";
    when(passportRepository.findById(passportId))
        .thenReturn(Optional.of(activePassport(passportId)));

    PassportDatasheetResultMapQueryResult childRow =
        mock(PassportDatasheetResultMapQueryResult.class);
    when(childRow.getPassportId()).thenReturn("child-id");
    when(childRow.getPassportName()).thenReturn("Child Passport");
    when(childRow.getStatus()).thenReturn("active");
    when(childRow.getParentId()).thenReturn(passportId);

    when(passportRepository.findImmediateChildren(passportId, PROJECT_ID))
        .thenReturn(Optional.of(List.of(childRow)));

    List<PassportDto> children = passportService.getImmediateChildren(passportId, ACTOR);

    assertEquals(1, children.size());
    assertEquals("child-id", children.get(0).getId());
    assertEquals("Child Passport", children.get(0).getName());
    assertEquals(passportId, children.get(0).getParentId());
  }

  @Test
  public void archivePassport_ShouldRefuseRootPassport() {
    Passport root = activePassport(ROOT_ID);
    when(passportRepository.findById(ROOT_ID)).thenReturn(Optional.of(root));

    assertThrows(
        com.opencirc.api.passport.exception.InvalidInputException.class,
        () -> passportService.archivePassport(ROOT_ID, ACTOR));
    org.mockito.Mockito.verify(passportRepository, org.mockito.Mockito.never())
        .archiveSubtree(anyString(), any());
  }

  @Test
  public void archivePassport_ShouldArchiveSubtreeAndLog() {
    Passport child = activePassport("child-id");
    child.setParentId(ROOT_ID);
    Passport archived = activePassport("child-id");
    archived.setParentId(ROOT_ID);
    archived.setStatus(Passport.Status.ARCHIVED);
    when(passportRepository.findById("child-id"))
        .thenReturn(Optional.of(child), Optional.of(archived));
    when(passportRepository.archiveSubtree(eq("child-id"), any())).thenReturn(3);

    PassportDto result = passportService.archivePassport("child-id", ACTOR);

    assertEquals("archived", result.getStatus());
    verify(auditLogService)
        .record(
            eq(AuditTargetType.PASSPORT),
            eq("child-id"),
            eq(PROJECT_ID),
            eq("child-id"),
            eq(AuditLogAction.UPDATE),
            any(),
            any());
  }
}
