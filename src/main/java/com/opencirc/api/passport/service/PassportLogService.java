package com.opencirc.api.passport.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencirc.api.passport.context.UserContext;
import com.opencirc.api.passport.dao.PassportLogRepository;
import com.opencirc.api.passport.dto.CreatedByDto;
import com.opencirc.api.passport.dto.UserDto;
import com.opencirc.api.passport.enums.PassportLogAction;
import com.opencirc.api.passport.model.PassportLog;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service to handle passport logging. */
@Service
@RequiredArgsConstructor
public class PassportLogService {

  private final PassportLogRepository passportLogRepository;
  private final UserContext userContext;
  private final ObjectMapper objectMapper;

  /**
   * Logs an event for a passport.
   *
   * @param passportId the ID of the passport
   * @param action the action performed
   * @param changes the changes made
   */
  @Transactional
  public void logEvent(String passportId, PassportLogAction action, Object changes) {
    logEvent(passportId, action, changes, null, null);
  }

  /**
   * Logs an event for a passport with explicit creator information.
   *
   * @param passportId the ID of the passport
   * @param action the action performed
   * @param changes the changes made
   * @param createdBy the creator information
   * @param createdById the creator user ID
   */
  @Transactional
  public void logEvent(
      String passportId,
      PassportLogAction action,
      Object changes,
      CreatedByDto createdBy,
      String createdById) {
    PassportLog log = new PassportLog();
    log.setPassportId(passportId);

    if (createdBy != null) {
      log.setCreatedById(createdById != null ? createdById : "system");
      log.setCreatedBy(createdBy);
    } else {
      UserDto currentUser = null;
      try {
        currentUser = userContext.getCurrentUser();
      } catch (Exception ignored) {
      }
      if (currentUser != null) {
        log.setCreatedById(currentUser.getId());
        log.setCreatedBy(CreatedByDto.from(currentUser));
      } else {
        log.setCreatedById(createdById != null ? createdById : "system");
        log.setCreatedBy(new CreatedByDto("System", "system@opencirc.org"));
      }
    }

    ObjectNode data = objectMapper.createObjectNode();
    data.put("action", action.getValue());
    data.set("changes", objectMapper.valueToTree(changes));
    log.setData(data);

    passportLogRepository.save(log);
  }

  /**
   * Retrieves all logs for a specific passport.
   *
   * @param passportId the ID of the passport
   * @return a list of passport logs
   */
  @Transactional(readOnly = true)
  public List<PassportLog> getLogsByPassportId(String passportId) {
    return passportLogRepository.findByPassportId(passportId);
  }
}
