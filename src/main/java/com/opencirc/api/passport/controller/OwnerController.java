package com.opencirc.api.passport.controller;

import com.opencirc.api.passport.dto.OwnerDto;
import com.opencirc.api.passport.dto.OwnerRequestDto;
import com.opencirc.api.passport.service.OwnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Administrative endpoints for owner organizations. */
@RestController
@RequestMapping("/api/admin/owners")
@RequiredArgsConstructor
@Tag(name = "Admin: Owners", description = "Administration of owner organizations")
public class OwnerController {

  private final OwnerService ownerService;

  /** Creates an owner. */
  @Operation(summary = "Creates an owner organization")
  @PostMapping
  public ResponseEntity<OwnerDto> createOwner(@Valid @RequestBody OwnerRequestDto request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ownerService.createOwner(request.getName()));
  }

  /** Lists owners. */
  @Operation(summary = "Lists owner organizations")
  @GetMapping
  public ResponseEntity<List<OwnerDto>> listOwners() {
    return ResponseEntity.ok(ownerService.listOwners());
  }

  /** Retrieves an owner. */
  @Operation(summary = "Retrieves an owner organization")
  @GetMapping("/{id}")
  public ResponseEntity<OwnerDto> getOwner(@PathVariable String id) {
    return ResponseEntity.ok(ownerService.getOwner(id));
  }

  /** Renames an owner. */
  @Operation(summary = "Renames an owner organization")
  @PutMapping("/{id}")
  public ResponseEntity<OwnerDto> updateOwner(
      @PathVariable String id, @Valid @RequestBody OwnerRequestDto request) {
    return ResponseEntity.ok(ownerService.updateOwner(id, request.getName()));
  }

  /** Deletes an owner; its projects keep existing without an owner. */
  @Operation(summary = "Deletes an owner organization; its projects are kept without an owner")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteOwner(@PathVariable String id) {
    ownerService.deleteOwner(id);
    return ResponseEntity.noContent().build();
  }
}
