package com.opencirc.api.passport.service;

import com.opencirc.api.passport.dao.OwnerRepository;
import com.opencirc.api.passport.dto.OwnerDto;
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

  /** Constructor. */
  public OwnerService(OwnerRepository ownerRepository) {
    this.ownerRepository = ownerRepository;
  }

  /** Creates an owner. */
  @Transactional
  public OwnerDto createOwner(String name) {
    Owner owner = new Owner();
    owner.setName(requireName(name));
    return OwnerDto.from(ownerRepository.save(owner));
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

  /** Deletes an owner; its projects keep existing with their owner reference cleared. */
  @Transactional
  public void deleteOwner(String id) {
    ownerRepository.delete(requireOwner(id));
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
