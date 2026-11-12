package com.opencirc.api.passport.dao;

import com.opencirc.api.passport.model.AuditLogTag;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Audit log tag repository. */
@Repository
public interface AuditLogTagRepository extends JpaRepository<AuditLogTag, String> {

  /** The single validation tag of an audit log entry. */
  Optional<AuditLogTag> findByLogId(String logId);

  /** The tags of several audit log entries. */
  List<AuditLogTag> findByLogIdIn(Collection<String> logIds);
}
