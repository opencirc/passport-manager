package com.opencirc.api.passport.service.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.opencirc.api.passport.dao.OwnerRepository;
import com.opencirc.api.passport.dao.ProjectRepository;
import com.opencirc.api.passport.enums.ProjectStatus;
import com.opencirc.api.passport.model.Owner;
import com.opencirc.api.passport.service.OwnerService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Unit tests of owner deletion limits. */
@ExtendWith(MockitoExtension.class)
public class TestOwnerService {

  @Mock private OwnerRepository ownerRepository;
  @Mock private ProjectRepository projectRepository;

  private OwnerService service;
  private Owner owner;

  @BeforeEach
  void setUp() {
    service = new OwnerService(ownerRepository, projectRepository);
    owner = new Owner();
    owner.setId("owner-1");
    owner.setName("Owner Org");
    when(ownerRepository.findById("owner-1")).thenReturn(Optional.of(owner));
  }

  @Test
  void refusesToDeleteAnOwnerWithActiveProjects() {
    when(projectRepository.countByOwnerIdAndStatus("owner-1", ProjectStatus.ACTIVE)).thenReturn(2L);

    ResponseStatusException thrown =
        assertThrows(ResponseStatusException.class, () -> service.deleteOwner("owner-1"));

    assertEquals(HttpStatus.CONFLICT, thrown.getStatusCode());
    verify(ownerRepository, never()).delete(owner);
  }

  @Test
  void deletesAnOwnerWhoseProjectsAreAllArchived() {
    when(projectRepository.countByOwnerIdAndStatus("owner-1", ProjectStatus.ACTIVE)).thenReturn(0L);

    service.deleteOwner("owner-1");

    verify(ownerRepository).delete(owner);
  }
}
