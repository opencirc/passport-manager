package com.opencirc.api.passport.service.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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
import com.opencirc.api.passport.dto.UserDto;
import com.opencirc.api.passport.enums.PassportLogAction;
import com.opencirc.api.passport.enums.Platform;
import com.opencirc.api.passport.model.Datasheet;
import com.opencirc.api.passport.model.Passport;
import com.opencirc.api.passport.model.PassportDatasheetMapping;
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

@ExtendWith(MockitoExtension.class)
public class TestPassportServiceStructureLogging {

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
  public void shouldLogEventWhenDatasheetAdded() throws Exception {
    String passportId = "passport-123";
    Passport passport = new Passport();
    passport.setId(passportId);
    passport.setStatus(Passport.Status.ACTIVE);
    passport.setDatasheetMappings(new HashSet<>());

    when(passportRepository.findById(passportId)).thenReturn(Optional.of(passport));

    Platform platform = Platform.BSDD;
    String platformId = "platform-abc";
    Datasheet.DataCategory category = Datasheet.DataCategory.GENERIC;
    UserDto author = new UserDto();
    author.setId("user-456");

    PlatformAdapter adapter = mock(PlatformAdapter.class);
    when(platformAdapterFactory.getAdapter(platform)).thenReturn(adapter);

    Datasheet rawDatasheet = new Datasheet();
    rawDatasheet.setId("datasheet-789");
    when(adapter.generateDatasheetsFromPlatformId(eq(platformId), anyBoolean()))
        .thenReturn(List.of(rawDatasheet));
    when(datasheetRepository.save(any(Datasheet.class))).thenReturn(rawDatasheet);
    when(passportDatasheetMappingRepository.save(any(PassportDatasheetMapping.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    passportService.addDatasheetsToPassportUsingPlatform(
        passportId, platform, platformId, category, author);

    verify(passportLogService)
        .logEvent(eq(passportId), eq(PassportLogAction.ADD_DATASHEET), any(List.class));
  }

  @Test
  public void shouldLogEventWhenDatasheetRemoved() {
    String passportId = "passport-123";
    String datasheetId = "ds-789";

    Passport passport = new Passport();
    passport.setId(passportId);
    passport.setStatus(Passport.Status.ACTIVE);

    Datasheet datasheet = new Datasheet();
    datasheet.setId(datasheetId);
    datasheet.setName("Test Datasheet");

    PassportDatasheetMapping mapping = new PassportDatasheetMapping();
    mapping.setPassport(passport);
    mapping.setDatasheet(datasheet);
    passport.setDatasheetMappings(new HashSet<>(List.of(mapping)));

    when(passportRepository.findById(passportId)).thenReturn(Optional.of(passport));

    passportService.removeDatasheet(passportId, datasheetId);

    verify(passportDatasheetMappingRepository).delete(mapping);
    verify(passportLogService)
        .logEvent(eq(passportId), eq(PassportLogAction.REMOVE_DATASHEET), any(List.class));
  }

  @Test
  public void shouldLogEventWhenParentUpdated() {
    String passportId = "passport-123";
    String oldParentId = "old-parent";
    String newParentId = "new-parent";

    Passport passport = new Passport();
    passport.setId(passportId);
    passport.setParentId(oldParentId);
    passport.setStatus(Passport.Status.ACTIVE);

    when(passportRepository.findById(passportId)).thenReturn(Optional.of(passport));
    when(passportRepository.findPassport(newParentId, Passport.Status.ACTIVE))
        .thenReturn(Optional.of(new Passport()));
    when(passportRepository.save(any(Passport.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    passportService.updateParent(passportId, newParentId);

    verify(passportRepository).save(passport);
    verify(passportLogService)
        .logEvent(eq(passportId), eq(PassportLogAction.UPDATE_RELATIONSHIPS), any(List.class));
  }
}
