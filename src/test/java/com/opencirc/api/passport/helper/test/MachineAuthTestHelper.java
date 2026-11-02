package com.opencirc.api.passport.helper.test;

import com.opencirc.api.passport.dto.AccessGroupDto;
import com.opencirc.api.passport.dto.AccessGroupRequestDto;
import com.opencirc.api.passport.dto.CreateProjectRequestDto;
import com.opencirc.api.passport.dto.CreatedByDto;
import com.opencirc.api.passport.dto.GeneratedApiKeyDto;
import com.opencirc.api.passport.dto.ProjectDto;
import com.opencirc.api.passport.model.AccessGroupPermissions;
import com.opencirc.api.passport.service.AccessGroupService;
import com.opencirc.api.passport.service.ApiKeyService;
import com.opencirc.api.passport.service.ProjectService;
import java.util.UUID;

/** Provisions a project, access group and API key so tests can call machine endpoints. */
public final class MachineAuthTestHelper {

  private MachineAuthTestHelper() {}

  /** Everything a machine-authenticated test needs. */
  public record MachineFixture(
      String projectId,
      String rootPassportId,
      String accessGroupId,
      String apiKeyId,
      String token) {

    /** The bearer header value for this fixture's key. */
    public String bearer() {
      return "Bearer " + token;
    }
  }

  /** Creates a fresh project with an access group carrying the permissions and one API key. */
  public static MachineFixture provision(
      ProjectService projectService,
      AccessGroupService accessGroupService,
      ApiKeyService apiKeyService,
      AccessGroupPermissions permissions) {
    CreatedByDto createdBy = new CreatedByDto("Test Admin", "admin@test.com");
    ProjectDto project =
        projectService.createProject(
            new CreateProjectRequestDto("Test Project " + UUID.randomUUID(), null, null),
            createdBy,
            "admin-id");
    AccessGroupDto accessGroup =
        accessGroupService.createAccessGroup(
            project.getId(),
            new AccessGroupRequestDto("Integrator", null, permissions),
            createdBy,
            "admin-id");
    GeneratedApiKeyDto apiKey = apiKeyService.createApiKey(accessGroup.getId(), "Test Key", null);
    return new MachineFixture(
        project.getId(),
        project.getRootPassportId(),
        accessGroup.getId(),
        apiKey.getId(),
        apiKey.getToken());
  }
}
