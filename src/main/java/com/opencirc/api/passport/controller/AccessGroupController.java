package com.opencirc.api.passport.controller;

import com.opencirc.api.passport.context.ActorContext;
import com.opencirc.api.passport.dto.AccessGroupDto;
import com.opencirc.api.passport.dto.AccessGroupRequestDto;
import com.opencirc.api.passport.dto.AdminDto;
import com.opencirc.api.passport.dto.CreatedByDto;
import com.opencirc.api.passport.service.AccessGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Administrative endpoints for the access groups of a project. */
@RestController
@RequestMapping("/api/admin/projects/{projectId}/access-groups")
@RequiredArgsConstructor
@Tag(name = "Admin: Access Groups", description = "Permission profiles for machine integrations")
public class AccessGroupController {

  private final AccessGroupService accessGroupService;

  private final ActorContext actorContext;

  /** Creates an access group. */
  @Operation(summary = "Creates an access group with its CRUD permissions")
  @PostMapping
  public ResponseEntity<AccessGroupDto> createAccessGroup(
      @PathVariable String projectId, @Valid @RequestBody AccessGroupRequestDto request) {
    AdminDto admin = actorContext.requireAdmin();
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            accessGroupService.createAccessGroup(
                projectId, request, CreatedByDto.from(admin), admin.getId()));
  }

  /** Lists the access groups of a project. */
  @Operation(summary = "Lists the access groups of a project")
  @GetMapping
  public ResponseEntity<List<AccessGroupDto>> listAccessGroups(@PathVariable String projectId) {
    return ResponseEntity.ok(accessGroupService.listAccessGroups(projectId));
  }

  /** Retrieves an access group. */
  @Operation(summary = "Retrieves an access group")
  @GetMapping("/{id}")
  public ResponseEntity<AccessGroupDto> getAccessGroup(
      @PathVariable String projectId, @PathVariable String id) {
    return ResponseEntity.ok(accessGroupService.getAccessGroup(projectId, id));
  }

  /** Updates an access group and its permissions. */
  @Operation(summary = "Updates the name, description and permissions of an access group")
  @PutMapping("/{id}")
  public ResponseEntity<AccessGroupDto> updateAccessGroup(
      @PathVariable String projectId,
      @PathVariable String id,
      @Valid @RequestBody AccessGroupRequestDto request) {
    return ResponseEntity.ok(accessGroupService.updateAccessGroup(projectId, id, request));
  }
}
