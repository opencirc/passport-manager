package com.opencirc.api.passport.service;

import com.opencirc.api.passport.auth.principal.AdminPrincipal;
import com.opencirc.api.passport.auth.principal.ApiKeyPrincipal;
import com.opencirc.api.passport.context.ActorContext;
import com.opencirc.api.passport.dao.PassportLogRepository;
import com.opencirc.api.passport.dto.CreatedByDto;
import com.opencirc.api.passport.dto.PassportLogDto;
import com.opencirc.api.passport.enums.PassportLogAction;
import com.opencirc.api.passport.model.PassportLog;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/** Service to handle passport logging. */
@Service
@RequiredArgsConstructor
public class PassportLogService {

  private final PassportLogRepository passportLogRepository;
  private final ActorContext actorContext;
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
   * @param createdById the creator id (admin id or API key id)
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

    if (createdBy == null) {
      Actor actor = captureActor();
      createdBy = actor.createdBy();
      if (!"system".equals(actor.createdById()) || createdById == null) {
        createdById = actor.createdById();
      }
    }
    log.setCreatedById(createdById != null ? createdById : "system");
    log.setCreatedBy(createdBy);

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
  public List<PassportLogDto> getLogsByPassportId(String passportId) {
    return passportLogRepository.findByPassportIdOrderByCreatedTimeAsc(passportId).stream()
        .map(
            log ->
                new PassportLogDto(
                    log.getId(),
                    log.getPassportId(),
                    log.getData(),
                    log.getCreatedById(),
                    log.getCreatedBy(),
                    log.getCreatedTime()))
        .toList();
  }

  /**
   * Captures the actor on the request thread before asynchronous processing: the API key of a
   * machine request, the administrator of an admin session, or the system identity.
   */
  public Actor captureActor() {
    Optional<ApiKeyPrincipal> apiKey = actorContext.currentApiKey();
    if (apiKey.isPresent()) {
      return new Actor(new CreatedByDto(apiKey.get().getKeyName(), null), apiKey.get().getKeyId());
    }
    Optional<AdminPrincipal> admin = actorContext.currentAdmin();
    if (admin.isPresent() && admin.get().getAdminId() != null) {
      return new Actor(
          new CreatedByDto(admin.get().getFullName(), admin.get().getEmail()),
          admin.get().getAdminId());
    }
    return systemActor();
  }

  /** Identity used for operations without an authenticated actor. */
  public static Actor systemActor() {
    return new Actor(new CreatedByDto("System", "system@opencirc.org"), "system");
  }

  /** Snapshot of the identity responsible for an audit event. */
  public record Actor(CreatedByDto createdBy, String createdById) {}
}
