package com.opencirc.api.passport.dao;

import com.opencirc.api.passport.enums.AuditTargetType;
import com.opencirc.api.passport.model.AuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Audit log repository; every feed is served by one of the composite indexes. */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

  /** Chronological history of one entity. */
  List<AuditLog> findByTargetTypeAndTargetIdOrderByCreatedTimeAsc(
      AuditTargetType targetType, String targetId);

  /** Newest-first activity stream of a project. */
  List<AuditLog> findByProjectIdOrderByCreatedTimeDesc(String projectId);

  /** Newest-first activity stream of a passport. */
  List<AuditLog> findByPassportIdOrderByCreatedTimeDesc(String passportId);
}
