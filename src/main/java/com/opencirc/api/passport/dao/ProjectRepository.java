package com.opencirc.api.passport.dao;

import com.opencirc.api.passport.enums.ProjectStatus;
import com.opencirc.api.passport.model.Project;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Project repository. */
@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {

  /** Lists every project in creation order, archived ones included. */
  List<Project> findAllByOrderByCreatedTimeAsc();

  /** Lists the projects in the given status in creation order. */
  List<Project> findAllByStatusOrderByCreatedTimeAsc(ProjectStatus status);

  /** Finds a project by name and status. */
  Optional<Project> findFirstByNameAndStatus(String name, ProjectStatus status);
}
