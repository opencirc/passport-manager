package com.opencirc.api.passport.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencirc.api.passport.context.UserContext;
import com.opencirc.api.passport.dao.PassportRepository;
import com.opencirc.api.passport.dao.PassportTemplateRepository;
import com.opencirc.api.passport.dto.CreatedByDto;
import com.opencirc.api.passport.dto.PassportTemplateDto;
import com.opencirc.api.passport.dto.UserDto;
import com.opencirc.api.passport.exception.ResourceNotFoundException;
import com.opencirc.api.passport.model.Datasheet;
import com.opencirc.api.passport.model.Passport;
import com.opencirc.api.passport.model.PassportDatasheetMapping;
import com.opencirc.api.passport.model.PassportTemplate;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Service class for PassportTemplate related operations. */
@Service
public class PassportTemplateService {

  @Autowired private PassportRepository passportRepository;

  @Autowired private PassportTemplateRepository passportTemplateRepository;

  @Autowired private UserContext userContext;

  @Autowired private ObjectMapper objectMapper;

  /** Creates a template from the existing passport. */
  public PassportTemplateDto createTemplateFromPassport(
      String passportId, boolean dryRun, String templateName) {
    Optional<Passport> passport =
        passportRepository.findPassport(passportId, Passport.Status.ACTIVE);
    if (passport.isEmpty() || !Passport.Status.ACTIVE.equals(passport.get().getStatus())) {
      throw new ResourceNotFoundException("Active passport not found");
    }

    PassportTemplate rawExtractedTemplate =
        generateTemplateFromPassport(passport.get(), templateName);
    PassportTemplate extractedTemplate =
        dryRun ? rawExtractedTemplate : passportTemplateRepository.save(rawExtractedTemplate);
    return PassportTemplateDto.from(extractedTemplate);
  }

  /** Extracts template from the existing passport. */
  private PassportTemplate generateTemplateFromPassport(Passport passport, String templateName) {
    ObjectNode rootNode = objectMapper.createObjectNode();

    for (PassportDatasheetMapping passportDatasheetMapping : passport.getDatasheetMappings()) {
      Datasheet datasheet = passportDatasheetMapping.getDatasheet();
      if (Datasheet.DataCategory.UNIQUE.equals(datasheet.getDataCategory())) {
        continue;
      }
      JsonNode dataNode = datasheet.getData();
      JsonNode newDataNode = dataNode.deepCopy();
      clearActualValues(newDataNode);
      rootNode = (ObjectNode) newDataNode;
    }
    UserDto userDtoContext = userContext.getCurrentUser();
    return PassportTemplate.builder()
        .name(templateName)
        .template(rootNode)
        .createdById(userDtoContext.getId())
        .createdBy(new CreatedByDto(userDtoContext.getFullName(), userDtoContext.getEmail()))
        .build();
  }

  /** Clears the value from the passport to make it as template. */
  private void clearActualValues(JsonNode node) {
    if (node.isObject()) {
      ObjectNode objectNode = (ObjectNode) node;
      Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> entry = fields.next();
        if ("actualValue".equals(entry.getKey())) {
          objectNode.put("actualValue", "");
        } else {
          clearActualValues(entry.getValue());
        }
      }
    } else if (node.isArray()) {
      for (JsonNode item : node) {
        clearActualValues(item);
      }
    }
  }

  /** Retrieves the template from the database. */
  public PassportTemplateDto getPassportTemplate(String id) {
    PassportTemplate template = passportTemplateRepository.findFirstById(id);

    if (template == null) {
      throw new ResourceNotFoundException("Passport template not found for ID: " + id);
    }
    return PassportTemplateDto.from(template);
  }

  /** Lists the templates. */
  public List<PassportTemplateDto> getAllPassportTemplates() {
    List<PassportTemplate> templates = passportTemplateRepository.findAll();

    if (templates.isEmpty()) {
      throw new ResourceNotFoundException("No passport templates found");
    }

    return templates.stream().map(PassportTemplateDto::from).toList();
  }
}
