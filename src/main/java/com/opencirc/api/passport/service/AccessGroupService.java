package com.opencirc.api.passport.service;

import com.opencirc.api.passport.dao.AccessGroupRepository;
import com.opencirc.api.passport.dto.AccessGroupDto;
import com.opencirc.api.passport.dto.AccessGroupRequestDto;
import com.opencirc.api.passport.dto.CreatedByDto;
import com.opencirc.api.passport.exception.InvalidInputException;
import com.opencirc.api.passport.model.AccessGroup;
import com.opencirc.api.passport.model.AccessGroupPermissions;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Administrative CRUD for the access groups of a project. */
@Service
public class AccessGroupService {

  private final AccessGroupRepository accessGroupRepository;

  private final ProjectService projectService;

  /** Constructor. */
  public AccessGroupService(
      AccessGroupRepository accessGroupRepository, ProjectService projectService) {
    this.accessGroupRepository = accessGroupRepository;
    this.projectService = projectService;
  }

  /** Creates an access group in an active project. */
  @Transactional
  public AccessGroupDto createAccessGroup(
      String projectId, AccessGroupRequestDto request, CreatedByDto createdBy, String createdById) {
    projectService.requireActiveProject(projectId);
    validate(request);

    AccessGroup accessGroup = new AccessGroup();
    accessGroup.setProjectId(projectId);
    accessGroup.setName(request.getName().trim());
    accessGroup.setDescription(request.getDescription());
    accessGroup.setPermissions(request.getPermissions());
    accessGroup.setCreatedById(createdById);
    accessGroup.setCreatedBy(createdBy);
    return AccessGroupDto.from(accessGroupRepository.save(accessGroup));
  }

  /** Updates the name, description and permissions of an access group. */
  @Transactional
  public AccessGroupDto updateAccessGroup(
      String projectId, String accessGroupId, AccessGroupRequestDto request) {
    validate(request);
    AccessGroup accessGroup = requireAccessGroup(projectId, accessGroupId);
    accessGroup.setName(request.getName().trim());
    accessGroup.setDescription(request.getDescription());
    accessGroup.setPermissions(request.getPermissions());
    return AccessGroupDto.from(accessGroupRepository.save(accessGroup));
  }

  /** Lists the access groups of a project. */
  @Transactional(readOnly = true)
  public List<AccessGroupDto> listAccessGroups(String projectId) {
    projectService.getProject(projectId, true);
    return accessGroupRepository.findAllByProjectIdOrderByCreatedTimeAsc(projectId).stream()
        .map(AccessGroupDto::from)
        .toList();
  }

  /** Retrieves an access group of a project. */
  @Transactional(readOnly = true)
  public AccessGroupDto getAccessGroup(String projectId, String accessGroupId) {
    return AccessGroupDto.from(requireAccessGroup(projectId, accessGroupId));
  }

  private AccessGroup requireAccessGroup(String projectId, String accessGroupId) {
    return accessGroupRepository
        .findByIdAndProjectId(accessGroupId, projectId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Access group not found in project: " + accessGroupId));
  }

  private static void validate(AccessGroupRequestDto request) {
    if (request == null || request.getName() == null || request.getName().isBlank()) {
      throw new InvalidInputException("Access group name is required");
    }
    if (request.getPermissions() == null) {
      request.setPermissions(new AccessGroupPermissions());
    }
  }
}
