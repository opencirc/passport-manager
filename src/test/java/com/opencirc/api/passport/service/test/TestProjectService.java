package com.opencirc.api.passport.service.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.opencirc.api.passport.dao.OwnerRepository;
import com.opencirc.api.passport.dao.PassportRepository;
import com.opencirc.api.passport.dao.ProjectRepository;
import com.opencirc.api.passport.dto.CreateProjectRequestDto;
import com.opencirc.api.passport.dto.CreatedByDto;
import com.opencirc.api.passport.dto.ProjectDto;
import com.opencirc.api.passport.enums.PassportLogAction;
import com.opencirc.api.passport.enums.ProjectStatus;
import com.opencirc.api.passport.model.Passport;
import com.opencirc.api.passport.model.Project;
import com.opencirc.api.passport.service.PassportLogService;
import com.opencirc.api.passport.service.ProjectService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
public class TestProjectService {

  private static final CreatedByDto ADMIN = new CreatedByDto("Jane Doe", "jane@example.com");

  @Mock private ProjectRepository projectRepository;
  @Mock private OwnerRepository ownerRepository;
  @Mock private PassportRepository passportRepository;
  @Mock private PassportLogService passportLogService;

  private ProjectService projectService;

  @BeforeEach
  void setUp() {
    projectService =
        new ProjectService(
            projectRepository, ownerRepository, passportRepository, passportLogService);
  }

  @Test
  void createsProjectAndRootPassportTogether() {
    when(projectRepository.save(any(Project.class)))
        .thenAnswer(
            i -> {
              Project project = i.getArgument(0);
              project.setId("project-1");
              return project;
            });
    when(passportRepository.save(any(Passport.class))).thenAnswer(i -> i.getArgument(0));

    ProjectDto dto =
        projectService.createProject(
            new CreateProjectRequestDto("EcoOffice", "Office complex", null), ADMIN, "admin-1");

    ArgumentCaptor<Passport> root = ArgumentCaptor.forClass(Passport.class);
    verify(passportRepository).save(root.capture());
    assertNotNull(root.getValue().getId());
    assertEquals("project-1", root.getValue().getProjectId());
    assertNull(root.getValue().getParentId());
    assertNull(root.getValue().getCreatedById());
    assertEquals(Passport.Status.ACTIVE, root.getValue().getStatus());
    assertEquals("EcoOffice", root.getValue().getName());

    assertEquals("project-1", dto.getId());
    assertEquals(root.getValue().getId(), dto.getRootPassportId());
    assertEquals("active", dto.getStatus());
    assertEquals("admin-1", dto.getCreatedById());
    assertEquals(ADMIN, dto.getCreatedBy());
    verify(passportLogService)
        .logEvent(eq(root.getValue().getId()), eq(PassportLogAction.CREATE), any());
  }

  @Test
  void rejectsUnknownOwner() {
    when(ownerRepository.existsById("owner-x")).thenReturn(false);

    assertThrows(
        com.opencirc.api.passport.exception.InvalidInputException.class,
        () ->
            projectService.createProject(
                new CreateProjectRequestDto("EcoOffice", null, "owner-x"), ADMIN, "admin-1"));
    verify(projectRepository, org.mockito.Mockito.never()).save(any());
  }

  @Test
  void archiveSoftArchivesProjectAndCascadesToPassports() {
    Project project = activeProject();
    when(projectRepository.findById("project-1")).thenReturn(Optional.of(project));
    when(projectRepository.save(any(Project.class))).thenAnswer(i -> i.getArgument(0));
    when(passportRepository.archiveByProjectId(
            eq("project-1"), any(), eq(Passport.Status.ARCHIVED)))
        .thenReturn(7);
    Passport root = new Passport();
    root.setId("root-1");
    when(passportRepository.findFirstByProjectIdAndParentIdIsNull("project-1"))
        .thenReturn(Optional.of(root));

    ProjectDto dto = projectService.archiveProject("project-1");

    assertEquals("archived", dto.getStatus());
    assertNotNull(dto.getArchivedTime());
    verify(passportRepository)
        .archiveByProjectId(eq("project-1"), any(), eq(Passport.Status.ARCHIVED));
    verify(projectRepository, org.mockito.Mockito.never()).delete(any());
    verify(passportLogService).logEvent(eq("root-1"), eq(PassportLogAction.ARCHIVE), any());
  }

  @Test
  void restoreReactivatesProjectAndRootPassport() {
    Project project = activeProject();
    project.setStatus(ProjectStatus.ARCHIVED);
    when(projectRepository.findById("project-1")).thenReturn(Optional.of(project));
    when(projectRepository.save(any(Project.class))).thenAnswer(i -> i.getArgument(0));
    Passport root = new Passport();
    root.setId("root-1");
    root.setStatus(Passport.Status.ARCHIVED);
    when(passportRepository.findFirstByProjectIdAndParentIdIsNull("project-1"))
        .thenReturn(Optional.of(root));

    ProjectDto dto = projectService.restoreProject("project-1");

    assertEquals("active", dto.getStatus());
    assertNull(dto.getArchivedTime());
    assertEquals(Passport.Status.ACTIVE, root.getStatus());
    verify(passportRepository).save(root);
  }

  @Test
  void hidesArchivedProjectUnlessRequested() {
    Project project = activeProject();
    project.setStatus(ProjectStatus.ARCHIVED);
    when(projectRepository.findById("project-1")).thenReturn(Optional.of(project));

    assertThrows(
        ResponseStatusException.class, () -> projectService.getProject("project-1", false));
    assertEquals("archived", projectService.getProject("project-1", true).getStatus());
  }

  private static Project activeProject() {
    Project project = new Project();
    project.setId("project-1");
    project.setName("EcoOffice");
    project.setStatus(ProjectStatus.ACTIVE);
    project.setCreatedBy(ADMIN);
    return project;
  }
}
