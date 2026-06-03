package com.opencirc.api.passport.dao;

import com.opencirc.api.passport.model.PassportLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for PassportLog entities. */
@Repository
public interface PassportLogRepository extends JpaRepository<PassportLog, String> {

  /**
   * Finds all logs for a given passport ID.
   *
   * @param passportId the ID of the passport
   * @return a list of passport logs
   */
  List<PassportLog> findByPassportId(String passportId);
}
