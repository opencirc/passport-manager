package com.opencirc.api.passport.service.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencirc.api.passport.adapter.PlatformAdapter;
import com.opencirc.api.passport.adapter.PlatformAdapterFactory;
import com.opencirc.api.passport.config.AppProperties;
import com.opencirc.api.passport.dao.DatasheetDefinitionRepository;
import com.opencirc.api.passport.dao.DatasheetRepository;
import com.opencirc.api.passport.dao.PassportRepository;
import com.opencirc.api.passport.enums.Platform;
import com.opencirc.api.passport.model.Datasheet;
import com.opencirc.api.passport.model.DatasheetDefinition;
import com.opencirc.api.passport.model.Passport;
import com.opencirc.api.passport.service.PassportService;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Verifies the core globalisation guarantee: a datasheet definition is fetched from the platform
 * exactly once per URI and reused across passports, with no further dictionary round-trips.
 */
class TestPassportServiceDefinitionReuse {

  private static final String URI =
      "https://identifier.buildingsmart.org/uri/molio/cciconstruction/1.0/class/A-A__";

  @Mock private DatasheetRepository datasheetRepository;
  @Mock private DatasheetDefinitionRepository datasheetDefinitionRepository;
  @Mock private PassportRepository passportRepository;
  @Mock private PlatformAdapterFactory platformAdapterFactory;
  @Mock private PlatformAdapter platformAdapter;
  @Mock private AppProperties appProperties;

  private PassportService passportService;

  @BeforeEach
  void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);

    passportService =
        new PassportService(
            datasheetRepository,
            datasheetDefinitionRepository,
            passportRepository,
            platformAdapterFactory,
            new ObjectMapper(),
            appProperties);

    when(platformAdapterFactory.getAdapter(Platform.BSDD)).thenReturn(platformAdapter);

    lenient().when(appProperties.getSystemAdminName()).thenReturn("admin");
    lenient().when(appProperties.getSystemAdminEmail()).thenReturn("admin@test.com");

    // Persisting a definition / datasheet returns the same entity (id assignment is irrelevant
    // here).
    when(datasheetDefinitionRepository.save(any(DatasheetDefinition.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(datasheetRepository.save(any(Datasheet.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // The adapter builds a fresh definition for the URI on each (avoided) call.
    when(platformAdapter.generateDatasheetsFromPlatformId(eq(URI), anyBoolean()))
        .thenAnswer(invocation -> List.of(newDefinition()));
  }

  private DatasheetDefinition newDefinition() {
    DatasheetDefinition definition = new DatasheetDefinition();
    definition.setPlatform(Platform.BSDD);
    definition.setPlatformId(URI);
    definition.setCode("A-A__");
    definition.setName("Space for human dwelling");
    return definition;
  }

  private Passport newPassport(String id) {
    Passport passport = new Passport();
    passport.setId(id);
    passport.setStatus(Passport.Status.ACTIVE);
    passport.setDatasheets(new HashSet<>());
    return passport;
  }

  @Test
  void reusesDefinitionAcrossPassportsWithoutRefetching() throws Exception {
    // First passport: definition does not exist yet -> fetched and persisted once.
    when(datasheetDefinitionRepository.findByPlatformAndPlatformId(Platform.BSDD, URI))
        .thenReturn(Optional.empty());

    Passport first = newPassport("passport-1");
    passportService.addDatasheetsToPassportUsingPlatform(
        first, Platform.BSDD, URI, Datasheet.DataCategory.GENERIC, null, false);

    // After the first create the definition now exists locally.
    DatasheetDefinition persisted = newDefinition();
    when(datasheetDefinitionRepository.findByPlatformAndPlatformId(Platform.BSDD, URI))
        .thenReturn(Optional.of(persisted));

    Passport second = newPassport("passport-2");
    passportService.addDatasheetsToPassportUsingPlatform(
        second, Platform.BSDD, URI, Datasheet.DataCategory.GENERIC, null, false);

    // The dictionary was hit exactly once; the definition was saved exactly once.
    verify(platformAdapter, times(1)).generateDatasheetsFromPlatformId(anyString(), anyBoolean());
    verify(datasheetDefinitionRepository, times(1)).save(any(DatasheetDefinition.class));

    // Each passport still got its own datasheet instance.
    verify(datasheetRepository, times(2)).save(any(Datasheet.class));
    assertEquals(1, first.getDatasheets().size());
    assertEquals(1, second.getDatasheets().size());
  }
}
