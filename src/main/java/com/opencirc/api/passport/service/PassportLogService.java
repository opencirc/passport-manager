package com.opencirc.api.passport.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencirc.api.passport.context.UserContext;
import com.opencirc.api.passport.dao.PassportLogRepository;
import com.opencirc.api.passport.dto.CreatedByDto;
import com.opencirc.api.passport.dto.UserDto;
import com.opencirc.api.passport.enums.PassportLogAction;
import com.opencirc.api.passport.model.PassportLog;
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
    UserDto currentUser = userContext.getCurrentUser();

    PassportLog log = new PassportLog();
    log.setPassportId(passportId);
    log.setCreatedById(currentUser.getId());
    log.setCreatedBy(CreatedByDto.from(currentUser));

    ObjectNode data = objectMapper.createObjectNode();
    data.put("action", action.getValue());
    data.set("changes", objectMapper.valueToTree(changes));
    log.setData(data);

    passportLogRepository.save(log);
  }
}
