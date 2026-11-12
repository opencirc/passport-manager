package com.opencirc.api.passport.dao;

import com.opencirc.api.passport.enums.ApiKeyStatus;
import com.opencirc.api.passport.model.ApiKey;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** API key repository. The primary key is the public key id, giving O(1) indexed lookup. */
@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, String> {

  /** Lists the keys issued by an access group in creation order. */
  List<ApiKey> findAllByAccessGroupIdOrderByCreatedTimeAsc(String accessGroupId);

  /** Finds a key of an access group by name and status. */
  Optional<ApiKey> findFirstByAccessGroupIdAndNameAndStatus(
      String accessGroupId, String name, ApiKeyStatus status);

  /**
   * Counts the keys of a project, across all its access groups, that have the status and are not
   * expired.
   */
  @Query(
      """
      select count(k) from ApiKey k join AccessGroup g on g.id = k.accessGroupId
      where g.projectId = :projectId and k.status = :status
        and (k.expirationTime is null or k.expirationTime > :now)
      """)
  long countUsableByProjectId(
      @Param("projectId") String projectId,
      @Param("status") ApiKeyStatus status,
      @Param("now") ZonedDateTime now);

  /** Counts the keys of one access group that have the status and are not expired. */
  @Query(
      """
      select count(k) from ApiKey k
      where k.accessGroupId = :accessGroupId and k.status = :status
        and (k.expirationTime is null or k.expirationTime > :now)
      """)
  long countUsableByAccessGroupId(
      @Param("accessGroupId") String accessGroupId,
      @Param("status") ApiKeyStatus status,
      @Param("now") ZonedDateTime now);
}
