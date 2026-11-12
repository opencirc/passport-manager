package com.opencirc.api.passport.service;

import com.opencirc.api.passport.dao.OwnerRepository;
import com.opencirc.api.passport.dao.ProjectRepository;
import com.opencirc.api.passport.dto.OwnerDto;
import com.opencirc.api.passport.enums.ProjectStatus;
import com.opencirc.api.passport.exception.InvalidInputException;
import com.opencirc.api.passport.model.Owner;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Administrative CRUD for owner organizations. */
@Service
public class OwnerService {

  private final OwnerRepository ownerRepository;

  /** Projects are counted before an owner may be deleted. */
  private final ProjectRepository projectRepository;

  /** Constructor. */
  public OwnerService(OwnerRepository ownerRepository, ProjectRepository projectRepository) {
    this.ownerRepository = ownerRepository;
    this.projectRepository = projectRepository;
  }

  /** Creates an owner. */
  @Transactional
  public OwnerDto createOwner(String name) {
    Owner owner = new Owner();
    owner.setName(requireName(name));
    return OwnerDto.from(ownerRepository.saveAndFlush(owner));
  }

  /** Lists owners in creation order. */
  @Transactional(readOnly = true)
  public List<OwnerDto> listOwners() {
    return ownerRepository.findAllByOrderByCreatedTimeAsc().stream().map(OwnerDto::from).toList();
  }

  /** Retrieves an owner. */
  @Transactional(readOnly = true)
  public OwnerDto getOwner(String id) {
    return OwnerDto.from(requireOwner(id));
  }

  /** Renames an owner. */
  @Transactional
  public OwnerDto updateOwner(String id, String name) {
    Owner owner = requireOwner(id);
    owner.setName(requireName(name));
    return OwnerDto.from(ownerRepository.save(owner));
  }

  /**
   * Deletes an owner that has no active projects. Archived projects keep existing without an owner;
   * active ones must be reassigned or archived first, so ownership is never lost silently.
   */
  @Transactional
  public void deleteOwner(String id) {
    Owner owner = requireOwner(id);
    long activeProjects = projectRepository.countByOwnerIdAndStatus(id, ProjectStatus.ACTIVE);
    if (activeProjects > 0) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Owner still has "
              + activeProjects
              + " active project(s); reassign or archive them before deleting the owner");
    }
    ownerRepository.delete(owner);
  }

  private Owner requireOwner(String id) {
    return ownerRepository
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Owner not found: " + id));
  }

  private static String requireName(String name) {
    if (name == null || name.isBlank()) {
      throw new InvalidInputException("Owner name is required");
    }
    return name.trim();
  }
}
