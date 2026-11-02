package com.opencirc.api.passport.command;

import com.opencirc.api.passport.auth.principal.ApiKeyPrincipal;
import com.opencirc.api.passport.config.AppProperties;
import com.opencirc.api.passport.dao.AccessGroupRepository;
import com.opencirc.api.passport.dao.ApiKeyRepository;
import com.opencirc.api.passport.dao.OwnerRepository;
import com.opencirc.api.passport.dao.PassportRepository;
import com.opencirc.api.passport.dao.ProjectRepository;
import com.opencirc.api.passport.dto.AccessGroupRequestDto;
import com.opencirc.api.passport.dto.CreateProjectRequestDto;
import com.opencirc.api.passport.dto.CreatedByDto;
import com.opencirc.api.passport.dto.GeneratedApiKeyDto;
import com.opencirc.api.passport.enums.ApiKeyStatus;
import com.opencirc.api.passport.enums.ProjectStatus;
import com.opencirc.api.passport.model.AccessGroup;
import com.opencirc.api.passport.model.AccessGroupPermissions;
import com.opencirc.api.passport.model.ApiKey;
import com.opencirc.api.passport.model.Owner;
import com.opencirc.api.passport.model.Project;
import com.opencirc.api.passport.service.AccessGroupService;
import com.opencirc.api.passport.service.ApiKeyService;
import com.opencirc.api.passport.service.ProjectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Provisions the administrative infrastructure the passport seeders need: a seed owner, a seed
 * project with its root passport, an access group with full permissions and an API key. Reuses what
 * already exists so seeding can be re-run.
 */
@Component
@Slf4j
public class SeedInfrastructure {

  public static final String SEED_OWNER_NAME = "Seed Owner";
  public static final String SEED_PROJECT_NAME = "Seed Project";
  public static final String SEED_ACCESS_GROUP_NAME = "Seed Integrator";
  public static final String SEED_API_KEY_NAME = "Seed Key";

  private final OwnerRepository ownerRepository;
  private final ProjectRepository projectRepository;
  private final ProjectService projectService;
  private final AccessGroupRepository accessGroupRepository;
  private final AccessGroupService accessGroupService;
  private final ApiKeyRepository apiKeyRepository;
  private final ApiKeyService apiKeyService;
  private final PassportRepository passportRepository;
  private final AppProperties appProperties;

  /** Constructor. */
  public SeedInfrastructure(
      OwnerRepository ownerRepository,
      ProjectRepository projectRepository,
      ProjectService projectService,
      AccessGroupRepository accessGroupRepository,
      AccessGroupService accessGroupService,
      ApiKeyRepository apiKeyRepository,
      ApiKeyService apiKeyService,
      PassportRepository passportRepository,
      AppProperties appProperties) {
    this.ownerRepository = ownerRepository;
    this.projectRepository = projectRepository;
    this.projectService = projectService;
    this.accessGroupRepository = accessGroupRepository;
    this.accessGroupService = accessGroupService;
    this.apiKeyRepository = apiKeyRepository;
    this.apiKeyService = apiKeyService;
    this.passportRepository = passportRepository;
    this.appProperties = appProperties;
  }

  /** The machine actor to seed with and the root passport to hang seeded passports from. */
  public record SeedContext(ApiKeyPrincipal actor, String rootPassportId) {}

  /** Finds or creates the seed owner, project, access group and API key. */
  public SeedContext ensure() {
    CreatedByDto createdBy =
        new CreatedByDto(appProperties.getSystemAdminName(), appProperties.getSystemAdminEmail());

    Owner owner =
        ownerRepository
            .findFirstByName(SEED_OWNER_NAME)
            .orElseGet(() -> ownerRepository.save(new Owner(null, SEED_OWNER_NAME, null)));

    Project project =
        projectRepository
            .findFirstByNameAndStatus(SEED_PROJECT_NAME, ProjectStatus.ACTIVE)
            .orElseGet(
                () -> {
                  var dto =
                      projectService.createProject(
                          new CreateProjectRequestDto(
                              SEED_PROJECT_NAME, "Seeded sample data", owner.getId()),
                          createdBy,
                          null);
                  return projectRepository.findById(dto.getId()).orElseThrow();
                });

    AccessGroup accessGroup =
        accessGroupRepository
            .findFirstByProjectIdAndName(project.getId(), SEED_ACCESS_GROUP_NAME)
            .orElseGet(
                () -> {
                  var dto =
                      accessGroupService.createAccessGroup(
                          project.getId(),
                          new AccessGroupRequestDto(
                              SEED_ACCESS_GROUP_NAME,
                              "Full access for seeding",
                              AccessGroupPermissions.full()),
                          createdBy,
                          null);
                  return accessGroupRepository.findById(dto.getId()).orElseThrow();
                });

    ApiKey apiKey =
        apiKeyRepository
            .findFirstByAccessGroupIdAndNameAndStatus(
                accessGroup.getId(), SEED_API_KEY_NAME, ApiKeyStatus.ACTIVE)
            .orElseGet(
                () -> {
                  GeneratedApiKeyDto generated =
                      apiKeyService.createApiKey(accessGroup.getId(), SEED_API_KEY_NAME, null);
                  log.info(
                      "Issued seed API key {} (token shown once): {}",
                      generated.getId(),
                      generated.getToken());
                  return apiKeyRepository.findById(generated.getId()).orElseThrow();
                });

    String rootPassportId =
        passportRepository
            .findFirstByProjectIdAndParentIdIsNull(project.getId())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Seed project has no root passport: " + project.getId()))
            .getId();

    return new SeedContext(apiKeyService.actorFor(apiKey.getId()), rootPassportId);
  }
}
