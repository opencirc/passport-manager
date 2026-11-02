package com.opencirc.api.passport.dao;

import com.opencirc.api.passport.enums.ApiKeyStatus;
import com.opencirc.api.passport.model.ApiKey;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** API key repository. The primary key is the public key id, giving O(1) indexed lookup. */
@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, String> {

  /** Lists the keys issued by an access group in creation order. */
  List<ApiKey> findAllByAccessGroupIdOrderByCreatedTimeAsc(String accessGroupId);

  /** Finds a key of an access group by name and status. */
  Optional<ApiKey> findFirstByAccessGroupIdAndNameAndStatus(
      String accessGroupId, String name, ApiKeyStatus status);
}
