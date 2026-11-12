package com.opencirc.api.passport.service;

import com.opencirc.api.passport.constants.AppConstants;
import com.opencirc.api.passport.dao.AccessGroupRepository;
import com.opencirc.api.passport.dao.ApiKeyRepository;
import com.opencirc.api.passport.dao.OwnerRepository;
import com.opencirc.api.passport.dao.PassportRepository;
import com.opencirc.api.passport.dao.ProjectRepository;
import com.opencirc.api.passport.dto.CreateProjectRequestDto;
import com.opencirc.api.passport.dto.CreatedByDto;
import com.opencirc.api.passport.dto.ProjectDto;
import com.opencirc.api.passport.dto.UpdateProjectRequestDto;
import com.opencirc.api.passport.enums.ApiKeyStatus;
import com.opencirc.api.passport.enums.AuditLogAction;
import com.opencirc.api.passport.enums.AuditTargetType;
import com.opencirc.api.passport.enums.ProjectStatus;
import com.opencirc.api.passport.exception.InvalidInputException;
import com.opencirc.api.passport.model.Passport;
import com.opencirc.api.passport.model.Project;
import io.github.thibaultmeyer.cuid.CUID;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Administrative project lifecycle: atomic provisioning of a project with its root passport, soft
 * archiving that cascades to the passport tree, and restoration.
 */
@Service
public class ProjectService {

  private final ProjectRepository projectRepository;

  private final OwnerRepository ownerRepository;

  private final PassportRepository passportRepository;

  private final AuditLogService auditLogService;

  private final ApiKeyRepository apiKeyRepository;

  private final AccessGroupRepository accessGroupRepository;

  /** Constructor. */
  public ProjectService(
      ProjectRepository projectRepository,
      OwnerRepository ownerRepository,
      PassportRepository passportRepository,
      AuditLogService auditLogService,
      ApiKeyRepository apiKeyRepository,
      AccessGroupRepository accessGroupRepository) {
    this.projectRepository = projectRepository;
    this.ownerRepository = ownerRepository;
    this.passportRepository = passportRepository;
    this.auditLogService = auditLogService;
    this.apiKeyRepository = apiKeyRepository;
    this.accessGroupRepository = accessGroupRepository;
  }

  /**
   * Creates a project and its root passport in one transaction. The root passport has no parent and
   * no creating API key; it is the anchor every machine-authored child hangs from.
   */
  @Transactional
  public ProjectDto createProject(
      CreateProjectRequestDto request, CreatedByDto createdBy, String createdById) {
    if (request == null || request.getName() == null || request.getName().isBlank()) {
      throw new InvalidInputException("Project name is required");
    }
    String ownerId = request.getOwnerId();
    if (ownerId != null && !ownerId.isBlank() && !ownerRepository.existsById(ownerId)) {
      throw new InvalidInputException("No owner found with id: " + ownerId);
    }

    Project project = new Project();
    project.setName(request.getName().trim());
    project.setDescription(request.getDescription());
    project.setOwnerId(ownerId != null && !ownerId.isBlank() ? ownerId : null);
    project.setStatus(ProjectStatus.ACTIVE);
    project.setCreatedById(createdById);
    project.setCreatedBy(createdBy);
    project = projectRepository.saveAndFlush(project);

    Passport root = new Passport();
    root.setId(CUID.randomCUID2(AppConstants.CUID_LENGTH).toString());
    root.setProjectId(project.getId());
    root.setName(project.getName());
    root.setStatus(Passport.Status.ACTIVE);
    root.setParentId(null);
    root.setCreatedById(null);
    root.setDatasheets(new HashSet<>());
    root = passportRepository.saveAndFlush(root);
    auditLogService.record(
        AuditTargetType.PASSPORT,
        root.getId(),
        project.getId(),
        root.getId(),
        AuditLogAction.CREATE,
        null,
        auditLogService.passportSnapshot(root),
        AuditLogService.Actor.system());

    return ProjectDto.from(project, root.getId());
  }

  /**
   * Updates the name, description and owner of an active project. Ownership is set as given: a
   * missing or blank owner id leaves the project without an owner.
   */
  @Transactional
  public ProjectDto updateProject(String id, UpdateProjectRequestDto request) {
    if (request == null || request.getName() == null || request.getName().isBlank()) {
      throw new InvalidInputException("Project name is required");
    }
    Project project = requireActiveProject(id);
    String ownerId = request.getOwnerId();
    if (ownerId != null && !ownerId.isBlank() && !ownerRepository.existsById(ownerId)) {
      throw new InvalidInputException("No owner found with id: " + ownerId);
    }
    project.setName(request.getName().trim());
    project.setDescription(request.getDescription());
    project.setOwnerId(ownerId != null && !ownerId.isBlank() ? ownerId : null);
    return toDto(projectRepository.save(project));
  }

  /** Lists projects; archived ones only when requested. */
  @Transactional(readOnly = true)
  public List<ProjectDto> listProjects(boolean includeArchived) {
    List<Project> projects =
        includeArchived
            ? projectRepository.findAllByOrderByCreatedTimeAsc()
            : projectRepository.findAllByStatusOrderByCreatedTimeAsc(ProjectStatus.ACTIVE);
    return projects.stream().map(this::toDto).toList();
  }

  /** Retrieves a project; an archived one only when requested. */
  @Transactional(readOnly = true)
  public ProjectDto getProject(String id, boolean includeArchived) {
    Project project = requireProject(id);
    if (!includeArchived && project.getStatus() == ProjectStatus.ARCHIVED) {
      throw notFound(id);
    }
    return toDto(project);
  }

  /** Soft-archives a project and every passport in it. Nothing is deleted. */
  @Transactional
  public ProjectDto archiveProject(String id) {
    Project project = requireProject(id);
    if (project.getStatus() == ProjectStatus.ARCHIVED) {
      return toDto(project);
    }
    OffsetDateTime now = OffsetDateTime.now();
    project.setStatus(ProjectStatus.ARCHIVED);
    project.setArchivedTime(now);
    project = projectRepository.save(project);
    final String projectId = project.getId();

    passportRepository
        .findFirstByProjectIdAndParentIdIsNull(projectId)
        .ifPresent(
            root -> {
              final var before = auditLogService.passportSnapshot(root);
              passportRepository.archiveByProjectId(projectId, now, Passport.Status.ARCHIVED);
              root.setStatus(Passport.Status.ARCHIVED);
              root.setArchivedTime(now);
              auditLogService.record(
                  AuditTargetType.PASSPORT,
                  root.getId(),
                  projectId,
                  root.getId(),
                  AuditLogAction.UPDATE,
                  before,
                  auditLogService.passportSnapshot(root),
                  AuditLogService.Actor.system());
            });
    return toDto(project);
  }

  /** Restores an archived project and its root passport to the active state. */
  @Transactional
  public ProjectDto restoreProject(String id) {
    Project project = requireProject(id);
    if (project.getStatus() == ProjectStatus.ACTIVE) {
      return toDto(project);
    }
    project.setStatus(ProjectStatus.ACTIVE);
    project.setArchivedTime(null);
    project = projectRepository.save(project);

    passportRepository
        .findFirstByProjectIdAndParentIdIsNull(project.getId())
        .ifPresent(
            root -> {
              root.setStatus(Passport.Status.ACTIVE);
              root.setArchivedTime(null);
              passportRepository.save(root);
            });
    return toDto(project);
  }

  /** Loads a project that is not archived, for administrative operations within it. */
  @Transactional(readOnly = true)
  public Project requireActiveProject(String id) {
    Project project = requireProject(id);
    if (project.getStatus() != ProjectStatus.ACTIVE) {
      throw new InvalidInputException("Project is archived: " + id);
    }
    return project;
  }

  private Project requireProject(String id) {
    if (id == null || id.isBlank()) {
      throw new InvalidInputException("Project ID must not be null");
    }
    return projectRepository.findById(id).orElseThrow(() -> notFound(id));
  }

  private static ResponseStatusException notFound(String id) {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found: " + id);
  }

  private ProjectDto toDto(Project project) {
    String rootPassportId =
        passportRepository
            .findFirstByProjectIdAndParentIdIsNull(project.getId())
            .map(Passport::getId)
            .orElse(null);
    ProjectDto dto = ProjectDto.from(project, rootPassportId);
    dto.setAccessGroupCount(accessGroupRepository.countByProjectId(project.getId()));
    dto.setActiveApiKeyCount(
        apiKeyRepository.countUsableByProjectId(
            project.getId(), ApiKeyStatus.ACTIVE, ZonedDateTime.now()));
    return dto;
  }
}
