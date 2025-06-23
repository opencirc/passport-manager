package com.opencirc.api.passport.service;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencirc.api.passport.context.UserContext;
import com.opencirc.api.passport.dao.PassportRepository;
import com.opencirc.api.passport.dao.PassportTemplateRepository;
import com.opencirc.api.passport.dto.PassportTemplateDto;
import com.opencirc.api.passport.model.Datasheet;
import com.opencirc.api.passport.model.Passport;
import com.opencirc.api.passport.model.PassportDatasheetMapping;
import com.opencirc.api.passport.model.PassportTemplate;

@Service
public class PassportTemplateService {


    /**
     * Injecting PassportRepository class.
     */
    @Autowired
    private PassportRepository passportRepository;


    /**
     * Injecting PassportTemplateRepository class.
     */
    @Autowired
    private PassportTemplateRepository passportTemplateRepository;

    /**
     * Injecting UserContext class.
     */
    @Autowired
    private UserContext userContext;


    /**
     * Creates template from the existing passport.
     *
     * @param passportId
     * @param dryRun
     * @param templateName
     * @return the template in json format
     */
    public PassportTemplateDto createTemplateFromPassport(String passportId,
            boolean dryRun, String templateName)
            throws JsonMappingException, JsonProcessingException {
        Optional<Passport> passport = passportRepository.findPassport(passportId,
                Passport.Status.ACTIVE);
        if (passport.isEmpty() || passport.get().getStatus() != Passport.Status.ACTIVE) {
            throw new HttpServerErrorException(HttpStatus.NOT_FOUND,
                    "Active passport found");
        }

        PassportTemplate rawExtractedTemplate = generateTemplateFromPassport(
                passport.get(), templateName);
        PassportTemplate extractedTemplate = dryRun ? rawExtractedTemplate
                : passportTemplateRepository.save(rawExtractedTemplate);
        return PassportTemplateDto.from(extractedTemplate);
    }

    /**
     * Extracts template from the existing passport.
     *
     * @param passport
     * @param templateName
     * @return the template in JSON format
     */
    private PassportTemplate generateTemplateFromPassport(Passport passport,
            String templateName) {
        PassportTemplate template = new PassportTemplate();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();

        for (PassportDatasheetMapping passportDatasheetMapping : passport
                .getDatasheetMappings()) {
            Datasheet datasheet = passportDatasheetMapping.getDatasheet();
            if (datasheet.getDataCategory() == Datasheet.DataCategory.UNIQUE) {
                continue;
            }
            JsonNode dataNode = datasheet.getData();
            JsonNode newDataNode = dataNode.deepCopy();
            clearActualValues(newDataNode);
            rootNode = (ObjectNode) newDataNode;

        }
        String userName = userContext.getCurrentUsername();
        template = PassportTemplate.builder().name(templateName).template(rootNode)
                .createdBy(userName).createdTime(LocalDateTime.now()).build();
        return template;
    }

    /**
     * Clears the value from the passport to make it as template.
     *
     * @param node - passport json
     */
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
    /**
     * Retrieves the template from database.
     *
     * @param id
     * @return template
     */
    public PassportTemplateDto getPassportTemplate(String id) {
        UUID uuid = UUID.fromString(id); 
        return PassportTemplateDto.from(passportTemplateRepository
                .findFirstById(uuid));
    }

    /**
     * Lists the templates.
     *
     * @return template
     */
    public List<PassportTemplateDto> getAllPassportTemplates() {
        return passportTemplateRepository.findAll().stream().map(PassportTemplateDto
                ::from).toList();
    }

}
