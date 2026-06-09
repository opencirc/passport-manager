package com.opencirc.api.passport.service.test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencirc.api.passport.adapter.PlatformAdapter;
import com.opencirc.api.passport.adapter.PlatformAdapterFactory;
import com.opencirc.api.passport.config.AppProperties;
import com.opencirc.api.passport.dao.DatasheetRepository;
import com.opencirc.api.passport.dao.PassportDatasheetMappingRepository;
import com.opencirc.api.passport.dao.PassportRepository;
import com.opencirc.api.passport.dto.CreatePassportUsingPlatformRequestDto;
import com.opencirc.api.passport.dto.PassportDto;
import com.opencirc.api.passport.dto.UserDto;
import com.opencirc.api.passport.dto.query.PassportDatasheetResultMapQueryResult;
import com.opencirc.api.passport.enums.PassportLogAction;
import com.opencirc.api.passport.enums.Platform;
import com.opencirc.api.passport.model.Passport;
import com.opencirc.api.passport.service.EpdEnrichmentService;
import com.opencirc.api.passport.service.PassportLogService;
import com.opencirc.api.passport.service.PassportService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;

@ExtendWith(MockitoExtension.class)
public class TestPassportService {

  @Mock private DatasheetRepository datasheetRepository;
  @Mock private PassportRepository passportRepository;
  @Mock private PassportDatasheetMappingRepository passportDatasheetMappingRepository;
  @Mock private PlatformAdapterFactory platformAdapterFactory;
  @Mock private ObjectMapper objectMapper;
  @Mock private AppProperties appProperties;
  @Mock private EpdEnrichmentService epdEnrichmentService;
  @Mock private PassportLogService passportLogService;

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
  public void shouldLogEventWhenPassportCreated() throws Exception {
    Platform platform = Platform.BSDD;
    CreatePassportUsingPlatformRequestDto request = new CreatePassportUsingPlatformRequestDto();
    request.setName("Test Passport");
    request.setPlatformId("platform-id");
    UserDto author = new UserDto();
    author.setId("author-id");

    PlatformAdapter adapter = mock(PlatformAdapter.class);
    when(platformAdapterFactory.getAdapter(platform)).thenReturn(adapter);

    // Passport created inside the service
    Passport passport = new Passport();
    passport.setId("generated-id");
    passport.setStatus(Passport.Status.ACTIVE);
    when(passportRepository.save(any(Passport.class))).thenAnswer(i -> i.getArguments()[0]);
    when(passportRepository.findById(anyString())).thenReturn(java.util.Optional.of(passport));

    passportService.createPassportUsingPlatform(platform, request, author);

    verify(passportLogService)
        .logEvent(anyString(), eq(PassportLogAction.CREATE), any(java.util.List.class));
  }

  @Test
  public void shouldUseProvidedIdWhenCreatingPassport() throws Exception {
    Platform platform = Platform.BSDD;
    CreatePassportUsingPlatformRequestDto request = new CreatePassportUsingPlatformRequestDto();
    request.setId("my-custom-id");
    request.setName("Test Passport");
    request.setPlatformId("platform-id");
    UserDto author = new UserDto();
    author.setId("author-id");

    PlatformAdapter adapter = mock(PlatformAdapter.class);
    when(platformAdapterFactory.getAdapter(platform)).thenReturn(adapter);

    // Passport created inside the service
    Passport passport = new Passport();
    passport.setId("my-custom-id");
    passport.setStatus(Passport.Status.ACTIVE);
    when(passportRepository.save(any(Passport.class))).thenAnswer(i -> i.getArguments()[0]);
    when(passportRepository.findById("my-custom-id")).thenReturn(java.util.Optional.of(passport));

    var createdPassport = passportService.createPassportUsingPlatform(platform, request, author);

    verify(passportLogService)
        .logEvent(eq("my-custom-id"), eq(PassportLogAction.CREATE), any(java.util.List.class));
    org.junit.jupiter.api.Assertions.assertEquals("my-custom-id", createdPassport.getId());
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

    // Create parent
    CreatePassportUsingPlatformRequestDto parentReq = new CreatePassportUsingPlatformRequestDto();
    parentReq.setId("parent-id");
    parentReq.setName("Parent Passport");
    parentReq.setPlatformId("platform-id");

    // Add in reverse order
    java.util.List<CreatePassportUsingPlatformRequestDto> batch =
        java.util.List.of(childReq, parentReq);

    UserDto author = new UserDto();
    author.setId("author-id");

    PlatformAdapter adapter = mock(PlatformAdapter.class);
    when(platformAdapterFactory.getAdapter(platform)).thenReturn(adapter);

    // For parent creation
    Passport parentPassport = new Passport();
    parentPassport.setId("parent-id");
    parentPassport.setStatus(Passport.Status.ACTIVE);

    // For child creation
    Passport childPassport = new Passport();
    childPassport.setId("child-id");
    childPassport.setParentId("parent-id");
    childPassport.setStatus(Passport.Status.ACTIVE);

    java.util.List<String> saveOrder = new java.util.ArrayList<>();
    when(passportRepository.save(any(Passport.class)))
        .thenAnswer(
            i -> {
              Passport p = i.getArgument(0);
              saveOrder.add(p.getId());
              return p;
            });

    when(passportRepository.findById("parent-id"))
        .thenReturn(java.util.Optional.of(parentPassport));
    when(passportRepository.findById("child-id")).thenReturn(java.util.Optional.of(childPassport));
    when(passportRepository.findPassport("parent-id", Passport.Status.ACTIVE))
        .thenReturn(java.util.Optional.of(parentPassport));

    var createdPassports =
        passportService.batchCreatePassportsUsingPlatform(platform, batch, author);

    org.junit.jupiter.api.Assertions.assertEquals(2, createdPassports.size());
    // The returned list should maintain the original input order
    org.junit.jupiter.api.Assertions.assertEquals("child-id", createdPassports.get(0).getId());
    org.junit.jupiter.api.Assertions.assertEquals("parent-id", createdPassports.get(1).getId());

    // BUT the actual processing/saving order must be topological (parent first)
    org.junit.jupiter.api.Assertions.assertEquals("parent-id", saveOrder.get(0));
    org.junit.jupiter.api.Assertions.assertEquals("child-id", saveOrder.get(1));
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

    org.junit.jupiter.api.Assertions.assertThrows(
        com.opencirc.api.passport.exception.InvalidInputException.class,
        () -> passportService.batchCreatePassportsUsingPlatform(platform, batch, new UserDto()));
  }

  @Test
  public void testTopologicalSortWithRealData() throws Exception {
    Platform platform = Platform.BSDD;
    ObjectMapper mapper = new ObjectMapper();
    java.util.List<CreatePassportUsingPlatformRequestDto> dataArray =
        mapper.readValue(
            new java.io.File("test.json"),
            new com.fasterxml.jackson.core.type.TypeReference<
                java.util.List<CreatePassportUsingPlatformRequestDto>>() {});

    UserDto author = new UserDto();
    author.setId("author-id");

    PlatformAdapter adapter = mock(PlatformAdapter.class);
    when(platformAdapterFactory.getAdapter(platform)).thenReturn(adapter);

    java.util.Set<String> savedIds = new java.util.HashSet<>();
    java.util.Set<String> batchIds = new java.util.HashSet<>();
    for (var d : dataArray) {
      batchIds.add(d.getId());
    }

    when(passportRepository.save(any(Passport.class)))
        .thenAnswer(
            i -> {
              Passport p = i.getArgument(0);
              savedIds.add(p.getId());
              return p;
            });

    when(passportRepository.findById(anyString()))
        .thenAnswer(
            i -> {
              String id = i.getArgument(0);
              if (savedIds.contains(id) || !batchIds.contains(id)) {
                Passport p = new Passport();
                p.setId(id);
                p.setStatus(Passport.Status.ACTIVE);
                return java.util.Optional.of(p);
              }
              return java.util.Optional.empty();
            });

    when(passportRepository.findPassport(anyString(), eq(Passport.Status.ACTIVE)))
        .thenAnswer(
            i -> {
              String id = i.getArgument(0);
              if (savedIds.contains(id) || !batchIds.contains(id)) {
                Passport p = new Passport();
                p.setId(id);
                p.setStatus(Passport.Status.ACTIVE);
                return java.util.Optional.of(p);
              }
              return java.util.Optional.empty();
            });

    var createdPassports =
        passportService.batchCreatePassportsUsingPlatform(platform, dataArray, author);
    org.junit.jupiter.api.Assertions.assertEquals(18, createdPassports.size());

    java.util.Set<CreatePassportUsingPlatformRequestDto> set = new java.util.HashSet<>(dataArray);
    org.junit.jupiter.api.Assertions.assertEquals(18, set.size(), "Set size should be 18");
  }

  @Test
  public void getImmediateChildren_ShouldThrowNotFound_WhenPassportDoesNotExist() {
    String passportId = "non-existent-id";
    when(passportRepository.existsById(passportId)).thenReturn(false);

    HttpServerErrorException exception =
        assertThrows(
            HttpServerErrorException.class, () -> passportService.getImmediateChildren(passportId));

    org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    assertTrue(exception.getMessage().contains("Could not find passport with ID " + passportId));
  }

  @Test
  public void getImmediateChildren_ShouldReturnEmptyList_WhenNoChildrenExist() {
    String passportId = "parent-id";
    when(passportRepository.existsById(passportId)).thenReturn(true);
    when(passportRepository.findImmediateChildren(passportId)).thenReturn(Optional.empty());

    List<PassportDto> children = passportService.getImmediateChildren(passportId);

    assertTrue(children.isEmpty());
  }

  @Test
  public void getImmediateChildren_ShouldReturnChildren_WhenChildrenExist() {
    String passportId = "parent-id";
    when(passportRepository.existsById(passportId)).thenReturn(true);

    PassportDatasheetResultMapQueryResult childRow =
        mock(PassportDatasheetResultMapQueryResult.class);
    when(childRow.getPassportId()).thenReturn("child-id");
    when(childRow.getPassportName()).thenReturn("Child Passport");
    when(childRow.getStatus()).thenReturn("active");
    when(childRow.getParentId()).thenReturn(passportId);

    when(passportRepository.findImmediateChildren(passportId))
        .thenReturn(Optional.of(List.of(childRow)));

    List<PassportDto> children = passportService.getImmediateChildren(passportId);

    org.junit.jupiter.api.Assertions.assertEquals(1, children.size());
    org.junit.jupiter.api.Assertions.assertEquals("child-id", children.get(0).getId());
    org.junit.jupiter.api.Assertions.assertEquals("Child Passport", children.get(0).getName());
    org.junit.jupiter.api.Assertions.assertEquals(passportId, children.get(0).getParentId());
  }
}
