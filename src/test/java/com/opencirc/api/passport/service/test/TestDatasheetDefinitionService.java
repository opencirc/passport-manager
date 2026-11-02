package com.opencirc.api.passport.service.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.opencirc.api.passport.adapter.PlatformAdapter;
import com.opencirc.api.passport.adapter.PlatformAdapterFactory;
import com.opencirc.api.passport.dao.DatasheetDefinitionRepository;
import com.opencirc.api.passport.enums.Platform;
import com.opencirc.api.passport.model.DatasheetDefinition;
import com.opencirc.api.passport.service.DatasheetDefinitionService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Verifies the global catalogue guarantee: a datasheet definition is fetched from its platform
 * exactly once per URI and reused across passports, including when two ingestions race.
 */
@ExtendWith(MockitoExtension.class)
public class TestDatasheetDefinitionService {

  private static final String URI =
      "https://identifier.buildingsmart.org/uri/molio/cciconstruction/1.0/class/A-A__";
  private static final String RELATED_URI =
      "https://identifier.buildingsmart.org/uri/buildingsmart/ifc/4.3/class/IfcSpace";

  @Mock private DatasheetDefinitionRepository definitionRepository;
  @Mock private PlatformAdapterFactory platformAdapterFactory;
  @Mock private PlatformAdapter platformAdapter;

  private DatasheetDefinitionService service;

  @BeforeEach
  void setUp() {
    service =
        new DatasheetDefinitionService(
            definitionRepository,
            platformAdapterFactory,
            new TestPassportServiceUpdateDataLogging.TestTransactionManager());
    when(platformAdapterFactory.getAdapter(Platform.BSDD)).thenReturn(platformAdapter);
  }

  @Test
  void fetchesOncePerUriAndReusesCachedDefinition() throws Exception {
    DatasheetDefinition persisted = definition(URI);
    when(definitionRepository.findByPlatformId(URI))
        .thenReturn(Optional.empty(), Optional.of(persisted), Optional.of(persisted));
    when(platformAdapter.generateDatasheetFromPlatformId(URI)).thenReturn(definition(URI));

    List<DatasheetDefinition> first = service.resolveDefinitions(Platform.BSDD, URI, false);
    List<DatasheetDefinition> second = service.resolveDefinitions(Platform.BSDD, URI, false);

    verify(platformAdapter, times(1)).generateDatasheetFromPlatformId(anyString());
    verify(definitionRepository, times(1)).saveAndFlush(any(DatasheetDefinition.class));
    assertSame(persisted, first.get(0));
    assertSame(persisted, second.get(0));
  }

  @Test
  void reusesDefinitionPersistedConcurrently() throws Exception {
    DatasheetDefinition winner = definition(URI);
    when(definitionRepository.findByPlatformId(URI))
        .thenReturn(Optional.empty(), Optional.of(winner));
    when(platformAdapter.generateDatasheetFromPlatformId(URI)).thenReturn(definition(URI));
    when(definitionRepository.saveAndFlush(any(DatasheetDefinition.class)))
        .thenThrow(new DataIntegrityViolationException("uq_datasheet_definitions_platform_id"));

    List<DatasheetDefinition> result = service.resolveDefinitions(Platform.BSDD, URI, false);

    assertEquals(1, result.size());
    assertSame(winner, result.get(0));
  }

  @Test
  void resolvesRelatedIfcEntitiesWhenRequested() throws Exception {
    DatasheetDefinition main = definition(URI);
    main.setRelatedPlatformIds(List.of(RELATED_URI));
    DatasheetDefinition related = definition(RELATED_URI);
    when(definitionRepository.findByPlatformId(URI)).thenReturn(Optional.of(main));
    when(definitionRepository.findByPlatformId(RELATED_URI))
        .thenReturn(Optional.empty(), Optional.of(related));
    when(platformAdapter.generateDatasheetFromPlatformId(RELATED_URI))
        .thenReturn(definition(RELATED_URI));

    List<DatasheetDefinition> result = service.resolveDefinitions(Platform.BSDD, URI, true);

    assertEquals(List.of(main, related), result);
    verify(platformAdapter, never()).generateDatasheetFromPlatformId(URI);
  }

  @Test
  void skipsRelatedIfcEntitiesWhenNotRequested() throws Exception {
    DatasheetDefinition main = definition(URI);
    main.setRelatedPlatformIds(List.of(RELATED_URI));
    when(definitionRepository.findByPlatformId(URI)).thenReturn(Optional.of(main));

    List<DatasheetDefinition> result = service.resolveDefinitions(Platform.BSDD, URI, false);

    assertEquals(List.of(main), result);
    verify(definitionRepository, never()).findByPlatformId(RELATED_URI);
    verify(platformAdapter, never()).generateDatasheetFromPlatformId(anyString());
  }

  private static DatasheetDefinition definition(String platformId) {
    DatasheetDefinition definition = new DatasheetDefinition();
    definition.setId("id:" + platformId);
    definition.setPlatform(Platform.BSDD);
    definition.setPlatformId(platformId);
    definition.setCode("A-A__");
    definition.setName("Space for human dwelling");
    return definition;
  }
}
