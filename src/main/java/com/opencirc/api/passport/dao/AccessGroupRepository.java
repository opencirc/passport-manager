package com.opencirc.api.passport.dao;

import com.opencirc.api.passport.model.AccessGroup;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Access group repository. */
@Repository
public interface AccessGroupRepository extends JpaRepository<AccessGroup, String> {

  /** Lists the access groups of a project in creation order. */
  List<AccessGroup> findAllByProjectIdOrderByCreatedTimeAsc(String projectId);

  /** Finds an access group that belongs to the given project. */
  Optional<AccessGroup> findByIdAndProjectId(String id, String projectId);

  /** Finds an access group of a project by name. */
  Optional<AccessGroup> findFirstByProjectIdAndName(String projectId, String name);
}
