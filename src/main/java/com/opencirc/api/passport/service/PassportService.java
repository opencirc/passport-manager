package com.opencirc.api.passport.service;

import com.opencirc.api.passport.adapter.PlatformAdapterFactory;
import com.opencirc.api.passport.auth.principal.ApiKeyPrincipal;
import com.opencirc.api.passport.constants.AppConstants;
import com.opencirc.api.passport.dao.DatasheetRepository;
import com.opencirc.api.passport.dao.PassportRepository;
import com.opencirc.api.passport.dto.CreatePassportUsingPlatformRequestDto;
import com.opencirc.api.passport.dto.DataDictionaryTreeStructureDto;
import com.opencirc.api.passport.dto.DatasheetDto;
import com.opencirc.api.passport.dto.DatasheetPropertyDto;
import com.opencirc.api.passport.dto.PassportDto;
import com.opencirc.api.passport.dto.UpdateDataRequestDto;
import com.opencirc.api.passport.dto.query.PassportDatasheetResultMapQueryResult;
import com.opencirc.api.passport.enums.DataDictionary;
import com.opencirc.api.passport.enums.PassportLogAction;
import com.opencirc.api.passport.enums.Platform;
import com.opencirc.api.passport.exception.ForbiddenException;
import com.opencirc.api.passport.exception.InvalidDataDictionaryException;
import com.opencirc.api.passport.exception.InvalidInputException;
import com.opencirc.api.passport.exception.JsonValidationException;
import com.opencirc.api.passport.model.Datasheet;
import com.opencirc.api.passport.model.DatasheetDefinition;
import com.opencirc.api.passport.model.DatasheetDefinitionProperty;
import com.opencirc.api.passport.model.Passport;
import io.github.thibaultmeyer.cuid.CUID;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Service class for passport operations. Every operation runs on behalf of a machine credential
 * ({@link ApiKeyPrincipal}) and is confined to that credential's project: the check is a single
 * comparison of the passport's {@code projectId}, no hierarchy traversal.
 */
@Service
public class PassportService {

  private static final String ERR_PARENT_NOT_FOUND = "Invalid parentId: active parent not found";

  private static final String ERR_CHILD_REQUIRES_PARENT =
      "A parentId is required: root passports are provisioned together with their project";

  private final DatasheetRepository datasheetRepository;

  private final DatasheetDefinitionService datasheetDefinitionService;

  private final PassportRepository passportRepository;

  private final ObjectMapper objectMapper;

  private final PlatformAdapterFactory platformAdapterFactory;

  private final EpdEnrichmentService epdEnrichmentService;

  private final PassportLogService passportLogService;

  private static final String EPD_TRIGGER_PLATFORM_ID =
      "https://identifier.buildingsmart.org/uri/LCA/LCA/3.0/class/GeneralInformation/prop/ID/referencetooriginalEPD";
  private static final String EPD_TRIGGER_CODE = "referencetooriginalEPD";
  private static final String EPD_TRIGGER_GROUP = "GeneralInformation";

  private boolean isEpdTriggerProperty(DatasheetDefinitionProperty property) {
    if (property == null) {
      return false;
    }
    return EPD_TRIGGER_PLATFORM_ID.equals(property.getPlatformId())
        || (EPD_TRIGGER_CODE.equals(property.getCode())
            && EPD_TRIGGER_GROUP.equals(property.getGroupTag()));
  }

  /** Constructor. */
  public PassportService(
      DatasheetRepository datasheetRepository,
      DatasheetDefinitionService datasheetDefinitionService,
      PassportRepository passportRepository,
      PlatformAdapterFactory platformAdapterFactory,
      ObjectMapper objectMapper,
      EpdEnrichmentService epdEnrichmentService,
      PassportLogService passportLogService) {
    this.datasheetRepository = datasheetRepository;
    this.datasheetDefinitionService = datasheetDefinitionService;
    this.passportRepository = passportRepository;
    this.platformAdapterFactory = platformAdapterFactory;
    this.objectMapper = objectMapper;
    this.epdEnrichmentService = epdEnrichmentService;
    this.passportLogService = passportLogService;
  }

  /** Creates multiple child passports in the actor's project. */
  @Transactional
  public List<PassportDto> batchCreatePassportsUsingPlatform(
      Platform platform,
      List<CreatePassportUsingPlatformRequestDto> dataArray,
      ApiKeyPrincipal actor)
      throws InvalidInputException, JsonValidationException {

    if (dataArray == null || dataArray.isEmpty()) {
      return Collections.emptyList();
    }

    // Fast-fail check: ensure all external parent references exist and are active
    Map<String, CreatePassportUsingPlatformRequestDto> idToDto = new HashMap<>();
    for (var passportDto : dataArray) {
      if (passportDto.getId() != null && !passportDto.getId().isBlank()) {
        if (idToDto.putIfAbsent(passportDto.getId(), passportDto) != null) {
          throw new InvalidInputException("Duplicate passport ID in batch");
        }
        if (passportRepository.existsById(passportDto.getId())) {
          throw new InvalidInputException("Passport ID already exists");
        }
      }
    }

    for (var passportDto : dataArray) {
      String parentId = passportDto.getParentId();
      if (parentId == null || parentId.isBlank()) {
        throw new ResponseStatusException(
            HttpStatus.UNPROCESSABLE_ENTITY, ERR_CHILD_REQUIRES_PARENT);
      }
      if (!idToDto.containsKey(parentId)) {
        requireActiveParent(parentId, actor);
      }
    }

    List<CreatePassportUsingPlatformRequestDto> sortedData = topologicalSort(dataArray);

    java.util.Map<CreatePassportUsingPlatformRequestDto, PassportDto> resultMap =
        new java.util.IdentityHashMap<>();
    for (var passportData : sortedData) {
      resultMap.put(passportData, createPassportUsingPlatform(platform, passportData, actor, true));
    }

    var passportDtos = new ArrayList<PassportDto>();
    for (var passportData : dataArray) {
      passportDtos.add(resultMap.get(passportData));
    }
    return passportDtos;
  }

  /**
   * Sorts the batch of passport creation requests in topological order using Kahn's algorithm.
   * Ensures that parent passports are processed and created before their children.
   *
   * <p>Step 1: Map all batch passport IDs to their corresponding request DTOs. Step 2: Build the
   * adjacency list and in-degree counts via {@link #buildAdjacencyList}. Step 3: Queue all root
   * passports with in-degree == 0 (no dependencies within the batch). Step 4: Iteratively dequeue
   * elements, append them to sorted output, and decrement child in-degrees. Step 5: Verify all
   * passports were sorted; if counts differ, a circular dependency exists.
   *
   * @param dataArray the batch of passport creation requests
   * @return the topologically sorted list of requests
   * @throws InvalidInputException if a circular dependency is detected
   */
  private List<CreatePassportUsingPlatformRequestDto> topologicalSort(
      List<CreatePassportUsingPlatformRequestDto> dataArray) {
    if (dataArray == null || dataArray.isEmpty()) {
      return dataArray;
    }

    // Step 1: Map all batch passport IDs to their corresponding DTOs
    Map<String, CreatePassportUsingPlatformRequestDto> idToDto = new HashMap<>();
    for (var passportDto : dataArray) {
      if (passportDto.getId() != null && !passportDto.getId().isBlank()) {
        idToDto.put(passportDto.getId(), passportDto);
      }
    }

    // Step 2: Build adjacency list and compute in-degrees for each passport
    Map<CreatePassportUsingPlatformRequestDto, Integer> inDegree =
        new java.util.IdentityHashMap<>();
    Map<String, List<CreatePassportUsingPlatformRequestDto>> childrenMap =
        buildAdjacencyList(dataArray, idToDto, inDegree);

    // Step 3: Queue all passports with an in-degree of 0 (no dependencies within the batch)
    java.util.Queue<CreatePassportUsingPlatformRequestDto> queue = new java.util.LinkedList<>();
    for (var passportDto : dataArray) {
      if (inDegree.get(passportDto) == 0) {
        queue.add(passportDto);
      }
    }

    // Step 4: Process the queue using Kahn's algorithm
    List<CreatePassportUsingPlatformRequestDto> sortedData = new ArrayList<>();
    while (!queue.isEmpty()) {
      var current = queue.poll();
      sortedData.add(current);

      if (current.getId() != null && childrenMap.containsKey(current.getId())) {
        for (var child : childrenMap.get(current.getId())) {
          inDegree.put(child, inDegree.get(child) - 1);
          if (inDegree.get(child) == 0) {
            queue.add(child);
          }
        }
      }
    }

    // Step 5: Check if all items were sorted. If not, a circular dependency is present.
    if (sortedData.size() != dataArray.size()) {
      throw new InvalidInputException("Circular dependency detected in passport batch creation");
    }

    return sortedData;
  }

  /**
   * Builds the adjacency list and initializes in-degree counts for batch passports.
   *
   * <p>Step 1: Initialize in-degree count to 0 for each passport in the batch. Step 2: For each
   * passport, check if it references a parent present in this batch. Step 3: If the parent is in
   * the batch, map the parent to this child and increment in-degree.
   *
   * @param dataArray the batch of passport creation requests
   * @param idToDto map of passport IDs to request DTOs
   * @param inDegree map to populate with in-degree counts
   * @return map of parent ID to list of child request DTOs within the batch
   */
  private Map<String, List<CreatePassportUsingPlatformRequestDto>> buildAdjacencyList(
      List<CreatePassportUsingPlatformRequestDto> dataArray,
      Map<String, CreatePassportUsingPlatformRequestDto> idToDto,
      Map<CreatePassportUsingPlatformRequestDto, Integer> inDegree) {
    Map<String, List<CreatePassportUsingPlatformRequestDto>> childrenMap = new HashMap<>();

    // Step 1: Initialize in-degrees to 0
    for (var passportDto : dataArray) {
      inDegree.put(passportDto, 0);
    }

    // Step 2 & 3: For each passport with a parent in the batch, record relationship and update
    // in-degree
    for (var passportDto : dataArray) {
      String parentId = passportDto.getParentId();
      if (parentId != null && !parentId.isBlank() && idToDto.containsKey(parentId)) {
        childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(passportDto);
        inDegree.put(passportDto, inDegree.get(passportDto) + 1);
      }
    }

    return childrenMap;
  }

  /** Creates a child passport in the actor's project. */
  @Transactional
  public PassportDto createPassportUsingPlatform(
      Platform platform, CreatePassportUsingPlatformRequestDto data, ApiKeyPrincipal actor)
      throws InvalidInputException, JsonValidationException {
    return createPassportUsingPlatform(platform, data, actor, false);
  }

  /** Creates a child passport in the actor's project. */
  @Transactional
  public PassportDto createPassportUsingPlatform(
      Platform platform,
      CreatePassportUsingPlatformRequestDto data,
      ApiKeyPrincipal actor,
      boolean asBatchOperation)
      throws InvalidInputException, JsonValidationException {

    String id = data.getId();
    if (id == null || id.isBlank()) {
      int customLength = AppConstants.CUID_LENGTH;
      CUID cuid = CUID.randomCUID2(customLength);
      id = cuid.toString();
    }

    if (passportRepository.existsById(id)) {
      throw new InvalidInputException("Passport ID already exists");
    }

    String parentId = data.getParentId();
    if (parentId == null || parentId.isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, ERR_CHILD_REQUIRES_PARENT);
    }
    requireActiveParent(parentId, actor);

    Passport passport = new Passport();
    passport.setId(id);
    passport.setProjectId(actor.getProjectId());
    passport.setName(data.getName());
    passport.setStatus(Passport.Status.ACTIVE);
    passport.setParentId(parentId);
    passport.setCreatedById(actor.getKeyId());
    passport.setCreatedTime(OffsetDateTime.now());
    passport.setDatasheets(new HashSet<>());

    passport = passportRepository.save(passport);
    passportLogService.logEvent(passport.getId(), PassportLogAction.CREATE, List.of());

    addDatasheetsToPassportUsingPlatform(
        passport,
        platform,
        data.getPlatformId(),
        Datasheet.DataCategory.fromValue(data.getDataCategory()),
        actor,
        true);

    if (asBatchOperation) {
      // @TODO this is an INSANELY ugly hack, but is needed for now.
      addDatasheetsToPassportUsingPlatform(
          passport,
          platform,
          "https://identifier.buildingsmart.org/uri/LCA/LCA/3.0/class/GeneralInformation",
          Datasheet.DataCategory.fromValue(data.getDataCategory()),
          actor,
          false);
    }

    passport =
        passportRepository
            .findById(passport.getId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Passport not found"));

    var dataValues = data.getValues();
    if (dataValues != null) {
      var datasheets = passport.getDatasheets();
      if (datasheets != null && !datasheets.isEmpty()) {
        Map<String, Object> updateValues = new HashMap<>();
        for (var datasheet : datasheets) {
          if (datasheet == null || datasheet.getDefinition() == null) {
            continue;
          }

          var properties = datasheet.getDefinition().getProperties();
          if (properties == null || properties.isEmpty()) {
            continue;
          }

          for (var property : properties) {
            if (dataValues.containsKey(property.getCode())) {
              updateValues.put(property.getId(), dataValues.get(property.getCode()));
            }
          }
        }

        if (!updateValues.isEmpty()) {
          updateData(passport.getId(), updateValues, actor);
        }
      }
    }

    return PassportDto.from(passport);
  }

  /**
   * Loads an active parent in the actor's project. A parent outside the project is reported the
   * same way as a missing one so passports of other projects are not revealed.
   */
  private Passport requireActiveParent(String parentId, ApiKeyPrincipal actor) {
    Passport parent =
        passportRepository
            .findPassport(parentId, Passport.Status.ACTIVE)
            .orElseThrow(
                () ->
                    new HttpServerErrorException(
                        HttpStatus.UNPROCESSABLE_ENTITY, ERR_PARENT_NOT_FOUND));
    if (!Objects.equals(parent.getProjectId(), actor.getProjectId())) {
      throw new HttpServerErrorException(HttpStatus.UNPROCESSABLE_ENTITY, ERR_PARENT_NOT_FOUND);
    }
    return parent;
  }

  /** Loads a passport of any status and verifies it belongs to the actor's project. */
  private Passport loadOwnedPassport(String passportId, ApiKeyPrincipal actor) {
    Passport passport =
        passportRepository
            .findById(passportId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Passport not found"));
    assertOwned(passport, actor);
    return passport;
  }

  private static void assertOwned(Passport passport, ApiKeyPrincipal actor) {
    if (!Objects.equals(passport.getProjectId(), actor.getProjectId())) {
      throw new ForbiddenException("Passport belongs to another project");
    }
  }

  /** Removes a datasheet from the passport. */
  @Transactional
  public PassportDto removeDatasheet(String passportId, String datasheetId, ApiKeyPrincipal actor) {
    Passport passport = loadOwnedPassport(passportId, actor);

    Datasheet datasheetToRemove =
        passport.getDatasheets().stream()
            .filter(datasheet -> datasheetId.equals(datasheet.getId()))
            .findFirst()
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Datasheet mapping not found"));

    passport.getDatasheets().remove(datasheetToRemove);
    datasheetRepository.delete(datasheetToRemove);

    DatasheetDefinition definition = datasheetToRemove.getDefinition();
    passportLogService.logEvent(
        passportId,
        PassportLogAction.REMOVE_DATASHEET,
        List.of(
            Map.of(
                "datasheetId",
                datasheetId,
                "datasheetName",
                definition != null && definition.getName() != null ? definition.getName() : "")));

    return PassportDto.from(passport);
  }

  /** Moves a passport under another active passport of the same project. */
  @Transactional
  public PassportDto updateParent(String passportId, String newParentId, ApiKeyPrincipal actor) {
    Passport passport = loadOwnedPassport(passportId, actor);

    final String oldParentId = passport.getParentId();

    if (newParentId == null || newParentId.isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "A passport cannot be detached from its parent; "
              + "the root passport belongs to the project");
    }

    var visitedParentIds = new HashSet<String>();
    String ancestorId = newParentId;
    while (ancestorId != null && !ancestorId.isBlank()) {
      if (passportId.equals(ancestorId) || !visitedParentIds.add(ancestorId)) {
        throw new InvalidInputException("Parent relationship would contain a cycle");
      }
      ancestorId = passportRepository.getParentId(ancestorId);
    }
    requireActiveParent(newParentId, actor);

    passport.setParentId(newParentId);
    passport = passportRepository.save(passport);

    passportLogService.logEvent(
        passportId,
        PassportLogAction.UPDATE_RELATIONSHIPS,
        List.of(
            Map.of(
                "oldParentId",
                oldParentId != null ? oldParentId : "",
                "newParentId",
                newParentId)));

    return PassportDto.from(passport);
  }

  /**
   * Creates a datasheet and adds it to the passport using information from the provided platform.
   */
  @Transactional
  public PassportDto addDatasheetsToPassportUsingPlatform(
      String passportId,
      Platform platform,
      String platformId,
      Datasheet.DataCategory dataCategory,
      ApiKeyPrincipal actor)
      throws JsonValidationException {
    Passport passport = loadOwnedPassport(passportId, actor);
    if (passport.getStatus() != Passport.Status.ACTIVE) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passport is not active");
    }
    return addDatasheetsToPassportUsingPlatform(
        passport, platform, platformId, dataCategory, actor, false);
  }

  /**
   * Attaches datasheet instances to the passport for the given classification, resolving the shared
   * definitions from the global catalogue. A definition already attached to the passport is
   * skipped.
   */
  @Transactional
  public PassportDto addDatasheetsToPassportUsingPlatform(
      Passport passport,
      Platform platform,
      String platformId,
      Datasheet.DataCategory dataCategory,
      ApiKeyPrincipal actor,
      boolean addRelatedIfcEntities)
      throws JsonValidationException {
    List<DatasheetDefinition> definitions =
        datasheetDefinitionService.resolveDefinitions(platform, platformId, addRelatedIfcEntities);

    if (passport.getDatasheets() == null) {
      passport.setDatasheets(new HashSet<>());
    }

    for (DatasheetDefinition definition : definitions) {
      if (isDefinitionAttached(passport, definition)) {
        continue;
      }

      Datasheet datasheet = new Datasheet();
      datasheet.setPassport(passport);
      datasheet.setDefinition(definition);
      datasheet.setDataCategory(dataCategory);
      datasheet.setCreatedById(actor != null ? actor.getKeyId() : null);
      datasheet = datasheetRepository.save(datasheet);
      passport.getDatasheets().add(datasheet);

      passportLogService.logEvent(
          passport.getId(),
          PassportLogAction.ADD_DATASHEET,
          List.of(
              Map.of(
                  "datasheetId",
                  datasheet.getId() != null ? datasheet.getId() : "",
                  "datasheetName",
                  definition.getName() != null ? definition.getName() : "")));
    }

    return PassportDto.from(passport);
  }

  private boolean isDefinitionAttached(Passport passport, DatasheetDefinition definition) {
    return passport.getDatasheets().stream()
        .map(Datasheet::getDefinition)
        .filter(Objects::nonNull)
        .anyMatch(
            attached ->
                (attached.getId() != null && attached.getId().equals(definition.getId()))
                    || (attached.getPlatformId() != null
                        && attached.getPlatformId().equals(definition.getPlatformId())));
  }

  /** Retrieves a passport of the actor's project; archived ones only when requested. */
  @Transactional(readOnly = true)
  public PassportDto getPassport(
      String passportId, ApiKeyPrincipal actor, boolean includeArchived) {
    Optional<Passport> optionalPassport =
        passportRepository.findByIdInProject(passportId, actor.getProjectId());
    if (optionalPassport.isEmpty()
        || (!includeArchived && optionalPassport.get().getStatus() == Passport.Status.ARCHIVED)) {
      throw new HttpServerErrorException(
          HttpStatus.NOT_FOUND, "Could not find passport with ID " + passportId);
    }

    return PassportDto.from(optionalPassport.get());
  }

  /** Retrieves a passport of the actor's project and its non-archived descendants. */
  @Transactional(readOnly = true)
  public List<PassportDto> getPassportChildren(String id, ApiKeyPrincipal actor) {
    List<PassportDatasheetResultMapQueryResult> resultRows =
        passportRepository
            .findPassportWithDescendants(id, actor.getProjectId())
            .orElse(Collections.emptyList());

    if (resultRows.isEmpty()) {
      throw new HttpServerErrorException(
          HttpStatus.NOT_FOUND, "No active passports found with children");
    }

    Map<String, PassportDto> passportMap = new LinkedHashMap<>();

    for (PassportDatasheetResultMapQueryResult row : resultRows) {
      PassportDto passportDto =
          passportMap.computeIfAbsent(row.getPassportId(), key -> buildPassportDto(row));

      if (row.getDatasheetId() != null) {
        DatasheetDto datasheetDto =
            passportDto.getDatasheets().stream()
                .filter(datasheet -> Objects.equals(datasheet.getId(), row.getDatasheetId()))
                .findFirst()
                .orElseGet(
                    () -> {
                      DatasheetDto newDatasheet = buildDatasheetDto(row);
                      passportDto.getDatasheets().add(newDatasheet);
                      return newDatasheet;
                    });

        if (row.getDatasheetPropertyId() != null) {
          boolean alreadyExists =
              datasheetDto.getDatasheetProperties().stream()
                  .anyMatch(
                      property -> Objects.equals(property.getId(), row.getDatasheetPropertyId()));
          if (!alreadyExists) {
            DatasheetPropertyDto propertyDto = buildDatasheetProperty(row);
            if (propertyDto != null) {
              datasheetDto.getDatasheetProperties().add(propertyDto);
            }
          }
        }
      }
    }

    return new ArrayList<>(passportMap.values());
  }

  /** Retrieves the non-archived passports with the given parent ID in the actor's project. */
  @Transactional(readOnly = true)
  public List<PassportDto> getImmediateChildren(String passportId, ApiKeyPrincipal actor) {
    Passport parent =
        passportRepository
            .findById(passportId)
            .orElseThrow(
                () ->
                    new HttpServerErrorException(
                        HttpStatus.NOT_FOUND, "Could not find passport with ID " + passportId));
    assertOwned(parent, actor);
    List<PassportDatasheetResultMapQueryResult> resultRows =
        passportRepository
            .findImmediateChildren(passportId, actor.getProjectId())
            .orElse(Collections.emptyList());

    return assemblePassportsFromResultRows(resultRows);
  }

  /** Retrieves the non-archived root passports of the actor's project. */
  @Transactional(readOnly = true)
  public List<PassportDto> getRootPassports(ApiKeyPrincipal actor) {
    List<Passport> passports = passportRepository.getRootPassports(actor.getProjectId());
    if (passports == null) {
      throw new HttpServerErrorException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Could not retrieve root passports");
    }
    return passports.stream().map(PassportDto::from).collect(Collectors.toList());
  }

  /**
   * Soft-archives a passport and its whole subtree. The root passport cannot be archived on its
   * own; archiving the project does that. Nothing is deleted.
   */
  @Transactional
  public PassportDto archivePassport(String passportId, ApiKeyPrincipal actor) {
    Passport passport = loadOwnedPassport(passportId, actor);
    if (passport.getParentId() == null) {
      throw new InvalidInputException("The root passport is archived together with its project");
    }
    if (passport.getStatus() == Passport.Status.ARCHIVED) {
      return PassportDto.from(passport);
    }

    int archived = passportRepository.archiveSubtree(passportId, OffsetDateTime.now());
    passportLogService.logEvent(
        passportId, PassportLogAction.ARCHIVE, List.of(Map.of("archivedPassports", archived)));

    return passportRepository
        .findById(passportId)
        .map(PassportDto::from)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Passport not found"));
  }

  private PassportDto buildPassportDto(PassportDatasheetResultMapQueryResult row) {
    PassportDto dto = new PassportDto();
    dto.setId(row.getPassportId());
    dto.setProjectId(row.getProjectId());
    dto.setParentId(row.getParentId());
    dto.setName(row.getPassportName());
    dto.setStatus(row.getStatus());
    dto.setCreatedById(row.getPassportCreatedById());
    if (row.getPassportCreatedTime() != null) {
      dto.setCreatedTime(row.getPassportCreatedTime().atOffset(ZoneOffset.UTC));
    }
    dto.setDatasheets(new ArrayList<>());
    return dto;
  }

  private DatasheetDto buildDatasheetDto(PassportDatasheetResultMapQueryResult row) {
    try {
      DatasheetDto dto = new DatasheetDto();
      dto.setId(row.getDatasheetId());

      dto.setPlatform(row.getPlatform());

      dto.setDictionary(row.getDictionary());

      dto.setCode(row.getDatasheetCode());
      dto.setName(row.getDatasheetName());
      dto.setDescription(row.getDatasheetDescription());
      dto.setPlatformId(row.getDatasheetPlatformId());
      dto.setDataCategory(row.getDataCategory());

      Map<String, Object> dataMap = null;
      String data = row.getData();
      if (data != null && !data.isBlank()) {
        dataMap = objectMapper.readValue(data, new tools.jackson.core.type.TypeReference<>() {});
      }
      dto.setData(dataMap);

      dto.setCreatedById(row.getDatasheetCreatedById());
      if (row.getDatasheetCreatedTime() != null) {
        dto.setCreatedTime(row.getDatasheetCreatedTime().atOffset(ZoneOffset.UTC));
      }
      dto.setDatasheetProperties(new ArrayList<>());

      return dto;
    } catch (JacksonException e) {
      throw new RuntimeException(
          "Error parsing datasheet JSON for datasheetId="
              + row.getDatasheetId()
              + ", passportId="
              + row.getPassportId(),
          e);
    }
  }

  private DatasheetPropertyDto buildDatasheetProperty(PassportDatasheetResultMapQueryResult row) {

    if (row.getDatasheetPropertyId() == null) {
      return null;
    }

    DatasheetPropertyDto propertyDto = new DatasheetPropertyDto();

    propertyDto.setId(row.getDatasheetPropertyId());
    propertyDto.setDatasheetId(row.getDatasheetId());
    propertyDto.setCode(row.getDatasheetPropertyCode());
    propertyDto.setPlatformId(row.getDatasheetPropertyPlatformId());
    propertyDto.setGroupTag(row.getDatasheetPropertyGroupTag());
    propertyDto.setPropertyType(row.getDatasheetPropertyType());

    String definition = row.getDatasheetPropertyDefinition();
    if (definition != null && !definition.isBlank()) {
      try {
        JsonNode definitionNode = objectMapper.readTree(definition);
        propertyDto.setDefinition(definitionNode);
      } catch (JacksonException e) {
        throw new RuntimeException(
            "Error parsing datasheet property JSON for datasheetPropertyId = "
                + row.getDatasheetPropertyId()
                + ", datasheetId = "
                + row.getDatasheetId()
                + ", passportId = "
                + row.getPassportId(),
            e);
      }
    } else {
      propertyDto.setDefinition(null);
    }

    return propertyDto;
  }

  /** Update passport datasheet data from a DTO. */
  @Transactional
  public PassportDto updateData(
      String passportId, UpdateDataRequestDto updateDataRequestDto, ApiKeyPrincipal actor) {
    return updateData(passportId, updateDataRequestDto.getValues(), actor);
  }

  /**
   * Updates datasheet property values for the given passport and group. Only properties present in
   * {@code values} are updated.
   */
  @Transactional
  public PassportDto updateData(
      String passportId, Map<String, Object> values, ApiKeyPrincipal actor) {
    Passport passport =
        passportRepository
            .findPassport(passportId, Passport.Status.ACTIVE)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Passport not found"));
    assertOwned(passport, actor);

    var datasheets = passport.getDatasheets();
    if (datasheets == null || datasheets.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Passport does not have any datasheet mappings: " + passportId);
    }

    List<Map<String, Object>> allChanges = new ArrayList<>();
    String epdUrlToEnrich = null;
    String epdTriggerPropertyId = null;
    for (Datasheet datasheet : datasheets) {
      if (datasheet == null || datasheet.getDefinition() == null) {
        continue;
      }
      var properties = datasheet.getDefinition().getProperties();
      if (properties == null) {
        continue;
      }

      // Check for EPD enrichment trigger
      if (epdUrlToEnrich == null) {
        for (DatasheetDefinitionProperty property : properties) {
          if (isEpdTriggerProperty(property)) {
            Object triggerValue = values.get(property.getId());
            if (triggerValue instanceof String epdUrl && !epdUrl.isBlank()) {
              epdUrlToEnrich = epdUrl;
              epdTriggerPropertyId = property.getId();
              break;
            }
          }
        }
      }

      ObjectNode dataNode =
          datasheet.getData() != null
              ? objectMapper.valueToTree(datasheet.getData())
              : JsonNodeFactory.instance.objectNode();

      boolean isChanged = false;

      // List<String> errorMessages = new ArrayList<>();
      for (DatasheetDefinitionProperty property : properties) {
        String propertyId = property.getId();

        if (!values.containsKey(propertyId)) {
          continue;
        }

        JsonNode newValueNode = objectMapper.valueToTree(values.get(propertyId));
        JsonNode existingValueNode = dataNode.get(propertyId);
        if (Objects.equals(newValueNode, existingValueNode)) {
          continue;
        }

        isChanged = true;
        dataNode.set(propertyId, newValueNode);

        Map<String, Object> diff = new HashMap<>();
        diff.put("propertyId", propertyId);
        diff.put("datasheetId", datasheet.getId());
        diff.put(
            "old",
            existingValueNode != null
                ? objectMapper.convertValue(existingValueNode, Object.class)
                : "");
        diff.put("new", values.get(propertyId) != null ? values.get(propertyId) : "");
        allChanges.add(diff);
      }

      if (isChanged) {
        datasheet.setData(
            objectMapper.convertValue(dataNode, new tools.jackson.core.type.TypeReference<>() {}));
        datasheetRepository.save(datasheet);
      }
    }

    if (!allChanges.isEmpty()) {
      passportLogService.logEvent(passportId, PassportLogAction.UPDATE_PROPERTIES, allChanges);
    }

    if (epdUrlToEnrich != null) {
      String enrichmentPassportId = passport.getId();
      String enrichmentUrl = epdUrlToEnrich;
      String triggerPropertyId = epdTriggerPropertyId;
      PassportLogService.Actor enrichmentActor = passportLogService.captureActor();
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              epdEnrichmentService.enrich(
                  enrichmentPassportId, enrichmentUrl, triggerPropertyId, enrichmentActor);
            }
          });
    }

    // if (updatedProperties.isEmpty()) {
    //  throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No valid properties found");
    // }

    return PassportDto.from(passport);
  }

  /** Retrieves the non-archived passports of the actor's project carrying the given code. */
  @Transactional(readOnly = true)
  public List<PassportDto> listPassportsByCode(String code, ApiKeyPrincipal actor) {

    List<PassportDatasheetResultMapQueryResult> resultRows =
        passportRepository
            .findPassportsByCode(code, actor.getProjectId())
            .orElse(Collections.emptyList());

    return assemblePassportsFromResultRows(resultRows);
  }

  /** This method sets the result sets to passport dto. */
  private List<PassportDto> assemblePassportsFromResultRows(
      List<PassportDatasheetResultMapQueryResult> resultRows) {

    Map<String, PassportDto> passportMap = new LinkedHashMap<>();

    for (PassportDatasheetResultMapQueryResult row : resultRows) {

      PassportDto passportDto =
          passportMap.computeIfAbsent(row.getPassportId(), id -> buildPassportDto(row));

      if (row.getDatasheetId() != null) {

        DatasheetDto datasheetDto =
            passportDto.getDatasheets().stream()
                .filter(datasheet -> Objects.equals(datasheet.getId(), row.getDatasheetId()))
                .findFirst()
                .orElseGet(
                    () -> {
                      DatasheetDto newDatasheet = buildDatasheetDto(row);
                      passportDto.getDatasheets().add(newDatasheet);
                      return newDatasheet;
                    });

        if (row.getDatasheetPropertyId() != null) {

          boolean exists =
              datasheetDto.getDatasheetProperties().stream()
                  .anyMatch(
                      datasheetProperty ->
                          Objects.equals(datasheetProperty.getId(), row.getDatasheetPropertyId()));

          if (!exists) {
            DatasheetPropertyDto propertyDto = buildDatasheetProperty(row);
            datasheetDto.getDatasheetProperties().add(propertyDto);
          }
        }
      }
    }

    return new ArrayList<>(passportMap.values());
  }

  /** Parse and form the tree structure of the platform. */
  public List<DataDictionaryTreeStructureDto> getDictionaryTreeStructure(
      Platform platform, DataDictionary dictionary)
      throws IOException, InvalidDataDictionaryException {
    return this.platformAdapterFactory.getAdapter(platform).getDictionaryTreeStructure(dictionary);
  }
}
