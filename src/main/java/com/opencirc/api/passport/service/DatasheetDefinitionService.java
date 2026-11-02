package com.opencirc.api.passport.service;

import com.opencirc.api.passport.adapter.PlatformAdapter;
import com.opencirc.api.passport.adapter.PlatformAdapterFactory;
import com.opencirc.api.passport.dao.DatasheetDefinitionRepository;
import com.opencirc.api.passport.enums.AuditLogAction;
import com.opencirc.api.passport.enums.AuditTargetType;
import com.opencirc.api.passport.enums.Platform;
import com.opencirc.api.passport.exception.JsonValidationException;
import com.opencirc.api.passport.model.DatasheetDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Resolves datasheet definitions from the global catalogue, fetching a classification from its
 * external platform only on first encounter and caching it for every later passport.
 */
@Service
@Slf4j
public class DatasheetDefinitionService {

  private final DatasheetDefinitionRepository definitionRepository;

  private final PlatformAdapterFactory platformAdapterFactory;

  private final AuditLogService auditLogService;

  /**
   * Runs each new definition insert in its own transaction so a conflict cannot abort the caller.
   */
  private final TransactionTemplate independentTransaction;

  /** Constructor. */
  public DatasheetDefinitionService(
      DatasheetDefinitionRepository definitionRepository,
      PlatformAdapterFactory platformAdapterFactory,
      AuditLogService auditLogService,
      PlatformTransactionManager transactionManager) {
    this.definitionRepository = definitionRepository;
    this.platformAdapterFactory = platformAdapterFactory;
    this.auditLogService = auditLogService;
    this.independentTransaction = new TransactionTemplate(transactionManager);
    this.independentTransaction.setPropagationBehavior(
        TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * Resolves the definition for a classification URI and, when requested, the definitions of its
   * related IFC entities. Element 0 is always the definition of the URI itself.
   */
  public List<DatasheetDefinition> resolveDefinitions(
      Platform platform, String platformId, boolean includeRelatedIfcEntities)
      throws JsonValidationException {
    PlatformAdapter adapter = platformAdapterFactory.getAdapter(platform);

    DatasheetDefinition main = resolveDefinition(adapter, platformId);
    List<DatasheetDefinition> definitions = new ArrayList<>();
    definitions.add(main);

    if (includeRelatedIfcEntities && main.getRelatedPlatformIds() != null) {
      for (String relatedPlatformId : main.getRelatedPlatformIds()) {
        if (relatedPlatformId == null
            || relatedPlatformId.isBlank()
            || relatedPlatformId.equals(platformId)) {
          continue;
        }
        definitions.add(resolveDefinition(adapter, relatedPlatformId));
      }
    }

    return definitions;
  }

  /** Resolves a single definition, reusing the cached row when the URI is already known. */
  public DatasheetDefinition resolveDefinition(Platform platform, String platformId)
      throws JsonValidationException {
    return resolveDefinition(platformAdapterFactory.getAdapter(platform), platformId);
  }

  private DatasheetDefinition resolveDefinition(PlatformAdapter adapter, String platformId)
      throws JsonValidationException {
    Optional<DatasheetDefinition> cached = definitionRepository.findByPlatformId(platformId);
    if (cached.isPresent()) {
      return cached.get();
    }
    return persistOrReuse(adapter.generateDatasheetFromPlatformId(platformId));
  }

  /**
   * Persists a freshly fetched definition. When a concurrent ingestion persisted the same URI
   * first, the unique constraint on {@code platform_id} rejects the insert and the winning row is
   * reused instead.
   */
  DatasheetDefinition persistOrReuse(DatasheetDefinition definition) {
    String platformId = definition.getPlatformId();
    boolean persisted = false;
    try {
      independentTransaction.executeWithoutResult(
          status -> definitionRepository.saveAndFlush(definition));
      persisted = true;
    } catch (DataIntegrityViolationException e) {
      log.info("Datasheet definition {} was persisted concurrently; reusing it", platformId);
    }
    if (persisted) {
      auditLogService.record(
          AuditTargetType.DATASHEET_DEFINITION,
          definition.getId(),
          null,
          null,
          AuditLogAction.CREATE,
          null,
          auditLogService.definitionSnapshot(definition));
    }
    return definitionRepository
        .findByPlatformId(platformId)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Datasheet definition could not be persisted: " + platformId));
  }
}
