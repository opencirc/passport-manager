package com.opencirc.api.passport.service;

import com.opencirc.api.passport.auth.principal.ApiKeyPrincipal;
import com.opencirc.api.passport.context.ActorContext;
import com.opencirc.api.passport.dao.AuditLogRepository;
import com.opencirc.api.passport.dao.AuditLogTagRepository;
import com.opencirc.api.passport.dto.AuditLogDto;
import com.opencirc.api.passport.enums.AuditLogAction;
import com.opencirc.api.passport.enums.AuditTargetType;
import com.opencirc.api.passport.model.AuditLog;
import com.opencirc.api.passport.model.AuditLogTag;
import com.opencirc.api.passport.model.Datasheet;
import com.opencirc.api.passport.model.DatasheetDefinition;
import com.opencirc.api.passport.model.DatasheetDefinitionProperty;
import com.opencirc.api.passport.model.Passport;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;

/**
 * Records every mutation of passports, passport datasheets and datasheet definitions as an RFC 6902
 * JSON Patch and serves the indexed audit feeds.
 */
@Service
public class AuditLogService {

  private final AuditLogRepository auditLogRepository;

  private final AuditLogTagRepository auditLogTagRepository;

  private final JsonDiffService jsonDiffService;

  private final ActorContext actorContext;

  private final ObjectMapper objectMapper;

  /** Constructor. */
  public AuditLogService(
      AuditLogRepository auditLogRepository,
      AuditLogTagRepository auditLogTagRepository,
      JsonDiffService jsonDiffService,
      ActorContext actorContext,
      ObjectMapper objectMapper) {
    this.auditLogRepository = auditLogRepository;
    this.auditLogTagRepository = auditLogTagRepository;
    this.jsonDiffService = jsonDiffService;
    this.actorContext = actorContext;
    this.objectMapper = objectMapper;
  }

  /**
   * Machine credential responsible for a mutation, captured on the request thread so asynchronous
   * work can still attribute what it writes. A null id means administrative or system action.
   */
  public record Actor(String createdById) {

    /** Identity used for operations without an authenticated API key. */
    public static Actor system() {
      return new Actor(null);
    }
  }

  /** The API key of the current request, or the system actor. */
  public Actor captureActor() {
    return actorContext
        .currentApiKey()
        .map(key -> new Actor(key.getKeyId()))
        .orElse(Actor.system());
  }

  /** Records a mutation performed by the current request's API key. */
  @Transactional
  public AuditLog record(
      AuditTargetType targetType,
      String targetId,
      String projectId,
      String passportId,
      AuditLogAction action,
      JsonNode before,
      JsonNode after) {
    return record(
        targetType, targetId, projectId, passportId, action, before, after, captureActor());
  }

  /** Records a mutation on behalf of the given actor. */
  @Transactional
  public AuditLog record(
      AuditTargetType targetType,
      String targetId,
      String projectId,
      String passportId,
      AuditLogAction action,
      JsonNode before,
      JsonNode after,
      Actor actor) {
    ArrayNode diff = jsonDiffService.diff(before, after);
    AuditLog log = new AuditLog();
    log.setTargetType(targetType);
    log.setTargetId(targetId);
    log.setProjectId(projectId);
    log.setPassportId(passportId);
    log.setAction(action);
    log.setDiff(diff);
    log.setCreatedById(actor != null ? actor.createdById() : null);
    return auditLogRepository.save(log);
  }

  /** Loads an audit log entry or fails with 404. */
  @Transactional(readOnly = true)
  public AuditLog requireLog(String logId) {
    return auditLogRepository
        .findById(logId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Audit log entry not found: " + logId));
  }

  /** Chronological history of one entity, limited to what the actor's project may see. */
  @Transactional(readOnly = true)
  public List<AuditLogDto> findByTarget(
      AuditTargetType targetType, String targetId, ApiKeyPrincipal actor) {
    List<AuditLog> logs =
        auditLogRepository
            .findByTargetTypeAndTargetIdOrderByCreatedTimeAsc(targetType, targetId)
            .stream()
            .filter(
                log ->
                    log.getProjectId() == null || log.getProjectId().equals(actor.getProjectId()))
            .toList();
    return withTags(logs);
  }

  /** Newest-first activity stream of a project, served by the project index without joins. */
  @Transactional(readOnly = true)
  public List<AuditLogDto> findByProjectId(String projectId) {
    return withTags(auditLogRepository.findByProjectIdOrderByCreatedTimeDesc(projectId));
  }

  /** Newest-first activity stream of a passport, served by the passport index without joins. */
  @Transactional(readOnly = true)
  public List<AuditLogDto> findByPassportId(String passportId) {
    return withTags(auditLogRepository.findByPassportIdOrderByCreatedTimeDesc(passportId));
  }

  private List<AuditLogDto> withTags(List<AuditLog> logs) {
    if (logs.isEmpty()) {
      return List.of();
    }
    Map<String, AuditLogTag> tags =
        auditLogTagRepository.findByLogIdIn(logs.stream().map(AuditLog::getId).toList()).stream()
            .collect(Collectors.toMap(AuditLogTag::getLogId, Function.identity()));
    List<AuditLogDto> result = new ArrayList<>();
    for (AuditLog log : logs) {
      result.add(AuditLogDto.from(log, tags.get(log.getId())));
    }
    return result;
  }

  /** The audited state of a passport. */
  public JsonNode passportSnapshot(Passport passport) {
    Map<String, Object> state = new LinkedHashMap<>();
    state.put("id", passport.getId());
    state.put("projectId", passport.getProjectId());
    state.put("name", passport.getName());
    state.put("status", passport.getStatus() != null ? passport.getStatus().getValue() : null);
    state.put("parentId", passport.getParentId());
    state.put("createdById", passport.getCreatedById());
    state.put(
        "archivedTime",
        passport.getArchivedTime() != null ? passport.getArchivedTime().toString() : null);
    return objectMapper.valueToTree(state);
  }

  /** The audited state of a passport datasheet instance. */
  public JsonNode datasheetSnapshot(Datasheet datasheet) {
    Map<String, Object> state = new LinkedHashMap<>();
    state.put("id", datasheet.getId());
    state.put(
        "passportId", datasheet.getPassport() != null ? datasheet.getPassport().getId() : null);
    DatasheetDefinition definition = datasheet.getDefinition();
    state.put("definitionId", definition != null ? definition.getId() : null);
    state.put("platformId", definition != null ? definition.getPlatformId() : null);
    state.put(
        "dataCategory",
        datasheet.getDataCategory() != null ? datasheet.getDataCategory().getValue() : null);
    state.put("data", datasheet.getData() != null ? datasheet.getData() : Map.of());
    return objectMapper.valueToTree(state);
  }

  /** The audited state of a global datasheet definition. */
  public JsonNode definitionSnapshot(DatasheetDefinition definition) {
    Map<String, Object> state = new LinkedHashMap<>();
    state.put("id", definition.getId());
    state.put(
        "platform", definition.getPlatform() != null ? definition.getPlatform().getValue() : null);
    state.put(
        "dictionary",
        definition.getDictionary() != null ? definition.getDictionary().getValue() : null);
    state.put("code", definition.getCode());
    state.put("name", definition.getName());
    state.put("description", definition.getDescription());
    state.put("platformId", definition.getPlatformId());
    state.put("relatedPlatformIds", definition.getRelatedPlatformIds());
    List<Map<String, Object>> properties = new ArrayList<>();
    if (definition.getProperties() != null) {
      definition.getProperties().stream()
          .sorted((a, b) -> String.valueOf(a.getCode()).compareTo(String.valueOf(b.getCode())))
          .forEach(property -> properties.add(propertySnapshot(property)));
    }
    state.put("properties", properties);
    return objectMapper.valueToTree(state);
  }

  private static Map<String, Object> propertySnapshot(DatasheetDefinitionProperty property) {
    Map<String, Object> state = new LinkedHashMap<>();
    state.put("code", property.getCode());
    state.put("platformId", property.getPlatformId());
    state.put("groupTag", property.getGroupTag());
    state.put("propertyType", property.getPropertyType());
    return state;
  }
}
